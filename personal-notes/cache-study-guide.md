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

## 12. 코드 리뷰: 실제 파일을 한 줄씩 읽는 방법

이 절은 “코드를 외우는” 대신, 각 코드가 요청 처리 중 언제 실행되고 어떤 문제를 막는지 따라가는 리뷰다. 아래 파일 경로는 현재 프로젝트 기준이다.

### 12-1. `CacheNames` — 문자열을 한 곳에서 관리하는 이유

파일: `src/main/java/org/example/murderhelp/global/config/cache/CacheNames.java`

```java
public static final String PRODUCT_SEARCH = "productSearch";
public static final String PRODUCT_DETAIL = "productDetail";
public static final String PRODUCT_LIST = "productList";
public static final String CART_ITEMS = "cartItems";
```

Spring Cache에서 `cacheNames`는 Redis의 논리적 저장소 이름이다. 예를 들어 검색 키가 `keyword:권총:tier:YELLOW:page:0...`이면 Redis에서는 보통 다음처럼 저장된다.

```text
productSearch::keyword:권총:tier:YELLOW:page:0:size:20:sort:POPULAR
```

문자열을 서비스마다 직접 쓰면 `"productSearch"`와 `"productsSearch"`처럼 오타가 나도 컴파일 오류가 나지 않는다. `CacheNames.PRODUCT_SEARCH` 상수로 묶어 두면 캐시 등록, 조회, 삭제가 같은 이름을 보게 된다. 이 클래스는 기능을 수행하지 않고 **이름의 불일치를 방지**하는 역할만 한다.

### 12-2. `@EnableCaching`과 `@ConditionalOnProperty` — 어떤 CacheManager가 선택되는가

파일:

- `global/config/cache/LocalCacheConfig.java`
- `global/config/cache/RedisCacheConfig.java`

두 설정 클래스 모두 `@Configuration`, `@EnableCaching`, `@ConditionalOnProperty`를 가진다. 단, 마지막 값은 서로 다르다.

```java
@Configuration
@EnableCaching
// LocalCacheConfig: havingValue = "caffeine"
// RedisCacheConfig: havingValue = "redis"
```

`@EnableCaching`은 Spring에게 `@Cacheable`, `@CacheEvict`를 해석하는 AOP 프록시를 만들라고 알린다. 이 설정이 없으면 서비스 메서드에 어노테이션이 있어도 평범한 메서드 호출처럼 동작한다.

`@ConditionalOnProperty`는 `spring.cache.type` 값으로 구현체를 하나만 고른다.

```text
spring.cache.type=caffeine
  → LocalCacheConfig의 CaffeineCacheManager만 Bean 등록

spring.cache.type=redis
  → RedisCacheConfig의 RedisCacheManager만 Bean 등록
```

둘을 동시에 등록하면 어느 `CacheManager`를 사용할지 모호해질 수 있다. 그래서 같은 `@Cacheable` 서비스 코드를 바꾸지 않고도, 로컬에서는 Caffeine, AWS에서는 Redis로 교체할 수 있다.

### 12-3. `RedisCacheConfig` — TTL은 캐시 이름별로 적용된다

파일: `global/config/cache/RedisCacheConfig.java`

```java
private static final Duration PRODUCT_SEARCH_TTL = Duration.ofMinutes(10);
private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(5);
private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(3);
private static final Duration CART_ITEMS_TTL = Duration.ofMinutes(1);
```

이 값은 “Redis 전체의 만료 시간”이 아니라 각 캐시 항목의 TTL이다. 이어지는 `withInitialCacheConfigurations(Map.of(...))`에서 캐시 이름과 TTL을 연결한다.

```java
RedisCacheConfiguration search = defaultConfiguration.entryTtl(PRODUCT_SEARCH_TTL);

Map.of(CacheNames.PRODUCT_SEARCH, search, ...)
```

