# 상품 검색 API 성능 테스트 보고서

## 목적

상품 검색 API의 Redis Remote Cache 적용 전후 성능을 비교한다.

- v1: 매 요청마다 MySQL에서 상품 검색
- v2: Redis Cache-aside 방식으로 검색 결과를 조회하고, 캐시 미스일 때만 MySQL에서 검색

## 테스트 데이터

`scripts/seed_product_search_performance_50k.sql`로 성능 테스트 전용 상품 50,000건을 로컬 MySQL에 적재했다.

| 검색어 | 데이터 수 |
| --- | ---: |
| 권총 | 10,000건 |
| 소총 | 10,000건 |
| 방탄조끼 | 10,000건 |
| 탄약 | 10,000건 |
| 전술 장비 | 10,000건 |

테스트 요청은 접근 가능한 최하위 상품 등급인 `yellow`를 사용했다. `권총`과 `yellow` 조건을 함께 만족하는 검색 결과는 2,500건이었다.

## 테스트 환경

- 로컬 Windows 환경
- Spring Boot 애플리케이션: `localhost:8080`
- MySQL: `localhost:3306`
- Redis 7 Docker 컨테이너: `localhost:6379`
- k6 Docker 이미지: `grafana/k6:latest`
- 검색 조건: `keyword=권총`, `tier=yellow`, `page=0`, `size=20`, `sort=POPULAR`
- 부하 조건: 5 VU, 3초 ramp-up, 8초 유지, 2초 ramp-down, 요청 간 0.1초 대기

## 결과

| API | 캐시 상태 | 평균 응답 시간 | p95 | 처리량 | 실패율 |
| --- | --- | ---: | ---: | ---: | ---: |
| v1 `/api/v1/products/search` | DB 직접 조회 | 58.59ms | 60.59ms | 24.97 req/s | 0% |
| v2 `/api/v2/products/search` | Redis 초기 상태 | 5.39ms | 6.56ms | 37.61 req/s | 0% |
| v2 `/api/v2/products/search` | Redis 워밍 후 | 5.96ms | 10.32ms | 37.53 req/s | 0% |

## 해석

Redis Cache를 적용한 v2는 v1보다 평균 응답 시간이 약 90% 낮았고, 처리량은 약 1.5배 높았다. 동일한 검색 조건을 반복 조회하는 상황에서 MySQL의 `LIKE` 검색과 count 쿼리를 피하고 Redis에서 결과를 반환하기 때문에 응답 시간이 감소했다.

## 측정 시 유의점

- 이 결과는 로컬 단일 머신에서 수행한 짧은 예비 측정이다. 네트워크 지연, AWS Redis 연결, 다중 애플리케이션 서버 환경은 포함하지 않는다.
- `v2 Redis 초기 상태`는 실행 전 Redis를 비운 상태다. 단, 첫 요청 이후에는 동일 키의 캐시가 채워지므로 전체 요청이 계속 cold cache인 측정은 아니다.
- 최종 비교 시에는 VU와 유지 시간을 늘리고, 각 조건을 여러 번 실행해 평균값을 기록한다.

## 재실행 방법

1. MySQL에 `scripts/seed_product_search_performance_50k.sql`을 실행한다.
2. Redis와 Spring Boot 애플리케이션을 실행한다.
3. 로그인 API로 access token을 발급받는다.
4. `scripts/k6/README.md`의 v1, v2 cold, v2 warm 명령을 각각 실행한다.
