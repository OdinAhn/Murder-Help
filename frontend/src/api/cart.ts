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
