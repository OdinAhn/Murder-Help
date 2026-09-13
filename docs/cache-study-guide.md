# 캐싱 구현 학습 노트

이 문서는 프로젝트에서 구현한 캐싱 기능을 처음부터 순서대로 설명한다. 목표는 “코드가 어느 파일에 있고, 요청이 들어왔을 때 무엇이 실행되며, 왜 그 방식으로 설계했는지”를 이해하는 것이다.

---

## 1. 먼저 알아둘 개념

### 1.1 캐시는 원본 DB를 대체하지 않는다

이 프로젝트에서 원본 데이터는 MySQL이다. Redis나 Caffeine은 **조회 결과를 잠시 복사해 두는 공간**이다.

```text
클라이언트
  → API
    → 캐시 확인
      → 있으면: 캐시 결과 반환 (cache hit)
      → 없으면: MySQL 조회 → 결과를 캐시에 저장 → 반환 (cache miss)
```

캐시가 비어 있거나 Redis가 재시작되어도 MySQL에 원본 데이터가 있으므로 다음 조회에서 다시 채울 수 있다. 이 방식을 **Cache-aside(= Lazy Loading)** 라고 한다.

### 1.2 TTL이란?

TTL(Time To Live)은 캐시 항목의 만료 시간이다. TTL이 지나면 Redis/Caffeine이 항목을 자동 삭제하고, 다음 요청은 다시 MySQL을 조회한다.

TTL만으로 최신성을 보장할 수는 없다. 예를 들어 상품 재고가 지금 바뀌었는데 캐시 TTL이 5분이면 최대 5분 동안 예전 재고가 보일 수 있다. 그래서 데이터 변경 직후에는 `@CacheEvict`로 즉시 삭제한다.

### 1.3 Caffeine과 Redis의 차이

| 구분 | Caffeine | Redis |
| --- | --- | --- |
| 위치 | 애플리케이션 서버 JVM 메모리 | 별도 Redis 서버 메모리 |
| 서버가 2대일 때 | 각 서버의 캐시가 서로 다름 | 모든 서버가 같은 캐시를 공유 |
| 장점 | 아주 빠르고 설정이 간단 | Scale-out 환경에서도 일관된 캐시 |
| 프로젝트 사용 목적 | 로컬/테스트 또는 1대 서버 학습 단계 | AWS 배포 환경의 Remote Cache |

즉, Caffeine은 “내 서버 안의 메모장”, Redis는 “모든 서버가 함께 보는 공유 메모장”에 가깝다.

---

## 2. 전체 구현 목록

현재 캐시와 Redis 활용 기능은 아래와 같다.

| 구분 | 대상 | 이름 또는 Redis Key | TTL | 목적 |
| --- | --- | --- | --- | --- |
| 조회 캐시 | 상품 검색 v2 | `productSearch` | 10분 | 같은 검색 조건의 MySQL `LIKE` 조회 감소 |
| 조회 캐시 | 상품 상세 | `productDetail` | 5분 | 반복 상세 조회 감소 |
| 조회 캐시 | 상품 목록 | `productList` | 3분 | 반복 목록·페이징 조회 감소 |
| 조회 캐시 | 장바구니 전체 조회 | `cartItems` | 1분 | 개인 장바구니 반복 조회 감소 |
| Redis 자료구조 | 인기 검색어 | `search:popular:daily:{날짜}` | 8일 | 검색어별 점수와 순위 집계 |
| Redis 자료구조 | 인기 검색어 중복 방지 | `search:dedupe:{회원}:{검색어}` | 5분 | 같은 회원의 반복 검색 점수 증가 방지 |

`productSearch`, `productDetail`, `productList`, `cartItems`는 Spring Cache 추상화를 사용하는 **조회 캐시**다. 인기 검색어는 `@Cacheable` 결과 저장이 아니라 Redis Sorted Set을 직접 사용하는 **집계 데이터**라는 차이가 있다.

---

## 3. 1단계: 상품 검색 v1 — 캐시 없는 기준 API

### 3.1 왜 v1이 필요한가?

성능 개선을 말하려면 비교 대상이 필요하다. v1은 캐시 없이 MySQL을 조회하는 기준 API다.

```text
GET /api/v1/products/search?keyword=권총&tier=yellow&page=0&size=20&sort=POPULAR
```

관련 파일:

