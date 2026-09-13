# 캐싱 구현 학습 노트

이 문서는 프로젝트에서 구현한 캐싱 기능을 처음부터 순서대로 설명한다. 목표는 “코드가 어느 파일에 있고, 요청이 들어왔을 때 무엇이 실행되며, 왜 그 방식으로 설계했는지”를 이해하는 것이다.

---

## 1. 캐시를 한 문장으로 이해하기

**MySQL은 원본 데이터, Redis/Caffeine은 빠른 복사본, `@Cacheable`은 복사본을 읽고 채우는 기능, `@CacheEvict`는 원본이 바뀐 뒤 오래된 복사본을 지우는 기능이다.**

캐시는 DB를 대체하지 않는다. 캐시가 비어 있거나 Redis가 재시작되어도 MySQL에 원본 데이터가 있으므로, 다음 조회가 DB를 읽어 캐시를 다시 채울 수 있다.

```text
클라이언트
  → API
    → 캐시 확인
      → 있으면: 캐시 결과 반환 (cache hit)
      → 없으면: MySQL 조회 → 캐시에 저장 → 반환 (cache miss)
```

이 방식을 **Cache-aside(= Lazy Loading)** 라고 한다.

### TTL이란?

TTL(Time To Live)은 캐시 항목의 만료 시간이다. TTL이 지나면 해당 캐시는 자동 삭제되고, 다음 요청이 DB에서 최신 데이터를 다시 읽는다.

하지만 TTL만으로 최신성을 보장할 수는 없다. 예를 들어 재고가 10개에서 9개로 바뀌었는데 상세 캐시 TTL이 5분이면, 최대 5분 동안 이전 재고가 보일 수 있다. 그래서 변경 성공 직후에는 `@CacheEvict`로 캐시를 즉시 삭제한다.

---

## 2. Caffeine과 Redis의 차이

| 구분 | Caffeine | Redis |
| --- | --- | --- |
| 위치 | 애플리케이션 서버 JVM 메모리 | 별도 Redis 서버 메모리 |
| 서버가 2대일 때 | 각 서버 캐시가 서로 다름 | 모든 서버가 같은 캐시 공유 |
| 장점 | 매우 빠르고 설정이 간단 | Scale-out 환경에서도 일관됨 |
| 프로젝트 사용 목적 | 로컬/테스트 및 학습 단계 | AWS 배포 환경 Remote Cache |

```text
Caffeine
사용자 A → 서버 1의 메모리
사용자 B → 서버 2의 메모리 (서버 1의 값을 모름)

Redis
사용자 A → 서버 1 ┐
사용자 B → 서버 2 ├→ 하나의 Redis
사용자 C → 서버 3 ┘
```

그래서 처음에는 Caffeine으로 Spring Cache 동작을 익히고, 배포 환경에는 Redis를 사용하도록 발전시켰다.

---

## 3. 현재 구현된 캐시와 Redis 기능

| 종류 | 대상 | 캐시 이름 또는 Redis Key | TTL | 목적 |
| --- | --- | --- | ---: | --- |
| 조회 캐시 | 상품 검색 v2 | `productSearch` | 10분 | 반복되는 MySQL `LIKE` 검색 감소 |
| 조회 캐시 | 상품 상세 | `productDetail` | 5분 | 반복 상세 조회 감소 |
| 조회 캐시 | 상품 목록 | `productList` | 3분 | 목록·페이징 반복 조회 감소 |
| 조회 캐시 | 장바구니 전체 조회 | `cartItems` | 1분 | 개인 장바구니 반복 조회 감소 |
| Redis ZSet | 인기 검색어 | `search:popular:daily:{날짜}` | 8일 | 검색어 점수·순위 집계 |
| Redis String | 인기 검색어 중복 방지 | `search:dedupe:{회원}:{검색어}` | 5분 | 반복 검색 점수 증가 방지 |

`productSearch`, `productDetail`, `productList`, `cartItems`는 Spring의 `@Cacheable`을 쓰는 **조회 결과 캐시**다.

