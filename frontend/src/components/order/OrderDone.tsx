import { C, krw } from "../../lib/theme";
import { SummaryRow } from "../common/SummaryRow";

/* ─── order complete ─────────────────────────────────────── */
export function OrderDone({ orderNo, total, onHome }: { orderNo: string; total: number; onHome: () => void }) {
  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-16">
      <div className="max-w-md mx-auto p-10 text-center" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
        <div className="text-2xl font-bold uppercase mb-3" style={{ fontFamily: "Cinzel, serif", color: C.text }}>
          Order Complete
        </div>
        <p className="text-sm mb-8" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
          주문이 정상적으로 접수되었습니다.
        </p>

        <div className="py-4 mb-6" style={{ borderTop: `1px solid ${C.panelBorder}`, borderBottom: `1px solid ${C.panelBorder}` }}>
          <SummaryRow label="주문번호" value={orderNo} />
          <SummaryRow label="결제금액" value={krw(total)} />
        </div>

        <button
          onClick={onHome}
          className="w-full py-3 text-sm font-bold uppercase tracking-widest"
          style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
        >
          쇼핑 계속하기 →
        </button>
      </div>
    </div>
  );
}