- `src/main/java/org/example/murderhelp/domain/product/controller/ProductController.java`
- `src/main/java/org/example/murderhelp/domain/product/service/ProductService.java`
- `src/main/java/org/example/murderhelp/domain/product/repository/ProductRepositoryImpl.java`

### 3.2 v1 요청 흐름

```text
1. ProductController.searchProducts()
2. JWT에서 회원 등급 확인
3. 요청한 상품 tier 접근 가능 여부 확인
4. ProductService.searchProducts()
5. ProductRepositoryImpl.searchProducts()
6. MySQL: product.name LIKE '%권총%' + tier 조건 + paging/count 쿼리
7. PageResponse로 변환하여 응답
```

`ProductRepositoryImpl`의 핵심 조건은 아래와 같다.

```java
product.name.contains(keyword) // SQL에서는 LIKE '%keyword%'
product.tier.eq(tier)
product.status.ne(ProductStatus.DISCONTINUED)
```

`contains()` 때문에 앞에 와일드카드가 붙는 `LIKE '%권총%'` 검색이 발생한다. 5만 건 이상에서는 MySQL이 많은 행을 살펴봐야 하므로, 반복되는 동일 검색어에 캐시를 적용할 이유가 생긴다.

---

## 4. 2단계: 상품 검색 v2 — Caffeine Local Cache

### 4.1 초기 목표

v1을 유지한 채 같은 요청 계약의 v2를 추가했다.

```text
GET /api/v2/products/search?keyword=권총&tier=yellow&page=0&size=20&sort=POPULAR
```

처음에는 Caffeine을 붙여 Spring Cache와 `@Cacheable` 동작을 학습했다.

관련 설정:

- `src/main/java/org/example/murderhelp/global/config/cache/LocalCacheConfig.java`
- `src/main/java/org/example/murderhelp/global/config/cache/CacheNames.java`

```java
cacheManager.registerCustomCache(CacheNames.PRODUCT_SEARCH, Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(500)
        .build());
```

- `expireAfterWrite(10분)`: 저장 후 10분 뒤 만료
- `maximumSize(500)`: 오래된 캐시가 메모리를 무한히 차지하지 않도록 최대 항목 수 제한

### 4.2 `@Cacheable`이 실제로 하는 일

`ProductService.searchProductsCached()`에는 다음과 같은 어노테이션이 있다.

```java
@Cacheable(
    cacheNames = CacheNames.PRODUCT_SEARCH,
    key = "'keyword:' + #keyword + ':tier:' + #tier"
        + " + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize"
        + " + ':sort:' + #sort"
)
public PageResponse<ProductResponse> searchProductsCached(...) {
    return doSearch(...);
}
```

Spring AOP 프록시가 메서드 호출을 가로채서 동작한다.

```text
처음 호출
  key 생성
  → productSearch에 key가 없음
  → 메서드 본문(doSearch, MySQL)이 실행됨
  → 반환값을 캐시에 저장

같은 조건으로 다시 호출
  key 생성
  → productSearch에 key가 있음
  → 메서드 본문을 실행하지 않음
  → 캐시 값을 바로 반환
```

### 4.3 캐시 키를 길게 만드는 이유

`keyword=권총`만 키로 사용하면 아래 두 요청이 같은 결과를 받는 오류가 생길 수 있다.

```text
권총 / yellow / 0페이지 / 가격 낮은순
권총 / yellow / 1페이지 / 최신순
```

그래서 **검색어, 상품 등급, 페이지 번호, 페이지 크기, 정렬 방식**을 모두 키에 포함했다. 이처럼 `value`(캐시 공간 이름)와 `key`(그 공간 안의 데이터 식별자)를 분리하는 것이 중요하다.

### 4.4 권한 검증을 캐시 바깥에서 하는 이유

v2 컨트롤러는 캐시 조회 전에 `assertProductTierAccess()`를 수행한다.

```text
회원 yellow가 green 상품을 요청
→ 캐시에 결과가 있더라도 권한 검증에서 차단
```

캐시 히트 시 `@Cacheable` 메서드 본문은 건너뛰므로, 권한 검증을 캐시 메서드 내부에만 두면 캐시 히트에서 검증이 생략될 위험이 있다. 그래서 컨트롤러에서 먼저 검증한다.

> 주의: `@Cacheable`은 같은 클래스 내부 메서드 호출에는 적용되지 않을 수 있다. 프록시를 우회하기 때문이다. 그래서 `ProductController`가 `ProductService.searchProductsCached()`를 직접 호출하도록 구성했다.

