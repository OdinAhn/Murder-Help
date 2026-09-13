# 상품 검색 k6 성능 테스트

## 사전 준비

1. `scripts/seed_product_search_performance_50k.sql`을 로컬 MySQL에 실행한다.
2. Redis와 애플리케이션을 실행한다.
3. `POST /api/auth/login`으로 테스트 계정의 access token을 발급받는다.
4. Docker Desktop을 실행한다.

`yellow` 상품은 모든 회원 등급에서 조회 가능한 최하위 등급이므로, 아래 기본값을 사용한다.

## 실행

PowerShell에서 프로젝트 루트 기준으로 실행한다. `<ACCESS_TOKEN>`은 실제 토큰으로 교체한다.

```powershell
docker run --rm -i -v "${PWD}:/scripts" grafana/k6 run `
  -e ACCESS_TOKEN=<ACCESS_TOKEN> `
  -e API_VERSION=v1 `
  /scripts/scripts/k6/product-search.js
```

캐시 미적중 상태의 v2:

```powershell
docker run --rm -i -v "${PWD}:/scripts" grafana/k6 run `
  -e ACCESS_TOKEN=<ACCESS_TOKEN> `
  -e API_VERSION=v2 `
  -e WARM_CACHE=false `
  /scripts/scripts/k6/product-search.js
```

캐시 워밍 후 v2:

```powershell
docker run --rm -i -v "${PWD}:/scripts" grafana/k6 run `
  -e ACCESS_TOKEN=<ACCESS_TOKEN> `
  -e API_VERSION=v2 `
  -e WARM_CACHE=true `
  /scripts/scripts/k6/product-search.js
```

기본 부하는 30초 동안 20 VU까지 증가, 1분 유지, 15초 동안 감소한다. 필요하면 `MAX_VUS`, `RAMP_UP_DURATION`, `STEADY_DURATION` 환경변수로 조정한다.

## 비교할 지표

각 실행 결과의 `http_req_duration` 평균·p95, `http_reqs` 처리량, `http_req_failed` 실패율을 기록한다. v1, v2 cold cache, v2 warm cache의 값만 비교한다.
