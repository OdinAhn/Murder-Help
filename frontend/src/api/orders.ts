import type { Tier } from "../catalog";

export type Receiver = {
  name: string;
  phone: string;
  postcode: string;
  address: string;
  detail: string;
  memo: string;
};

export type OrderPayload = {
  items: { id: string; name: string; price: number; qty: number }[];
  itemsTotal: number;
  shipping: number;
  total: number;
  receiver: Receiver;
  memberTier: Tier | null;
};

/* ══════════════════════════════════════════════════════════
   백엔드 연동 지점.
   실제 결제를 붙일 때 고쳐야 하는 곳은 이 함수 하나뿐이다.
   서버에 주문을 만들고 PG 결제창을 띄운 뒤 주문번호를 돌려주면 된다.

     const res = await fetch("/api/orders", {
       method: "POST",
       headers: { "Content-Type": "application/json" },
       body: JSON.stringify(payload),
     });
     if (!res.ok) throw new Error("주문 생성 실패");
     return await res.json();

   결제 키는 서버에만 두어야 하며 이 번들에 넣으면 안 된다.
   ══════════════════════════════════════════════════════════ */
export async function placeOrder(payload: OrderPayload): Promise<{ orderNo: string }> {
  await new Promise((resolve) => setTimeout(resolve, 900));
  void payload;
  return { orderNo: "MH" + String(Date.now()).slice(-8) };
}