검색은 같은 조건이 반복될 가능성이 크므로 10분, 자주 바뀌는 개인 장바구니는 1분으로 설정했다. TTL은 성능과 최신성의 절충값이다. 10분이 “정답”이라는 뜻은 아니며, 실제 요청 패턴과 AWS Redis 메모리 사용량을 보고 조정할 값이다.

```java
.disableCachingNullValues()
```

이 설정은 조회 결과가 `null`일 때 Redis에 저장하지 않는다. 존재하지 않는 값을 캐시해 DB 조회를 줄이는 전략도 가능하지만, 이 프로젝트는 상품이 새로 등록된 뒤 과거의 null 캐시가 남는 상황을 피하는 쪽을 선택했다.

### 12-4. `RedisConfig` — 직렬화는 왜 별도 Bean인가

파일: `global/config/redis/RedisConfig.java`

```java
template.setKeySerializer(new StringRedisSerializer());
template.setValueSerializer(redisValueSerializer);
```

Redis는 Java 객체를 그대로 저장할 수 없으므로 바이트로 바꾸는 과정이 필요하다.

- 키: `StringRedisSerializer`를 사용한다. `redis-cli`나 AWS에서 `productSearch::...`처럼 사람이 읽을 수 있다.
- 값: `GenericJacksonJsonRedisSerializer`를 사용한다. `PageResponse<ProductResponse>` 같은 반환 객체를 JSON으로 직렬화·역직렬화한다.

```java
.customize(builder -> builder.findAndAddModules())
```

이 줄은 Jackson이 시간 관련 모듈을 자동 등록하도록 한다. 응답 DTO에 `LocalDate` 또는 `LocalDateTime`이 있으면, 이 설정이 없을 때 Redis에서 값을 꺼내는 과정에 역직렬화 오류가 발생할 수 있다.

또한 `allowIfSubType("org.example.murderhelp")` 같은 타입 제한은 Redis 데이터의 타입 정보를 역직렬화할 수 있는 범위를 제한한다. 아무 클래스나 역직렬화하도록 열어 두는 것보다 안전한 구성이다.

### 12-5. 검색 v1/v2 — 같은 조회를 왜 둘로 나눴는가

파일:

- `domain/product/controller/ProductController.java`
- `domain/product/service/ProductService.java`

v1은 다음 한 줄로 서비스에 들어간다.

```java
productService.searchProducts(memberTier, requestedTier, keyword, productSort, pageable);
```

이 메서드는 `@Cacheable`이 없으므로 호출할 때마다 `doSearch()`와 repository 검색 쿼리를 실행한다. 그래서 성능 비교에서 **DB 직접 조회 기준선**이 된다.

v2에서는 먼저 권한을 검사하고, 캐시 메서드를 호출한다.

```java
productService.assertProductTierAccess(memberTier, requestedTier);
PageResponse<ProductResponse> response = productService.searchProductsCached(
        requestedTier, keyword, productSort, pageable
);
```

여기서 순서가 중요하다. 캐시 히트라면 `searchProductsCached()` 메서드 본문은 실행되지 않는다. 권한 검증을 메서드 본문에 두면 다음 문제가 생길 수 있다.

```text
낮은 등급 회원이 높은 등급 상품 요청
→ 이전에 저장된 캐시가 존재
→ 메서드 본문(권한 검사 포함)을 건너뜀
→ 권한 없는 결과를 반환할 위험
```

따라서 `assertProductTierAccess()`는 캐시 프록시 밖인 컨트롤러 호출 흐름에서 매 요청 실행한다. 캐시는 성능 최적화일 뿐, 인증·인가를 대체하면 안 된다.

### 12-6. `searchProductsCached()` — 캐시 키와 실제 조회의 경계

파일: `domain/product/service/ProductService.java`

```java
@Cacheable(
    cacheNames = CacheNames.PRODUCT_SEARCH,
    key = "'keyword:' + #keyword + ':tier:' + #tier"
        + " + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize"
        + " + ':sort:' + #sort"
)
public PageResponse<ProductResponse> searchProductsCached(...) {
    return doSearch(tier, keyword, sort, pageable);
}
```