인기 검색어는 조회 결과를 저장하는 캐시가 아니다. Redis Sorted Set에 검색 횟수 점수를 저장하는 **집계 기능**이다.

---

## 4. 1단계: 상품 검색 v1 — 캐시 없는 기준 API

API:

```text
GET /api/v1/products/search?keyword=권총&tier=yellow&page=0&size=20&sort=POPULAR
```

관련 코드:

- `src/main/java/org/example/murderhelp/domain/product/controller/ProductController.java`
- `src/main/java/org/example/murderhelp/domain/product/service/ProductService.java`
- `src/main/java/org/example/murderhelp/domain/product/repository/ProductRepositoryImpl.java`

### v1 요청 흐름

```text
1. ProductController.searchProducts()
2. JWT에서 회원 등급 확인
3. 요청한 상품 tier 접근 가능 여부 확인
4. ProductService.searchProducts()
5. ProductRepositoryImpl.searchProducts()
6. MySQL 검색 + paging + count 쿼리
7. PageResponse로 변환하여 반환
```

Repository의 핵심 검색 조건은 다음과 같다.

```java
product.name.contains(keyword) // SQL: LIKE '%keyword%'
product.tier.eq(tier)
product.status.ne(ProductStatus.DISCONTINUED)
```

`contains()`는 실제 SQL에서 `LIKE '%권총%'`가 된다. 앞에 `%`가 붙는 검색은 인덱스를 효율적으로 사용하기 어려워 많은 데이터를 살펴볼 수 있다. 5만 건 이상의 데이터에서 동일 검색어가 반복되면 캐시가 특히 효과적이다.

v1은 그대로 남겨 둔다. v2가 정말 개선됐는지 성능 테스트에서 비교하는 기준이기 때문이다.

---

## 5. 2단계: 상품 검색 v2 — `@Cacheable` 적용

API:

```text
GET /api/v2/products/search?keyword=권총&tier=yellow&page=0&size=20&sort=POPULAR
```

핵심 메서드는 `ProductService.searchProductsCached()`이다.

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

### `@Cacheable`이 실제로 하는 일

```text
첫 번째 요청
  key 생성
  → productSearch에 key 없음
  → doSearch() 실행
  → MySQL 조회
  → 반환값을 캐시에 저장

두 번째 요청 (조건 동일)
  key 생성
  → productSearch에 key 있음
  → doSearch()와 MySQL 조회를 건너뜀
  → 캐시 값을 바로 반환
```

즉, 메서드 본문을 항상 실행하는 것이 아니다. 캐시 히트라면 Spring AOP 프록시가 저장된 값을 반환한다.

### 캐시 키를 길게 만드는 이유

아래 두 요청은 결과가 다르다.

```text
권총 / yellow / 0페이지 / POPULAR
권총 / yellow / 1페이지 / PRICE_ASC
```

검색어만 키에 넣으면 서로 다른 페이지와 정렬이 같은 캐시 값을 받는 오류가 생긴다. 그래서 검색어, 상품 등급, 페이지 번호, 페이지 크기, 정렬 방식을 모두 키에 포함했다.

### 권한 검증을 캐시 바깥에서 하는 이유

v2는 캐시 조회 전에 `assertProductTierAccess()`를 실행한다.

```text
yellow 회원이 green 상품 요청
→ 캐시에 green 결과가 있어도 권한 검증에서 차단
```

캐시 히트 시 `@Cacheable` 메서드 본문은 실행되지 않는다. 권한 검증을 캐시 메서드 내부에만 두면 캐시 히트에서 검증이 생략될 수 있으므로, 컨트롤러에서 먼저 검증한다.

> `@Cacheable`은 같은 클래스 안에서 `this.someMethod()`처럼 호출하면 AOP 프록시를 우회할 수 있다. 그래서 컨트롤러가 `ProductService.searchProductsCached()`를 직접 호출한다.

---

## 6. 3단계: Caffeine Local Cache에서 Redis Remote Cache로 전환