---

## 5. 3단계: Caffeine에서 Redis Remote Cache로 전환

### 5.1 왜 Redis로 바꿨나?

서버가 2대 이상이면 Caffeine은 서버마다 별도로 존재한다.

```text
사용자 A → 서버 1 → 서버 1의 Caffeine에 캐시 저장
사용자 B → 서버 2 → 서버 2의 Caffeine에는 캐시가 없음 → MySQL 재조회
```

Redis는 모든 서버가 같은 Redis 인스턴스를 보므로 이 문제가 없다.

```text
사용자 A → 서버 1 ┐
사용자 B → 서버 2 ├→ AWS Redis → 같은 캐시 데이터 공유
사용자 C → 서버 3 ┘
```

### 5.2 어떤 설정이 선택되는가?

`spring.cache.type` 값에 따라 CacheManager 구현체가 선택된다.

| 설정 | 활성 설정 클래스 | 실제 저장 위치 |
| --- | --- | --- |
| `caffeine` | `LocalCacheConfig` | 현재 JVM 메모리 |
| `redis` | `RedisCacheConfig` | Redis 서버 |

두 설정 클래스에는 `@ConditionalOnProperty`가 붙어 있어 동시에 활성화되지 않는다.

```java
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
```

운영 설정인 `src/main/resources/application-prod.yml`은 `spring.cache.type: redis`를 사용하고, 호스트·포트·비밀번호는 환경변수로 받는다.

```yaml
spring:
  data:
    redis:
      host: ${AWS_REDIS_HOST}
      port: ${AWS_REDIS_PORT:6379}
      password: ${AWS_REDIS_PASSWORD:}
```

따라서 소스 코드에는 AWS Redis 주소나 비밀번호를 넣지 않는다. AWS 담당자는 배포 환경에 `AWS_REDIS_HOST`, `AWS_REDIS_PORT`, 필요 시 `AWS_REDIS_PASSWORD`, `AWS_REDIS_SSL_ENABLED`를 주입하면 된다.

### 5.3 Serializer 설정

관련 파일: `src/main/java/org/example/murderhelp/global/config/redis/RedisConfig.java`

```java
StringRedisSerializer keySerializer = new StringRedisSerializer();
template.setKeySerializer(keySerializer);
template.setValueSerializer(redisValueSerializer);
```

| 대상 | Serializer | 이유 |
| --- | --- | --- |
| Key | `StringRedisSerializer` | Redis CLI에서 읽기 쉽고 키 충돌 파악이 편함 |
| Value | `GenericJacksonJsonRedisSerializer` | DTO, PageResponse 같은 Java 객체를 JSON으로 저장 |

`redisValueSerializer()`는 `findAndAddModules()`를 호출한다. 그래서 `LocalDate`, `LocalDateTime`처럼 Java 기본 JSON 처리만으로는 어려운 시간 타입도 Jackson 모듈을 통해 처리할 수 있다.

`RedisCacheConfig`에서는 같은 serializer를 Spring Cache에도 적용한다.

```java
.serializeKeysWith(... new StringRedisSerializer())
.serializeValuesWith(... redisValueSerializer)
```

### 5.4 캐시별 TTL

| 캐시 | TTL | 이유 |
| --- | ---: | --- |
| 상품 검색 | 10분 | 같은 검색어 반복 비율이 높고, 약간의 지연 허용 가능 |
| 상품 상세 | 5분 | 가격·재고·상태가 검색보다 더 자주 바뀔 수 있음 |
| 상품 목록 | 3분 | 목록은 페이지·정렬 조합이 많고 변경 반영을 조금 더 빠르게 함 |
| 장바구니 | 1분 | 개인 데이터이며 수량·구성이 자주 변경됨 |

TTL은 정답이 아니라 trade-off다. 더 길면 DB 부하는 줄지만 최신성이 떨어지고, 더 짧으면 최신성은 좋아지지만 캐시 효과가 줄어든다.

---

## 6. 4단계: 인기 검색어 — Redis ZSet 직접 사용

관련 파일:

- `src/main/java/org/example/murderhelp/domain/search/service/PopularSearchService.java`
- `src/main/java/org/example/murderhelp/domain/search/controller/SearchController.java`

API는 다음과 같다.

```text
GET /api/searches/popular?limit=10
```

### 6.1 왜 ZSet인가?

인기 검색어는 “검색어 → 검색 횟수 점수”와 “점수 높은 순 정렬”이 동시에 필요하다.

