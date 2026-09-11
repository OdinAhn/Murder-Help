import type { Product, Tier } from "./catalog";

/* ══════════════════════════════════════════════════════════
   백엔드 연동 지점 (상품 목록 · 검색).
   실제 로그인(JWT)이 붙기 전까지는 로컬 백엔드(local 프로필)의
   X-Product-Tier 헤더로 "내 등급"을 흉내 낸다. 진짜 인증이 들어오면
   이 헤더 대신 Authorization: Bearer <token> 을 보내는 것으로 바꾸면 된다.
   ══════════════════════════════════════════════════════════ */
const MEMBER_TIER_HEADER = "X-Product-Tier";

export type ApiProductSort = "POPULAR" | "PRICE_ASC" | "PRICE_DESC" | "NEWEST";

type ApiProductResponse = {
  id: number;
  productCode: string;
  name: string;
  category: string;
  subCategory: string;
  price: number;
  tier: string;
  imageUrl: string;
};

type ApiPage<T> = {
  items: T[];
  page: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

type ApiEnvelope<T> = {
  code: string;
  message?: string;
  data: T;
};

export type ApiProduct = Product & {
  productId: number;
};

export type ProductPage = {
  items: ApiProduct[];
  page: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

function toProduct(raw: ApiProductResponse): ApiProduct {
  return {
    productId: raw.id,
    id: raw.productCode,
    name: raw.name,
    category: raw.category,
    sub: raw.subCategory,
    price: raw.price,
    tier: raw.tier as Tier,
    img: raw.imageUrl,
    desc: "",
    specs: [],
  };
}

async function getPage(url: string, memberTier: Tier): Promise<ProductPage> {
  const res = await fetch(url, {
    headers: { [MEMBER_TIER_HEADER]: memberTier },
  });
  if (!res.ok) throw new Error(`상품 조회에 실패했습니다 (${res.status})`);

  const body = (await res.json()) as ApiEnvelope<ApiPage<ApiProductResponse>>;
  return {
    items: body.data.items.map(toProduct),
    page: body.data.page,
    totalElements: body.data.totalElements,
    totalPages: body.data.totalPages,
    hasNext: body.data.hasNext,
  };
}

/** GET /api/products — 카테고리 목록 조회 */
export function fetchProductList(params: {
  category: string;
  subCategory?: string;
  tier: Tier;
  sort: ApiProductSort;
  page: number;
  size: number;
  memberTier: Tier;
}): Promise<ProductPage> {
  const qs = new URLSearchParams({
    category: params.category,
    tier: params.tier,
    sort: params.sort,
    page: String(params.page),
    size: String(params.size),
  });
  if (params.subCategory) qs.set("subCategory", params.subCategory);

  return getPage(`/api/products?${qs.toString()}`, params.memberTier);
}

/** GET /api/v1/products/search — 상품명 검색 (v1, 캐시 미적용) */
export function searchProducts(params: {
  keyword: string;
  tier: Tier;
  sort: ApiProductSort;
  page: number;
  size: number;
  memberTier: Tier;
}): Promise<ProductPage> {
  const qs = new URLSearchParams({
    keyword: params.keyword,
    tier: params.tier,
    sort: params.sort,
    page: String(params.page),
    size: String(params.size),
  });

  return getPage(`/api/v1/products/search?${qs.toString()}`, params.memberTier);
}
