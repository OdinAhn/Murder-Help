import { authFetch } from "./client";

type ApiResponse<T> = {
  code: string;
  message?: string;
  data?: T;
};

export type Receiver = {
  name: string;
  phone: string;
  postcode: string;
  address: string;
  detail: string;
  memo: string;
};

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PAID"
  | "PREPARING_DELIVERY"
  | "SHIPPING"
  | "DELIVERED"
  | "CANCELED";

/* ─── 주문 생성 ─────────────────────────────────────────── */
export type CreateOrderInput = {
  cartItemIds: number[];
  receiverName: string;
  receiverPhone: string;
  deliveryAddress: string;
  deliveryRequest?: string;
};

export type CreatedOrder = {
  orderId: number;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
};

/** POST /api/orders — 장바구니 항목으로 주문 생성
 *  서버가 재고를 차감하고 주문한 장바구니 항목을 지운 뒤, 결제 대기(PENDING_PAYMENT) 주문을 만든다.
 *  결제(PortOne)는 아직 백엔드에 없다. 붙으면 주문 생성 → 결제창 → /api/payments/confirm 순서가 된다. */
export async function createOrder(input: CreateOrderInput): Promise<CreatedOrder> {
  const response = await authFetch("/api/orders", {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<CreatedOrder> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `주문을 접수하지 못했습니다 (${response.status})`);
  }

  return body.data;
}

/* ─── 주문 내역 조회 ─────────────────────────────────────── */
/* 백엔드 OrderListPeriod — 값을 안 보내면 서버가 MONTH_3 으로 조회한다 */
export type OrderPeriod = "MONTH_1" | "MONTH_3" | "MONTH_6" | "MONTH_12" | "ALL";

export type OrderData = {
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  receiverName: string;
  receiverPhone: string;
  deliveryAddress: string;
  deliveryRequest: string | null;
  items: { productName: string; unitPrice: number; quantity: number }[];
};

/* 컨트롤러가 Spring Data Page 를 그대로 반환한다(PageSerializationMode.DIRECT).
   쓰는 필드만 적어 둔다.
   요청 page 는 1부터 시작하지만(WebConfig 의 oneIndexedParameters), 응답 number 는 0부터 시작한다. */
type SpringPage<T> = {
  content: T[];
  number: number;
  totalElements: number;
  last: boolean;
};

export type OrderPage = {
  items: OrderData[];
  page: number;
  totalElements: number;
  hasNext: boolean;
};

/** GET /api/orders — 로그인한 회원의 주문 내역 조회 (최신 주문순) */
export async function fetchMyOrders(
  params: { period: OrderPeriod; status?: OrderStatus; page: number; size: number },
  signal?: AbortSignal,
): Promise<OrderPage> {
  const qs = new URLSearchParams({
    period: params.period,
    page: String(params.page),
    size: String(params.size),
  });
  if (params.status) qs.set("status", params.status);

  const response = await authFetch(`/api/orders?${qs.toString()}`, {
    headers: { Accept: "application/json" },
    signal,
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<SpringPage<OrderData>> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `주문 내역을 불러오지 못했습니다 (${response.status})`);
  }

  return {
    items: body.data.content,
    page: body.data.number + 1, // 요청과 같은 1부터 시작하는 번호로 맞춘다
    totalElements: body.data.totalElements,
    hasNext: !body.data.last,
  };
}
