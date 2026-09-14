import { C } from "../../lib/theme";

export function OrderSummaryHeader({ itemCount }: { itemCount: number }) {
  return (
    <div
      className="flex items-center justify-between gap-3 pb-4 mb-4"
      style={{ borderBottom: `1px solid ${C.panelBorder}` }}
    >
      <div>
        <h2 className="text-base font-bold" style={{ color: C.text }}>
          주문 요약
        </h2>
        <p className="text-[10px] mt-1 uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
          Order Summary
        </p>
      </div>
      <div
        className="shrink-0 px-3 py-1.5 text-xs"
        style={{ color: C.textDim, background: "rgba(255,255,255,0.04)", border: `1px solid ${C.panelBorder}` }}
      >
        <strong className="mr-1 text-sm" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
          {itemCount}
        </strong>
        개 상품
      </div>
    </div>
  );
}