관련 코드:

- `src/main/java/org/example/murderhelp/global/config/cache/LocalCacheConfig.java`
- `src/main/java/org/example/murderhelp/global/config/cache/RedisCacheConfig.java`
- `src/main/java/org/example/murderhelp/global/config/cache/CacheNames.java`
- `src/main/java/org/example/murderhelp/global/config/redis/RedisConfig.java`

### CacheManager 선택

`spring.cache.type` 값에 따라 설정이 선택된다.

| 설정값 | 활성 설정 클래스 | 저장 위치 |
| --- | --- | --- |
| `caffeine` | `LocalCacheConfig` | 현재 JVM 메모리 |
| `redis` | `RedisCacheConfig` | Redis 서버 |

각 설정 클래스에는 다음 조건이 있다.

```java
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
```

운영 환경 `application-prod.yml`은 `spring.cache.type: redis`를 사용한다. AWS Redis 주소와 비밀번호는 코드에 쓰지 않고 환경변수로 주입한다.

```yaml
spring:
  data:
    redis:
      host: ${AWS_REDIS_HOST}
      port: ${AWS_REDIS_PORT:6379}
      password: ${AWS_REDIS_PASSWORD:}
```

### Serializer 설정

Redis key와 value는 바이트로 저장되므로 serializer가 필요하다.

| 대상 | Serializer | 이유 |
| --- | --- | --- |
| Key | `StringRedisSerializer` | Redis CLI에서 읽기 쉽고 키 확인이 편함 |
| Value | `GenericJacksonJsonRedisSerializer` | DTO·PageResponse 같은 Java 객체를 JSON으로 저장 |

`RedisConfig.redisValueSerializer()`는 `findAndAddModules()`를 사용한다. `LocalDate`, `LocalDateTime` 등 시간 타입을 JSON으로 변환할 때 필요한 Jackson 모듈도 함께 등록된다.

---

## 7. 4단계: 인기 검색어 — Redis ZSet 직접 사용

관련 코드:

- `src/main/java/org/example/murderhelp/domain/search/service/PopularSearchService.java`
- `src/main/java/org/example/murderhelp/domain/search/controller/SearchController.java`

API:

```text
GET /api/searches/popular?limit=10
```

인기 검색어에는 “검색어별 횟수”와 “횟수 높은 순 정렬”이 동시에 필요하다. Redis ZSet은 member와 score를 함께 저장하고 score순 정렬을 제공한다.

```text
search:popular:daily:2026-09-13
  권총       120점
  소총        98점
  방탄조끼    73점
```

핵심 코드:

```java
stringRedisTemplate.opsForZSet().incrementScore(key, normalizedKeyword, 1);
stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
```

- `incrementScore`: 검색 성공 시 점수 1 증가 (`ZINCRBY`)
- `reverseRangeWithScores`: 높은 점수부터 상위 N개 조회 (`ZREVRANGE`)

### 중복 검색 방지

```text
search:dedupe:{memberId}:{normalizedKeyword}
```

같은 회원이 5분 안에 같은 검색어를 다시 검색하면 중복 방지 키가 이미 존재한다. 이 경우 ZSet 점수를 올리지 않는다.

인기 검색어는 상품 조회 결과 캐시가 아니라 날짜별 집계 데이터다. 일자별 키와 8일 TTL을 사용하므로 상품 수정 시 `CacheEvict`로 지우지 않는다.

---

## 8. 5단계: 상품 상세·목록 캐시

### 상품 상세 캐시

`ProductService.getProduct()`에 적용되어 있다.

```java
@Cacheable(
    cacheNames = CacheNames.PRODUCT_DETAIL,
    key = "'id:' + #productId + ':memberTier:' + #memberTier"
)
```

상품 접근 가능 여부가 회원 등급에 따라 달라진다. 그래서 상품 ID만이 아니라 `memberTier`도 키에 포함한다.

### 상품 목록 캐시

`ProductService.getProducts()`에는 아래 정보가 키에 포함된다.

