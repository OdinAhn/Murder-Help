import type { OrderStatus } from "../api/orders";
import { C } from "./theme";

/* ─── 주문 상태 ─────────────────────────────────────────── */
export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "결제대기",
  PAID: "결제완료",
  PREPARING_DELIVERY: "배송준비",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  CANCELED: "주문취소",
};

/* 진행 중인 주문은 채운 뱃지로, 끝났거나 멈춘 주문은 테두리만 그린다 */
export function orderStatusStyle(status: OrderStatus) {
  if (status === "CANCELED") return { color: C.redBright, border: `1px solid ${C.redDim}`, background: "transparent" };
  if (status === "DELIVERED") return { color: C.textDim, border: `1px solid ${C.panelBorder}`, background: "transparent" };
  if (status === "PENDING_PAYMENT") return { color: C.yellowBright, border: `1px solid ${C.yellow}`, background: "transparent" };
  return { color: "#fff", border: `1px solid ${C.red}`, background: C.red };
}

/* ─── 날짜 ───────────────────────────────────────────────── */
/* "2026-09-08T09:35:12" → "2026.09.08" */
export function formatDate(value?: string) {
  return value ? value.slice(0, 10).replace(/-/g, ".") : "";
}

/* "2026-09-08T09:35:12" → "2026.09.08 09:35" */
export function formatDateTime(value?: string) {
  return value ? `${formatDate(value)} ${value.slice(11, 16)}` : "";
}
