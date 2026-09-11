import { getAccessToken } from "./auth";

type ApiResponse<T> = {
  code: string;
  message?: string;
  data?: T;
};

export type CartItemData = {
  id: number;
  productId: number;
  quantity: number;
};

export type CartItemDetailData = CartItemData & {
  productCode: string;
  name: string;
  category: string;
  subCategory: string;
  price: number;
  tier: "yellow" | "purple" | "red" | "green";
  imageUrl: string;
  status: "ON_SALE" | "SOLD_OUT" | "DISCONTINUED";
  stockQuantity: number;
};

function getAuthorizationHeaders(contentType = false): HeadersInit {
  const accessToken = getAccessToken();
  if (!accessToken) throw new Error("로그인이 필요합니다.");

  return {
    Accept: "application/json",
    Authorization: `Bearer ${accessToken}`,
    ...(contentType ? { "Content-Type": "application/json" } : {}),
  };
}

async function parseData<T>(response: Response, fallbackMessage: string): Promise<T> {
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || body.data === undefined) {
    throw new Error(body?.message ?? `${fallbackMessage} (${response.status})`);
  }

  return body.data;
}

/** POST /api/cart/items — 로그인한 회원의 장바구니에 상품 추가 */
export async function addCartItem(productId: number, quantity: number): Promise<CartItemData> {
  const accessToken = getAccessToken();
  if (!accessToken) throw new Error("로그인이 필요합니다.");

  const response = await fetch("/api/cart/items", {
    method: "POST",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ productId, quantity }),
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<CartItemData> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `장바구니에 상품을 담지 못했습니다 (${response.status})`);
  }

  return body.data;
}

/** GET /api/cart/items — 로그인한 회원의 장바구니 목록 조회 */
export async function fetchCartItems(): Promise<CartItemDetailData[]> {
  const response = await fetch("/api/cart/items", {
    headers: getAuthorizationHeaders(),
    credentials: "include",
  });

  return parseData(response, "장바구니를 불러오지 못했습니다");
}

/** PATCH /api/cart/items/{id} — 장바구니 상품의 수량 변경 */
export async function updateCartItemQuantity(
  cartItemId: number,
  quantity: number,
): Promise<CartItemData> {
  const response = await fetch(`/api/cart/items/${cartItemId}`, {
    method: "PATCH",
    headers: getAuthorizationHeaders(true),
    credentials: "include",
    body: JSON.stringify({ quantity }),
  });

  return parseData(response, "장바구니 수량을 변경하지 못했습니다");
}

/** DELETE /api/cart/items/{id} — 장바구니 상품 삭제 */
export async function deleteCartItem(cartItemId: number): Promise<void> {
  const response = await fetch(`/api/cart/items/${cartItemId}`, {
    method: "DELETE",
    headers: getAuthorizationHeaders(),
    credentials: "include",
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<void> | null;
  if (!response.ok || !body || body.code !== "SUCCESS") {
    throw new Error(body?.message ?? `장바구니 상품을 삭제하지 못했습니다 (${response.status})`);
  }
}
