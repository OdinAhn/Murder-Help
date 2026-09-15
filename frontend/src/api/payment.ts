import { authFetch } from "./client";

type ApiResponse<T> = {
  code: string;
  message?: string;
  data?: T;
};

/* ─── 결제 승인 ──────────────────────────────────────────── */
export type ConfirmPaymentInput = {
  orderId: number;
  portonePaymentId: string;
};

export type PaymentConfirmResult = {
  paymentId: number;
  orderId: number;
  amount: number;
  paymentStatus: string;
  orderStatus: string;
  message: string;
};

/** POST /api/payments/confirm — PortOne 결제창 완료 후 서버 승인 요청.
 *  결제창 응답과 무관하게 항상 호출해야 한다. 서버가 PortOne API를 직접 재조회해서
 *  금액/상태를 검증한 뒤에만 진짜 승인 처리한다. */
export async function confirmPayment(input: ConfirmPaymentInput): Promise<PaymentConfirmResult> {
  const response = await authFetch("/api/payments/confirm", {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<PaymentConfirmResult> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `결제 승인에 실패했습니다 (${response.status})`);
  }

  return body.data;
}

/* ─── 결제 취소 ──────────────────────────────────────────── */
export type PaymentCancelResult = {
  paymentId: number;
  orderId: number;
  portonePaymentId: string;
  paymentStatus: string;
  orderStatus: string;
  message: string;
};

/** POST /api/payments/{id}/cancel — 결제 대기 중(PG 미승인) 사용자 직접 취소.
 *  결제 완료 후 환불은 이 API가 아니라 refund.ts 의 requestRefund 를 쓴다. */
export async function cancelPayment(paymentId: number, reason?: string): Promise<PaymentCancelResult> {
  const response = await authFetch(`/api/payments/${paymentId}/cancel`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: reason ? JSON.stringify({ reason }) : undefined,
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<PaymentCancelResult> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data) {
    throw new Error(body?.message ?? `결제 취소에 실패했습니다 (${response.status})`);
  }

  return body.data;
}