`#keyword`, `#tier`, `#pageable.pageNumber` 등은 SpEL(Spring Expression Language)로, 메서드 인자를 읽는다. 결과가 달라질 수 있는 모든 요청 조건을 키에 넣는 것이 핵심이다.

| 키 구성 요소 | 빠지면 생길 오류 |
| --- | --- |
| `keyword` | `권총` 결과를 `소총` 검색에 반환 |
| `tier` | 다른 등급 상품 결과가 섞임 |
| `page`, `size` | 0페이지 결과를 1페이지에 반환하거나 항목 수가 틀림 |
| `sort` | 인기순 결과를 가격순 요청에 반환 |

반대로 `memberTier`는 검색 캐시 키에 넣지 않았다. v2는 먼저 `requestedTier` 접근을 검사하고, 검색 결과 자체는 `requestedTier`로 결정된다. 같은 `requestedTier`에 접근 가능한 회원이라면 결과는 같으므로 회원마다 중복 캐시를 만들 필요가 없다.

`doSearch()` 내부의 `normalizeRequiredKeyword()`는 공백 검색을 거부하고 앞뒤 공백을 제거한다. 다만 **캐시 키 생성은 메서드 본문보다 먼저** 발생한다는 점을 기억해야 한다. 현재 키는 원본 `keyword`를 사용하므로 `"권총"`과 `" 권총 "`은 같은 DB 검색 결과라도 서로 다른 캐시 항목이 될 수 있다. 기능 오류는 아니지만 적중률 측면에서는 개선 여지가 있다. 개선한다면 컨트롤러에서 정규화한 값을 전달하거나, 별도 request DTO/키 생성 방식을 두는 방향을 팀과 합의해야 한다.

### 12-7. AOP의 함정: 같은 클래스 내부 호출

`@Cacheable`은 `ProductService` 객체 자체에 코드를 삽입하는 방식이 아니라, Spring이 만든 프록시가 외부 호출을 가로채는 방식이다.

```text
Controller → Spring Proxy → ProductService.searchProductsCached()  // 캐시 동작
ProductService 내부 this.searchProductsCached()                    // 프록시 우회, 캐시 미동작
```

그래서 컨트롤러가 `searchProductsCached()`를 직접 호출하도록 나눴다. 서비스 안에서 v1 메서드가 v2 메서드를 호출하는 식으로 합치면 캐시가 조용히 적용되지 않을 수 있다. 이 설명은 “메서드에 `@Cacheable`을 붙였는데 repository가 매번 호출된다”는 문제를 디버깅할 때 특히 중요하다.

### 12-8. 상품 상세·목록 키에 등급이 포함된 이유

```java
key = "'id:' + #productId + ':memberTier:' + #memberTier"
```

상품 상세는 회원 등급에 따라 접근 가능 여부가 달라진다. 상품 ID만 키로 쓰면 높은 등급 회원이 채운 상세 결과를 낮은 등급 회원에게 반환할 위험이 있으므로 `memberTier`를 함께 넣었다.

상품 목록도 `category`, `subCategory`, `requestedTier`, `memberTier`, `page`, `size`, `sort`를 모두 키로 만든다. 특히 상품 목록은 요청 조합이 많아 키 수가 빠르게 늘 수 있다. 그래서 검색보다 짧은 3분 TTL을 둔다. 만약 AWS에서 메모리 사용량·evicted keys가 계속 증가한다면 목록 TTL이나 최대 메모리 정책을 우선 점검한다.

### 12-9. `ProductCacheEvictionService` — 본문이 빈 메서드가 필요한 이유

파일: `domain/product/service/ProductCacheEvictionService.java`

```java
@CacheEvict(
    cacheNames = {
        CacheNames.PRODUCT_DETAIL,
        CacheNames.PRODUCT_LIST,
        CacheNames.PRODUCT_SEARCH
    },
    allEntries = true
)
public void evictProductCaches() {
}
```

