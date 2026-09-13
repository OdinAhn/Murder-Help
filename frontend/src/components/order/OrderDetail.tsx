import type { ReactNode } from "react";
import type { OrderData, OrderStatus } from "../../api/orders";
import { formatDate, formatDateTime, ORDER_STATUS_LABEL, orderStatusStyle } from "../../lib/order";
import { C, krw } from "../../lib/theme";
import { OrderItemRow } from "./OrderItemRow";

/* 결제완료 → 배송준비 → 배송중 → 배송완료 */
const STEPS: OrderStatus[] = ["PAID", "PREPARING_DELIVERY", "SHIPPING", "DELIVERED"];

/* ─── 주문 상세 ──────────────────────────────────────────── */
/* 상세 조회 API(GET /api/orders/{orderId})에 넘길 orderId 가 목록 응답에 없어서,
   목록에서 받은 주문을 그대로 넘겨받아 보여 준다. */
export function OrderDetail({ order, onBack }: { order: OrderData; onBack: () => void }) {
  const itemsTotal = order.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
  const quantityTotal = order.items.reduce((sum, item) => sum + item.quantity, 0);
  const canceled = order.status === "CANCELED";

  return (
    <>
      <button type="button" onClick={onBack} className="text-xs mb-5" style={{ color: C.textMuted }}>
        ← 주문 내역으로
      </button>

      <div style={{ border: `1px solid ${C.panelBorder}`, background: "rgba(0,0,0,0.18)" }}>
        {/* 주문 요약 · 진행 단계 */}
        <section className="px-6 py-5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
          <div className="flex flex-wrap items-center gap-3">
            <h1
              className="font-bold uppercase leading-none"
              style={{ fontFamily: "Cinzel, serif", fontSize: "clamp(18px,2.4vw,26px)", color: C.text }}
            >
              Order Detail
            </h1>
            <span className="text-xs" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
              {order.orderNumber}
            </span>
            {order.orderedAt && (
              <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                {formatDateTime(order.orderedAt)}
              </span>
            )}
            <span
              className="ml-auto text-[10px] px-2 py-0.5 tracking-widest"
              style={{ ...orderStatusStyle(order.status), fontFamily: "Share Tech Mono" }}
            >
              {ORDER_STATUS_LABEL[order.status]}
            </span>
          </div>

          {/* 취소 주문은 단계 대신 취소일 한 줄로 보여 준다 */}
          {canceled ? (
            <p className="mt-5 text-sm" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
              {order.canceledAt ? `${formatDate(order.canceledAt)} 취소됨` : "취소됨"}
            </p>
          ) : (
            <OrderProgress status={order.status} />
          )}
        </section>

        {/* 주문 품목 */}
        <section className="px-6 py-5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
          <SectionLabel>// 주문 품목 {order.items.length}건</SectionLabel>
          {order.items.map((item, i) => (
            <div key={item.orderItemId ?? i} style={{ borderTop: i > 0 ? `1px solid ${C.panelBorder}` : undefined }}>
              <OrderItemRow item={item} />
            </div>
          ))}
        </section>

        {/* 배송 정보 · 결제 정보 */}
        <div className="grid grid-cols-1 md:grid-cols-2" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
          <section className="px-6 py-5">
            <SectionLabel>// 배송 정보</SectionLabel>
            <InfoRow label="받는 사람">{order.receiverName}</InfoRow>
            <InfoRow label="연락처">{order.receiverPhone}</InfoRow>
            <InfoRow label="주소">{order.deliveryAddress}</InfoRow>
            <InfoRow label="배송 요청사항">{order.deliveryRequest || "-"}</InfoRow>
            <InfoRow label="송장번호">{order.trackingNumber || "-"}</InfoRow>
          </section>

          <section className="px-6 py-5 border-t md:border-t-0 md:border-l" style={{ borderColor: C.panelBorder }}>
            <SectionLabel>// 결제 정보</SectionLabel>
            <AmountRow label="상품 합계">{krw(itemsTotal)}</AmountRow>
            <AmountRow label="수량 합계">{quantityTotal}개</AmountRow>
            <AmountRow label="결제 수단">{order.paymentMethod || "-"}</AmountRow>

            <div className="flex items-center justify-between pt-4 mt-3" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
              <span className="text-sm font-bold" style={{ color: C.text }}>
                총 결제금액
              </span>
              <span
                className="text-xl font-bold"
                style={{ color: canceled ? C.textMuted : C.price, fontFamily: "Share Tech Mono" }}
              >
                {krw(order.totalAmount)}
              </span>
            </div>
          </section>
        </div>

        <div className="px-6 py-4">
          <button
            type="button"
            onClick={onBack}
            className="px-6 py-2.5 text-xs"
            style={{ color: C.textDim, border: `1px solid ${C.panelBorder}` }}
          >
            목록으로
          </button>
        </div>
      </div>
    </>
  );
}

/* 지난 단계는 채운 점, 현재 단계는 굵은 테두리 점, 남은 단계는 흐린 테두리 점 */
function OrderProgress({ status }: { status: OrderStatus }) {
  /* 결제 대기는 아직 어느 단계에도 오지 않았으므로 -1 이 된다 */
  const current = STEPS.indexOf(status);

  return (
    <div className="relative grid grid-cols-4 mt-6">
      {/* 첫 점부터 마지막 점까지 잇는 선 */}
      <div className="absolute top-[7px] left-[12.5%] right-[12.5%] h-px" style={{ background: C.panelBorder }} />

      {STEPS.map((step, i) => {
        const past = i < current;
        const now = i === current;

        return (
          <div key={step} className="relative flex flex-col items-center gap-2">
            <span
              className="w-3.5 h-3.5 rounded-full"
              style={{
                background: past ? C.redBright : "#0e0000",
                border: now ? `3px solid ${C.redBright}` : `1px solid ${past ? C.redBright : C.textMuted}`,
              }}
            />
            <span className="text-[11px]" style={{ color: past || now ? C.text : C.textMuted, fontWeight: now ? 700 : 400 }}>
              {ORDER_STATUS_LABEL[step]}
            </span>
          </div>
        );
      })}
    </div>
  );
}

function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <p className="mb-3 text-[11px]" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
      {children}
    </p>
  );
}

function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex gap-4 py-1.5 text-xs">
      <span className="w-24 shrink-0" style={{ color: C.textMuted }}>
        {label}
      </span>
      <span className="min-w-0 break-words" style={{ color: C.text }}>
        {children}
      </span>
    </div>
  );
}

function AmountRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between py-1.5 text-xs">
      <span style={{ color: C.textMuted }}>{label}</span>
      <span style={{ color: C.text, fontFamily: "Share Tech Mono" }}>{children}</span>
    </div>
  );
}
