import { authFetch } from "./client";

type ApiResponse<T> = {
  code: string;
  message?: string;
  data?: T;
};

/* 백엔드 RefundStatus — COMPLETED 는 PG 취소까지 성공한 상태,
   PG_FAILED 는 DB 상 환불 처리는 됐지만 PG 통신이 실패해 수동 보정이 필요한 상태다. */
export type RefundStatus = "COMPLETED" | "PG_FAILED";

/* ─── 환불 가능 상품 조회 ────────────────────────────────── */
export type RefundableItem = {
  orderItemId: number;
  productName: string;
  orderPrice: number;
  originalQuantity: number;
  remainQuantity: number;
};

/** GET /api/refunds/refundable/{paymentId} — 환불 신청 팝업을 열 때,
 *  상품별로 몇 개까지 더 환불 가능한지(remainQuantity) 조회한다. */
export async function fetchRefundableItems(paymentId: number): Promise<RefundableItem[]> {
  const response = await authFetch(`/api/refunds/refundable/${paymentId}`, {
    headers: { Accept: "application/json" },
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<RefundableItem[]> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `환불 가능 상품을 불러오지 못했습니다 (${response.status})`);
  }

  return body.data;
}

/* ─── 환불 요청 ──────────────────────────────────────────── */
export type RefundItemInput = {
  orderItemId: number;
  requestQuantity: number;
};

export type RequestRefundInput = {
  paymentId: number;
  cancelReason: string;
  items: RefundItemInput[];
};

export type RefundResult = {
  refundId: number;
  status: RefundStatus;
  pgRefundAmount: number;
  refundedAt: string;
};

/** POST /api/refunds — 선택한 상품·수량 기준으로 환불 신청.
 *  전체 상품을 남은 수량 그대로 다 선택하면 서버가 자동으로 전액 환불로 처리한다. */
export async function requestRefund(input: RequestRefundInput): Promise<RefundResult> {
  const response = await authFetch("/api/refunds", {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<RefundResult> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `환불 신청에 실패했습니다 (${response.status})`);
  }

  return body.data;
}

/* ─── 환불 내역(영수증) 조회 ─────────────────────────────── */
export type RefundHistoryItem = {
  refundItemId: number;
  productName: string;
  refundQuantity: number;
  itemRefundAmount: number;
};

export type RefundHistory = {
  refundId: number;
  refundDate: string;
  status: RefundStatus;
  pgRefundAmount: number;
  items: RefundHistoryItem[];
};

/** GET /api/refunds/history/{paymentId} — 해당 결제 건에 대한 환불 이력 전체(여러 건일 수 있음) */
export async function fetchRefundHistory(paymentId: number): Promise<RefundHistory[]> {
  const response = await authFetch(`/api/refunds/history/${paymentId}`, {
    headers: { Accept: "application/json" },
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<RefundHistory[]> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `환불 내역을 불러오지 못했습니다 (${response.status})`);
  }

  return body.data;
}