비어 있는 메서드는 실수가 아니다. Spring 프록시가 메서드 호출 전후에 `@CacheEvict`를 해석해 세 캐시를 삭제하므로, 별도 Java 본문이 필요 없다.

`allEntries = true`는 해당 캐시 이름 아래의 모든 키를 지운다는 뜻이다. 주문 후 바뀐 상품 하나의 상세 키만 지우고 싶어도, 목록과 검색은 어떤 키워드·페이지·정렬 조합에 그 상품이 들어 있는지 알아야 한다. 현재 구현은 다음의 선택이다.

```text
장점: 빠진 키 없이 최신성을 보장하고 구현이 단순함
단점: 관련 없는 상품 조회 캐시도 함께 사라져 다음 요청이 DB를 조회함
```

상품 데이터 변경 빈도가 매우 높아진다면 상품별 세밀한 무효화나 짧은 TTL을 검토할 수 있다. 현재 과제·규모에서는 안전한 전체 무효화가 더 적절하다.

### 12-10. 왜 Evict 전용 서비스를 분리했는가

`OrderFacade.createOrder()`는 재고를 줄인 뒤 다음을 호출한다.

```java
productCacheEvictionService.evictProductCaches();
```

이를 `ProductService` 안의 자기 메서드 호출로 두면 앞에서 설명한 AOP 프록시 우회가 다시 발생할 수 있다. 별도 Spring Bean인 `ProductCacheEvictionService`로 분리하면 `OrderFacade → Proxy → Evict 서비스` 경로가 보장된다.

호출 위치도 중요하다. 현재는 재고 차감·주문 저장·장바구니 선택 삭제 다음에 호출한다. 즉, 주문 처리 중 예외가 나면 캐시를 먼저 비워 불필요한 cache miss를 만드는 일을 줄인다. 다만 트랜잭션 커밋 직전/직후와 Redis 호출 실패까지 엄격히 다루는 운영 설계는 별도의 고급 주제다. 현재 구현의 목표는 **성공 흐름에서 오래된 상품 조회 결과를 즉시 제거하는 것**이다.

### 12-11. 장바구니 Evict는 왜 회원 한 명만 지우는가

파일: `domain/cart/service/CartCacheEvictionService.java`

```java
@CacheEvict(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
public void evictCartItems(Long memberId) {
}
```

장바구니는 회원별 데이터다. 상품 캐시처럼 `allEntries = true`를 쓰면 회원 1명이 수량을 바꿀 때 모든 회원의 장바구니 캐시가 사라진다. 그래서 조회 키와 삭제 키를 정확히 같은 형태로 맞춘다.

```text
조회: @Cacheable(... key = "'member:' + #memberId")
삭제: @CacheEvict (... key = "'member:' + #memberId")
```

둘 중 하나의 접두어라도 다르면 삭제가 성공해 보이지만 실제 조회 캐시가 남는다. 이 대칭성은 캐시 코드 리뷰에서 꼭 확인할 항목이다.

`CartService`는 `addItem`, `updateItemQuantity`, `deleteItem`, `deleteItems`의 DB 변경 성공 뒤 이 서비스를 호출한다. 그 결과 다음 전체 조회는 DB에서 최신 장바구니를 읽어 다시 저장한다.

### 12-12. 인기 검색어는 `@Cacheable`이 아닌 Redis 자료구조 코드다

파일: `domain/search/service/PopularSearchService.java`

```java
Boolean firstSearch = stringRedisTemplate.opsForValue()
        .setIfAbsent(deduplicationKey, "1", DEDUPLICATION_TTL);

if (!Boolean.TRUE.equals(firstSearch)) {
    return;
}

stringRedisTemplate.opsForZSet().incrementScore(popularSearchKey, normalizedKeyword, 1);
```

`setIfAbsent`는 Redis의 `SET NX` 계열 동작이다. 동일 회원·동일 검색어 조합의 중복 방지 키가 없을 때만 만들고 `true`를 반환한다. 이미 있으면 `false` 또는 null이므로 점수 증가 없이 끝난다.