Redis ZSet(Sorted Set)은 멤버마다 score를 저장하고 score 기준 정렬을 제공한다.

```text
search:popular:daily:2026-09-13
  권총       120점
  소총        98점
  방탄조끼    73점
```

코드에서는 다음 Redis 연산을 사용한다.

```java
stringRedisTemplate.opsForZSet().incrementScore(key, normalizedKeyword, 1);
stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
```

- `incrementScore`: 검색 성공 시 점수 1 증가 (`ZINCRBY`에 해당)
- `reverseRangeWithScores`: 높은 점수부터 상위 N개 조회 (`ZREVRANGE`에 해당)

### 6.2 같은 회원이 새로고침만 하면 순위가 올라가지 않나?

중복 방지 키를 먼저 사용한다.

```text
search:dedupe:{memberId}:{normalizedKeyword}
```

이 키를 `setIfAbsent(..., 5분)`으로 생성한다.

```text
5분 안에 같은 회원이 같은 검색어를 다시 검색
→ dedupe 키가 이미 있음
→ ZSet 점수를 증가시키지 않음
```

검색어는 공백을 하나로 합치고 소문자로 변환해 정규화한다. 예를 들어 `" 권총  "`과 `"권총"`을 같은 검색어로 집계하기 위해서다.

### 6.3 왜 별도의 CacheEvict가 없나?

인기 검색어 ZSet은 상품 조회 결과 캐시가 아니라 시간별 집계 데이터다. 일자별 키를 사용하고 8일 TTL을 두므로 하루가 바뀌면 새 키에서 새 집계가 시작된다. 따라서 상품 수정 시 인기 검색어 ZSet을 지우지 않는다.

---

## 7. 5단계: 상품 상세·목록 캐시

### 7.1 상품 상세

관련 메서드: `ProductService.getProduct()`

```java
@Cacheable(
    cacheNames = CacheNames.PRODUCT_DETAIL,
    key = "'id:' + #productId + ':memberTier:' + #memberTier"
)
public ProductDetailResponse getProduct(ProductTier memberTier, Long productId) { ... }
```

상품 ID뿐 아니라 회원 등급도 키에 포함했다. 상품 접근 가능 여부가 회원 등급에 따라 달라지므로, 서로 다른 등급이 같은 상세 캐시를 공유하지 않게 하기 위해서다.

### 7.2 상품 목록

관련 메서드: `ProductService.getProducts()`

목록 응답은 카테고리, 하위 카테고리, 요청 상품 등급, 회원 등급, 페이지, 페이지 크기, 정렬에 따라 달라진다. 그래서 모두 캐시 키에 포함한다.

```text
category / subCategory / requestedTier / memberTier / page / size / sort
```

목록·검색처럼 페이지 조합이 많은 API는 캐시 키 수가 빠르게 늘어날 수 있다. 그래서 최대 캐시 수(Caffeine)와 짧은 TTL(Redis)을 함께 둔다.

---

## 8. 6단계: Cache Eviction — 원본 변경 후 최신성 보장

### 8.1 왜 필요한가?

상품 재고가 10개일 때 상세 캐시에 `stockQuantity=10`이 저장되어 있다고 가정한다.

```text
주문 성공 → MySQL 재고 9개
캐시에는 아직 10개
→ 캐시를 지우지 않으면 사용자에게 오래된 재고가 보임
```

TTL이 끝날 때까지 기다리는 대신, 변경 성공 직후 캐시를 삭제한다.

### 8.2 상품 캐시 무효화 구현

관련 파일: `src/main/java/org/example/murderhelp/domain/product/service/ProductCacheEvictionService.java`

```java
@CacheEvict(
    cacheNames = {
        CacheNames.PRODUCT_DETAIL,
        CacheNames.PRODUCT_LIST,
        CacheNames.PRODUCT_SEARCH
    },
    allEntries = true
)
public void evictProductCaches() { }
```

메서드 본문은 비어 있다. 실제 삭제 동작은 Spring AOP가 `@CacheEvict`를 보고 수행한다.

`allEntries = true`를 쓴 이유는 다음과 같다.

- 상세: 상품 ID와 회원 tier 조합이 여러 개
- 목록: 카테고리·페이지·정렬 조합이 많음
- 검색: 키워드·페이지·정렬 조합이 많음

