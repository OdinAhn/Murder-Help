import http from 'k6/http';
import {check, fail, sleep} from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const accessToken = __ENV.ACCESS_TOKEN;
const apiVersion = __ENV.API_VERSION || 'v1';
const keyword = __ENV.KEYWORD || '권총';
const tier = __ENV.TIER || 'yellow';
const pageSize = __ENV.PAGE_SIZE || '20';
const warmCache = __ENV.WARM_CACHE !== 'false';

if (!accessToken) {
    throw new Error('ACCESS_TOKEN 환경변수가 필요합니다.');
}

if (!['v1', 'v2'].includes(apiVersion)) {
    throw new Error('API_VERSION은 v1 또는 v2만 가능합니다.');
}

export const options = {
    scenarios: {
        product_search: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                {duration: __ENV.RAMP_UP_DURATION || '30s', target: Number(__ENV.MAX_VUS || 20)},
                {duration: __ENV.STEADY_DURATION || '1m', target: Number(__ENV.MAX_VUS || 20)},
                {duration: __ENV.RAMP_DOWN_DURATION || '15s', target: 0},
            ],
            gracefulRampDown: __ENV.GRACEFUL_RAMP_DOWN || '10s',
            gracefulStop: __ENV.GRACEFUL_STOP || '10s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
};

function searchUrl() {
    return `${baseUrl}/api/${apiVersion}/products/search`
        + `?keyword=${encodeURIComponent(keyword)}`
        + `&tier=${encodeURIComponent(tier)}`
        + `&page=0&size=${pageSize}&sort=POPULAR`;
}

function requestSearch() {
    return http.get(searchUrl(), {
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
        tags: {
            api_version: apiVersion,
            cache_state: apiVersion === 'v2' && warmCache ? 'warm' : 'cold',
        },
    });
}

export function setup() {
    if (apiVersion !== 'v2' || !warmCache) {
        return;
    }

    const response = requestSearch();
    if (response.status !== 200) {
        fail(`v2 캐시 워밍 요청 실패: HTTP ${response.status}`);
    }
}

export default function () {
    const response = requestSearch();

    check(response, {
        '검색 요청이 성공한다': (res) => res.status === 200,
        '검색 결과가 반환된다': (res) => {
            const body = res.json();
            return Array.isArray(body.data?.items) && body.data.items.length > 0;
        },
    });

    sleep(Number(__ENV.THINK_TIME_SECONDS || 0.1));
}