그 후 `incrementScore`는 Redis `ZINCRBY`로 ZSet member의 점수를 1 올린다. 별도로 “검색어가 이미 존재하는가?”를 읽고 쓰는 두 단계보다 원자적이라 동시 요청에도 적합하다.

```java
reverseRangeWithScores(key, 0, limit - 1)
```

이 호출은 높은 score부터 0번~`limit - 1`번까지 가져온다. `limit=10`이면 0~9이므로 정확히 최대 10개다. 반환 score는 검색 횟수이고, `rank` 배열은 stream 처리 중 1부터 순위를 붙이기 위한 간단한 가변 값이다.

인기 검색어 기록은 `try/catch (DataAccessException)`으로 감싼다. Redis 집계가 잠시 실패해도 상품 검색 자체는 실패시키지 않겠다는 정책이다. 인기 검색어는 부가 기능이지만 상품 검색은 핵심 기능이기 때문이다.

### 12-13. 테스트를 코드 리뷰의 증거로 읽기

`ProductSearchCachingTest`의 이 검증은 캐시 구현의 가장 중요한 약속을 확인한다.

```java
verify(productRepository, times(1))
        .searchProducts(any(), any(), any(), any());
```

동일 요청을 세 번 했는데 repository가 한 번만 호출되었다면, 첫 요청은 miss, 나머지 둘은 hit였다는 뜻이다. 반대로 키워드·등급·페이지·정렬 중 하나씩 바꾸어 다섯 번 호출한 테스트에서 repository가 다섯 번 호출되는지도 확인한다. 즉, “캐시는 된다”뿐 아니라 “서로 다른 결과를 잘못 공유하지 않는다”까지 검증한다.

`ProductCacheEvictionServiceTest`는 각 캐시에 값을 직접 넣고 `evictProductCaches()`를 호출한 뒤 세 값이 모두 사라졌는지 확인한다. `OrderFacadeTest`는 주문 성공 후 이 Evict 서비스가 호출되는 순서를 검증한다. 한 테스트는 **삭제 기능**, 다른 테스트는 **실제 업무 흐름에 연결되었는지**를 확인하므로 둘 다 필요하다.

### 12-14. 이 구현에서 의도적으로 남겨 둔 개선 후보

코드 리뷰는 장점만 찾는 것이 아니라 현재 범위를 명확히 하는 일이다.

| 항목 | 현재 구현 | 다음 개선 방향 |
| --- | --- | --- |
| 검색어 키 정규화 | DB 조회 전 trim하지만 캐시 키에는 원문 사용 | 키 생성 전 정규화하여 `권총`/` 권총 ` 중복 캐시 감소 |
| 상품 변경 API | 아직 없음 | 상품명·가격·상태·재고 변경 성공 뒤 상품 캐시 Evict 연결 |
| 결제 취소/환불 | 재고 복구 흐름이 완성되지 않음 | 실제 `restoreStock()` 뒤 상품 캐시 Evict 연결 |
| 전체 상품 Evict | 안전하지만 넓은 삭제 | 변경 빈도가 높아질 때 영향 범위·TTL·세밀한 삭제를 재검토 |
| Redis 장애 | 인기 검색어는 예외를 삼키지만 Spring Cache 실패 정책은 별도 없음 | 운영 요구가 생기면 cache error handler·장애 시나리오 검토 |

이 항목들은 지금 당장 모두 구현해야 하는 할 일은 아니다. “왜 지금은 전체 삭제와 TTL을 택했는지”를 설명할 수 있고, 서비스 규모가 바뀌면 어떤 지점을 다시 봐야 하는지를 남겨 두는 것이 목적이다.

---

## 13. 5만 건 더미 데이터와 k6 성능 테스트

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

## 14. 요청 흐름을 끝까지 따라가기

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

## 15. 앞으로 캐시를 추가할 때 스스로 묻기

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