모든 영향을 받은 개별 키를 계산해 지우는 것보다, 상품 데이터 변경 시 세 캐시 공간을 비우는 단순한 방식이 현재 규모에서는 안전하다.

### 8.3 현재 실제 호출 지점: 주문 생성

관련 파일: `src/main/java/org/example/murderhelp/domain/order/facade/OrderFacade.java`

```text
주문할 장바구니 조회
→ 상품 재고 decreaseStock()
→ 주문 저장
→ 주문한 장바구니 항목 삭제
→ productCacheEvictionService.evictProductCaches()
```

주문으로 `stockQuantity`와 경우에 따라 상품 상태(`SOLD_OUT`)가 바뀌므로 상품 상세·목록·검색 모두에 영향을 준다.

### 8.4 아직 연결되지 않은 상품 변경 지점

| 변경 상황 | 현재 상태 | 추후 처리 |
| --- | --- | --- |
| 주문 생성 재고 차감 | 구현됨 | 상품 캐시 3종 전체 삭제 |
| 상품명·가격·카테고리·tier 수정 | 수정 API 자체가 아직 없음 | 수정 성공 후 `evictProductCaches()` 호출 |
| 판매 상태 변경 | 상태 변경 API가 아직 없음 | 변경 성공 후 `evictProductCaches()` 호출 |
| 결제 취소·환불 재고 복구 | 취소 흐름은 있으나 실제 `restoreStock()` 호출이 주석 상태 | 재고 복구 직후 `evictProductCaches()` 호출 |

나중에 데이터와 트래픽이 훨씬 커지면 상세 캐시는 상품 ID별 삭제, 목록·검색은 버전 키 전략 같은 방식으로 더 세밀하게 최적화할 수 있다. 지금은 정확성과 이해하기 쉬운 구조를 우선했다.

---

## 9. 7단계: 장바구니 조회 캐시와 회원별 무효화

관련 파일:

- `src/main/java/org/example/murderhelp/domain/cart/service/CartService.java`
- `src/main/java/org/example/murderhelp/domain/cart/service/CartCacheEvictionService.java`

### 9.1 조회 캐시

```java
@Cacheable(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
public List<CartItemDetailResponse> getItems(Long memberId) { ... }
```

장바구니는 회원마다 완전히 다른 데이터다. 그래서 전역 `allEntries` 삭제가 아니라 `member:{memberId}` 키를 사용한다.

```text
cartItems::member:1
cartItems::member:2
```

Spring Redis Cache는 보통 `캐시이름::키` 형태로 Redis 키를 만든다.

### 9.2 쓰기 후 해당 회원만 삭제

```java
@CacheEvict(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
public void evictCartItems(Long memberId) { }
```

`CartService`는 아래 작업이 성공한 뒤 이 메서드를 호출한다.

| 장바구니 변경 | 왜 삭제해야 하나? |
| --- | --- |
| 상품 담기 | 품목 또는 수량이 증가 |
| 수량 변경 | 수량이 달라짐 |
| 개별 삭제 | 품목이 사라짐 |
| 선택 삭제 | 주문 등으로 여러 품목이 사라짐 |

회원 1의 장바구니를 고쳐도 회원 2의 `cartItems::member:2`는 삭제되지 않는다. 상품 캐시의 `allEntries=true`와 장바구니의 회원별 삭제가 다른 이유다.

---

## 10. 테스트 코드에서 확인한 것

주요 테스트 파일:

| 테스트 | 검증 대상 |
| --- | --- |
| `ProductCacheEvictionServiceTest` | 상품 캐시 3종이 모두 삭제되는지 |
| `OrderFacadeTest` | 주문 성공 뒤 장바구니 삭제와 상품 캐시 삭제가 호출되는지 |
| `CartCacheEvictionServiceTest` | 한 회원의 장바구니 캐시만 삭제되고 다른 회원 캐시는 남는지 |
| `CartServiceTest` | 선택 장바구니 삭제 뒤 Evict 호출 여부 |
| `LocalCacheConfigTest` | Caffeine에 캐시가 등록되는지 |
| `RedisCacheConfigTest` | Redis 캐시별 TTL이 의도대로 설정되는지 |

테스트는 “Redis가 실제로 빠른가”보다 “캐시 키, TTL, 삭제 흐름이 코드대로 동작하는가”를 검증한다. 성능 자체는 다음 절의 k6로 검증한다.

---

## 11. 성능 테스트: 5만 건 + k6

### 11.1 더미 데이터