```text
category / subCategory / requestedTier / memberTier / page / size / sort
```

목록 응답은 카테고리, 등급, 페이지, 정렬에 따라 다르므로 모두 구분해야 한다. 조합이 많기 때문에 목록 TTL은 3분으로 비교적 짧다.

---

## 9. 6단계: 상품 캐시 무효화

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

메서드 본문이 비어 있어도 Spring AOP가 어노테이션을 보고 캐시 삭제를 수행한다.

### 왜 `allEntries = true`인가?

상품 상세는 상품 ID와 회원 등급 조합이 있고, 목록·검색은 키워드·페이지·정렬 조합이 많다. 현재 규모에서는 영향을 받은 모든 개별 키를 계산하는 것보다 상품 캐시 3종을 전체 삭제하는 방식이 단순하고 안전하다.

### 현재 실제 호출 지점: 주문 생성

`OrderFacade.createOrder()`의 흐름은 다음과 같다.

```text
장바구니 선택 항목 조회
→ 상품 재고 decreaseStock()
→ 주문 저장
→ 선택 장바구니 항목 삭제
→ productCacheEvictionService.evictProductCaches()
```

주문으로 재고가 줄고, 재고가 0이면 판매 상태가 `SOLD_OUT`으로 바뀔 수 있다. 따라서 상품 상세·목록·검색 결과 모두에 영향을 준다.

### 아직 연결되지 않은 변경 지점

| 변경 상황 | 현재 상태 | 나중에 할 일 |
| --- | --- | --- |
| 주문 생성 재고 차감 | 구현됨 | 상품 캐시 3종 전체 삭제 |
| 상품명·가격·카테고리·tier 수정 | 수정 API 없음 | 수정 성공 후 `evictProductCaches()` 호출 |
| 판매 상태 변경 | 상태 변경 API 없음 | 변경 성공 후 `evictProductCaches()` 호출 |
| 결제 취소·환불 재고 복구 | `restoreStock()` 호출이 아직 주석 상태 | 재고 복구 후 `evictProductCaches()` 호출 |

---

## 10. 7단계: 장바구니 조회 캐시

관련 파일:

- `src/main/java/org/example/murderhelp/domain/cart/service/CartService.java`
- `src/main/java/org/example/murderhelp/domain/cart/service/CartCacheEvictionService.java`

조회 메서드:

```java
@Cacheable(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
public List<CartItemDetailResponse> getItems(Long memberId) { ... }
```

장바구니는 회원마다 완전히 다른 데이터다. 그래서 상품 캐시처럼 전체 삭제하지 않고 회원별 키를 사용한다.

```text
cartItems::member:1
cartItems::member:2
```

장바구니 변경 후 삭제 메서드:

```java
@CacheEvict(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
public void evictCartItems(Long memberId) { }
```

`CartService`는 다음 작업 성공 뒤 해당 회원의 캐시만 지운다.

| 변경 | 캐시를 지우는 이유 |
| --- | --- |
| 상품 담기 | 품목 또는 수량 증가 |
| 수량 변경 | 기존 수량 변경 |
| 개별 삭제 | 품목 제거 |
| 선택 삭제 | 주문 등으로 여러 품목 제거 |

회원 1이 장바구니를 고쳐도 회원 2의 장바구니 캐시는 삭제되지 않는다.

---

## 11. 테스트 코드에서 확인한 것

| 테스트 | 검증 대상 |
| --- | --- |
| `ProductCacheEvictionServiceTest` | 상품 캐시 3종 전체 삭제 |
| `OrderFacadeTest` | 주문 성공 뒤 장바구니 삭제와 상품 캐시 삭제 호출 |
| `CartCacheEvictionServiceTest` | 한 회원의 장바구니 캐시만 삭제 |
| `CartServiceTest` | 선택 삭제 뒤 Evict 호출 |
| `LocalCacheConfigTest` | Caffeine 캐시 등록 |
| `RedisCacheConfigTest` | Redis TTL 설정 |

