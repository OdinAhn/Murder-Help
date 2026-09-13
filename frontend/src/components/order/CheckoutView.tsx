import { useState, type FormEvent } from "react";
import { createOrder, type Receiver } from "../../api/orders";
import type { Product } from "../../catalog";
import { C, krw } from "../../lib/theme";
import { FREE_SHIPPING_OVER, SHIPPING_FEE } from "../../lib/shipping";
import { Field } from "../common/Field";
import { PageTitle } from "../common/PageTitle";
import { Spinner } from "../common/Spinner";
import { SummaryRow } from "../common/SummaryRow";

/* ─── checkout ───────────────────────────────────────────── */
export function CheckoutView({
  lines, cartItemIds, onBack, onDone,
}: {
  lines: { p: Product; qty: number }[];
  /* 주문 생성 API에 보낼 장바구니 항목 ID */
  cartItemIds: number[];
  onBack: () => void;
  onDone: (orderNo: string, total: number) => void;
}) {
  const [r, setR] = useState<Receiver>({ name: "", phone: "", postcode: "", address: "", detail: "", memo: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const itemsTotal = lines.reduce((sum, l) => sum + l.p.price * l.qty, 0);
  const shipping = itemsTotal >= FREE_SHIPPING_OVER ? 0 : SHIPPING_FEE;
  const total = itemsTotal + shipping;

  const set = (k: keyof Receiver) => (v: string) => setR((prev) => ({ ...prev, [k]: v }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;

    if (!r.name.trim() || !r.phone.trim() || !r.address.trim()) {
      setError("받는 분, 연락처, 주소는 필수 항목입니다.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const order = await createOrder({
        cartItemIds,
        receiverName: r.name.trim(),
        receiverPhone: r.phone.trim(),
        /* 백엔드는 주소를 한 필드로 받으므로 우편번호 · 주소 · 상세 주소를 이어 붙인다 */
        deliveryAddress: [r.postcode, r.address, r.detail].map((v) => v.trim()).filter(Boolean).join(" "),
        deliveryRequest: r.memo.trim() || undefined,
      });
      /* 완료 화면에는 서버가 확정한 주문번호와 금액을 쓴다 */
      onDone(order.orderNumber, order.totalAmount);
    } catch (error) {
      setBusy(false);
      setError(error instanceof Error ? error.message : "주문을 접수하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }
  }

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
      <button
        onClick={onBack}
        className="text-xs uppercase tracking-widest mb-4"
        style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
      >
        ← 장바구니로
      </button>

      <PageTitle note="// 배송 정보를 입력해 주세요">Checkout</PageTitle>

      <form onSubmit={submit} className="flex flex-col lg:flex-row gap-6 items-start">
        {/* 배송 정보 */}
        <div className="flex-1 w-full p-6" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
          <div className="text-[10px] uppercase tracking-widest mb-5" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
            // 배송지
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <Field label="받는 분 *" value={r.name} onChange={set("name")} placeholder="홍길동" />
            <Field label="연락처 *" value={r.phone} onChange={set("phone")} placeholder="010-0000-0000" />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <Field label="우편번호" value={r.postcode} onChange={set("postcode")} placeholder="00000" />
            <div className="md:col-span-2">
              <Field label="주소 *" value={r.address} onChange={set("address")} placeholder="시/도 구/군 도로명" />
            </div>
          </div>

          <div className="mb-4">
            <Field label="상세 주소" value={r.detail} onChange={set("detail")} placeholder="동 · 호수" />
          </div>

          <Field label="배송 요청사항" value={r.memo} onChange={set("memo")} placeholder="부재 시 경비실에 맡겨주세요" />

          <div className="mt-6 pt-5 flex items-start gap-3" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
            <span
              className="text-[10px] uppercase tracking-widest shrink-0"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono", paddingTop: 2 }}
            >
              연령 확인
            </span>
            <span className="text-xs leading-relaxed" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
              BB탄 전용 에어소프트 제품으로, 만 18세 이상만 구매하실 수 있습니다. 주문 시 연령 확인에 동의한 것으로 봅니다.
            </span>
          </div>
        </div>

        {/* 주문 요약 */}
        <div className="w-full lg:w-80 shrink-0 p-5" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
          <div className="text-[10px] uppercase tracking-widest mb-4" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
            // 주문 요약
          </div>

          <div className="mb-4" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
            {lines.map(({ p, qty }) => (
              <div key={p.id} className="flex justify-between items-baseline gap-3 py-1.5">
                <span className="text-xs truncate" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
                  {p.name} <span style={{ color: C.textMuted }}>× {qty}</span>
                </span>
                <span className="text-xs shrink-0" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                  {krw(p.price * qty)}
                </span>
              </div>
            ))}
          </div>

          <SummaryRow label="상품 합계" value={krw(itemsTotal)} />
          <SummaryRow label="배송비" value={shipping === 0 ? "무료" : krw(shipping)} />

          <div className="flex justify-between items-baseline py-3 mt-2" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
            <span className="text-xs uppercase tracking-widest" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
              총 결제금액
            </span>
            <span className="text-xl font-bold" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
              {krw(total)}
            </span>
          </div>

          {error && (
            <p className="text-xs mb-3" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="w-full py-3.5 mt-2 text-sm font-bold uppercase tracking-widest flex items-center justify-center gap-2"
            style={{
              background: busy ? C.redDim : C.red,
              color: "#fff",
              border: `1px solid ${busy ? C.redDim : C.redBright}`,
              fontFamily: "Share Tech Mono",
              cursor: busy ? "wait" : "pointer",
            }}
          >
            {busy ? <><Spinner /> 결제 처리 중…</> : `${krw(total)} 결제하기 →`}
          </button>
        </div>
      </form>
    </div>
  );
}