파일: `scripts/seed_product_search_performance_50k.sql`

- `PERF-` 접두어 상품 50,000건 생성
- `권총`, `소총`, `방탄조끼`, `탄약`, `전술 장비`를 각각 10,000건 배치
- 재실행 시 `PERF-` 상품만 지우고 다시 생성
- MySQL CLI 실행 시 UTF-8을 위해 `--default-character-set=utf8mb4` 사용

### 11.2 k6의 역할

k6는 캐시 기능이 아니라 **부하 테스트 도구**다.

```text
k6 가상 사용자 여러 명
  → v1 검색 API 또는 v2 검색 API에 반복 요청
  → 평균 응답 시간, p95, 처리량, 실패율 기록
```

파일:

- `scripts/k6/product-search.js`
- `scripts/k6/README.md`

### 11.3 실제 예비 측정 결과

5 VU, 3초 ramp-up, 8초 유지, 2초 ramp-down, `권총/yellow` 검색 조건으로 로컬에서 측정했다.

| API | 캐시 상태 | 평균 응답 시간 | p95 | 처리량 | 실패율 |
| --- | --- | ---: | ---: | ---: | ---: |
| v1 | MySQL 직접 조회 | 58.59ms | 60.59ms | 24.97 req/s | 0% |
| v2 | Redis 초기 상태 | 5.39ms | 6.56ms | 37.61 req/s | 0% |
| v2 | Redis 워밍 후 | 5.96ms | 10.32ms | 37.53 req/s | 0% |

v2는 평균 응답 시간이 약 90% 낮고 처리량은 약 1.5배 높았다. 단, 이 결과는 로컬 단일 머신의 짧은 예비 측정이다. v2 초기 상태도 첫 요청 이후 같은 키가 캐시되므로 모든 요청이 cold cache인 것은 아니다.

더 자세한 결과 보고서는 `docs/product-search-performance-test.md`에 있다.

---

## 12. 요청 하나를 끝까지 따라가기

### 12.1 v2 상품 검색, 캐시 히트

```text
클라이언트
  → GET /api/v2/products/search?keyword=권총&tier=yellow...
  → JWT 인증 및 회원 등급 확인
  → yellow 상품 접근 가능 여부 확인
  → productSearch::keyword:권총:tier:yellow:page:0:size:20:sort:POPULAR 확인
  → Redis에 값 존재
  → MySQL을 호출하지 않고 PageResponse 반환
  → PopularSearchService가 ZSet 점수 집계 시도
```

### 12.2 주문 성공, 캐시 무효화

```text
클라이언트 주문 요청
  → 장바구니 선택 항목 조회
  → 상품 재고 차감
  → 주문/주문상품 저장
  → 선택 장바구니 항목 삭제
      → 해당 회원 cartItems 캐시 삭제
  → productDetail / productList / productSearch 전체 삭제
  → 다음 상품 조회는 MySQL에서 최신 재고를 읽고 캐시를 새로 저장
```

---

## 13. 앞으로 코드를 읽거나 확장할 때 체크할 질문

새 API에 캐시를 붙이기 전에 아래를 먼저 판단한다.

1. 이 API는 읽기 비중이 높고 같은 요청이 반복되는가?
2. 응답이 사용자별·등급별로 달라지는가? 그렇다면 그 값이 캐시 키에 포함됐는가?
3. 원본 데이터가 바뀌는 메서드는 어디인가?
4. 변경 직후 즉시 Evict할지, TTL로 자연 만료를 기다려도 되는가?
5. Redis 장애 시 DB 조회로 정상 동작할 수 있는가?
6. 캐시가 실제로 성능을 개선했는지 k6 같은 도구로 측정했는가?

### 현재 다음 구현 후보

- 결제 취소/환불에서 실제 `restoreStock()` 수행 후 상품 캐시 무효화 연결
- 관리자 상품 수정 API에 상품 캐시 무효화 연결
- 더 큰 부하 조건(20 VU 이상, 긴 유지 시간)으로 k6 성능 보고서 보강
- 운영 Redis에서 hit rate, 메모리 사용량, evicted keys를 모니터링

---

## 한 줄 정리

**MySQL은 정답 데이터, Redis/Caffeine은 빠른 복사본, `@Cacheable`은 복사본을 읽고 채우는 기능, `@CacheEvict`는 원본 변경 후 오래된 복사본을 지우는 기능, k6는 이 구조가 실제로 빨라졌는지 측정하는 도구다.**