단위·통합 테스트는 캐시 키, TTL, 무효화 흐름이 코드대로 동작하는지를 확인한다. “정말 빨라졌는지”는 k6로 측정한다.

---

## 12. 5만 건 더미 데이터와 k6 성능 테스트

### 더미 데이터

파일: `scripts/seed_product_search_performance_50k.sql`

- `PERF-` 접두어 상품 50,000건 생성
- `권총`, `소총`, `방탄조끼`, `탄약`, `전술 장비`를 각각 10,000건 배치
- 재실행 시 `PERF-` 상품만 지우고 다시 생성
- 한글 실행 시 `--default-character-set=utf8mb4` 필요

### k6는 무엇인가?

k6는 캐시 기능이 아니라 **부하 테스트 도구**다.

```text
k6 가상 사용자 여러 명
  → v1 또는 v2 검색 API에 반복 요청
  → 평균 응답 시간, p95, 처리량, 실패율 기록
```

파일:

- `scripts/k6/product-search.js`
- `scripts/k6/README.md`

### 실제 예비 측정 결과

조건: 5 VU, 3초 ramp-up, 8초 유지, 2초 ramp-down, `권총/yellow` 검색

| API | 캐시 상태 | 평균 응답 시간 | p95 | 처리량 | 실패율 |
| --- | --- | ---: | ---: | ---: | ---: |
| v1 | MySQL 직접 조회 | 58.59ms | 60.59ms | 24.97 req/s | 0% |
| v2 | Redis 초기 상태 | 5.39ms | 6.56ms | 37.61 req/s | 0% |
| v2 | Redis 워밍 후 | 5.96ms | 10.32ms | 37.53 req/s | 0% |

v2는 평균 응답 시간이 약 90% 낮고 처리량은 약 1.5배 높았다.

다만 로컬 단일 머신의 짧은 예비 측정이다. Redis 초기 상태도 첫 요청 이후에는 캐시가 채워지므로, 전체 요청이 계속 cold cache인 테스트는 아니다. 최종 보고서에서는 VU와 유지 시간을 늘리고 여러 번 실행해 평균을 기록하면 더 좋다.

---

## 13. 요청 흐름을 끝까지 따라가기

### v2 상품 검색: 캐시 히트

```text
GET /api/v2/products/search?keyword=권총&tier=yellow...
→ JWT 인증 및 회원 등급 확인
→ 상품 등급 접근 검증
→ productSearch 캐시 키 확인
→ Redis에 값 존재
→ MySQL을 호출하지 않고 결과 반환
→ PopularSearchService가 ZSet 점수 집계 시도
```

### 주문 성공: 최신성 보장

```text
주문 요청
→ 장바구니 선택 항목 조회
→ 상품 재고 차감
→ 주문 저장
→ 선택 장바구니 삭제
  → 해당 회원 cartItems 캐시 삭제
→ productDetail / productList / productSearch 전체 삭제
→ 다음 상품 조회는 MySQL에서 최신 재고를 읽고 캐시를 다시 채움
```

---

## 14. 앞으로 캐시를 추가할 때 스스로 묻기

1. 이 API는 읽기 비중이 높고 같은 요청이 반복되는가?
2. 응답이 사용자별·등급별로 달라지는가? 그렇다면 그 값이 캐시 키에 포함됐는가?
3. 원본 데이터가 바뀌는 메서드는 어디인가?
4. 변경 직후 즉시 Evict할지, TTL 만료를 기다려도 되는가?
5. Redis가 잠시 실패해도 DB 조회로 서비스가 정상 동작할 수 있는가?
6. k6 등으로 실제 성능 개선을 숫자로 확인했는가?

### 다음 구현 후보

- 결제 취소/환불에서 실제 `restoreStock()` 수행 후 상품 캐시 무효화 연결
- 관리자 상품 수정 API에 상품 캐시 무효화 연결
- 20 VU 이상, 더 긴 유지 시간의 k6 최종 성능 보고서 작성
- AWS Redis에서 hit rate, 메모리 사용량, evicted keys 모니터링

