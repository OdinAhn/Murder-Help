import type { Product, Tier } from "../catalog";
import { authFetch } from "./client";

export const PAGE_SIZE = 12;

export type ApiProductSort = "POPULAR" | "PRICE_ASC" | "PRICE_DESC" | "NEWEST";

type ApiEnvelope<T> = {
  code: string;
  message?: string;
  data?: T;
};

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

export type PopularSearch = {
  rank: number;
  keyword: string;
  score: number;
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

async function getPage(url: string): Promise<ProductPage> {
  const res = await authFetch(url, {
    headers: { Accept: "application/json" },
  });
  if (!res.ok) throw new Error(`상품 조회에 실패했습니다 (${res.status})`);

  const body = (await res.json()) as ApiEnvelope<ApiPage<ApiProductResponse>>;
  if (!body.data) throw new Error(body.message ?? "상품 조회에 실패했습니다.");

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
}): Promise<ProductPage> {
  const qs = new URLSearchParams({
    category: params.category,
    tier: params.tier,
    sort: params.sort,
    page: String(params.page),
    size: String(params.size),
  });
  if (params.subCategory) qs.set("subCategory", params.subCategory);

  return getPage(`/api/products?${qs.toString()}`);
}

/** GET /api/v1/products/search — 상품명 검색 (v1, 캐시 미적용) */
export function searchProducts(params: {
  keyword: string;
  tier: Tier;
  sort: ApiProductSort;
  page: number;
  size: number;
}): Promise<ProductPage> {
  const qs = new URLSearchParams({
    keyword: params.keyword,
    tier: params.tier,
    sort: params.sort,
    page: String(params.page),
    size: String(params.size),
  });

  return getPage(`/api/v1/products/search?${qs.toString()}`);
}

/** GET /api/searches/popular — 오늘의 인기 검색어 조회 */
export async function fetchPopularSearches(limit = 10): Promise<PopularSearch[]> {
  const response = await authFetch(`/api/searches/popular?limit=${limit}`, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) throw new Error(`인기 검색어 조회에 실패했습니다 (${response.status})`);

  const body = (await response.json()) as ApiEnvelope<PopularSearch[]>;
  if (!body.data) throw new Error(body.message ?? "인기 검색어 조회에 실패했습니다.");

  return body.data;
}

/* ─── 상품 상세 ───────────────────────────────────────────── */
type ProductSpecApiResponse = {
  name: string;
  value: string;
  sortOrder: number;
};

type ProductDetailApiResponse = {
  id: number;
  productCode: string;
  name: string;
  description: string;
  category: string;
  subCategory: string;
  price: number;
  stockQuantity: number;
  tier: Tier;
  imageUrl: string;
  status: "ON_SALE" | "SOLD_OUT";
  specs: ProductSpecApiResponse[];
};

export type ProductDetailData = ApiProduct & {
  stockQuantity: number;
  status: "ON_SALE" | "SOLD_OUT";
};

/** GET /api/products/{id} — 상품 상세 조회 */
export async function fetchProductDetail(
  productId: number,
  signal: AbortSignal,
): Promise<ProductDetailData> {
  const response = await authFetch(`/api/products/${productId}`, {
    headers: { Accept: "application/json" },
    signal,
  });
  const body = await response.json().catch(() => null) as ApiEnvelope<ProductDetailApiResponse> | null;

  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `상품 상세정보를 불러오지 못했습니다 (${response.status})`);
  }

  const product = body.data;
  return {
    productId: product.id,
    id: product.productCode,
    name: product.name,
    category: product.category,
    sub: product.subCategory,
    price: product.price,
    tier: product.tier,
    img: product.imageUrl,
    desc: product.description,
    specs: product.specs.map((spec) => [spec.name, spec.value]),
    stockQuantity: product.stockQuantity,
    status: product.status,
  };
}
