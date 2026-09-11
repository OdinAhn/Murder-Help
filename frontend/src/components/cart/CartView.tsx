import type { Product } from "../../catalog";
import { C, krw } from "../../lib/theme";
import { TIERS } from "../../lib/tier";
import { PageTitle } from "../common/PageTitle";
import { QtyStepper } from "../common/QtyStepper";
import { Spinner } from "../common/Spinner";
import { SummaryRow } from "../common/SummaryRow";
import { TierBadge } from "../member/TierBadge";

/* ─── cart ───────────────────────────────────────────────── */
export function CartView({
  lines, loading, error, pendingIds, onQty, onRemove, onRetry, onContinue, onCheckout,
}: {
  lines: { p: Product; qty: number }[];
  loading: boolean;
  error: string | null;
  pendingIds: Set<string>;
  onQty: (id: string, qty: number) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
  onRetry: () => void;
  onContinue: () => void;
  onCheckout: () => void;
}) {
  const itemsTotal = lines.reduce((sum, l) => sum + l.p.price * l.qty, 0);

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
      <PageTitle note={`// ${lines.length}개 품목`}>Cart</PageTitle>

      {error && (
        <div
          className="mb-4 flex items-center justify-between gap-4 px-4 py-3"
          style={{ background: C.panel, border: `1px solid ${C.redDim}` }}
        >
          <p className="text-xs" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
            {error}
          </p>
          <button
            type="button"
            onClick={onRetry}
            className="shrink-0 px-3 py-1.5 text-[10px] font-bold uppercase tracking-widest"
            style={{ color: C.text, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
          >
            다시 시도
          </button>
        </div>
      )}

      {loading ? (
        <div
          className="flex items-center justify-center gap-3 py-20"
          style={{ border: `1px dashed ${C.panelBorder}`, color: C.textDim }}
        >
          <Spinner color={C.redBright} />
          <span className="text-xs" style={{ fontFamily: "Share Tech Mono" }}>
            장바구니를 불러오는 중...
          </span>
        </div>
      ) : lines.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4" style={{ border: `1px dashed ${C.panelBorder}` }}>
          <div className="text-2xl font-bold uppercase" style={{ fontFamily: "Cinzel, serif", color: C.redDim }}>
            Cart Is Empty
          </div>
          <button
            onClick={onContinue}
            className="px-8 py-2.5 text-xs font-bold uppercase tracking-widest"
            style={{ border: `1px solid ${C.panelBorder}`, color: C.textDim, fontFamily: "Share Tech Mono" }}
          >
            쇼핑 계속하기 →
          </button>
        </div>
      ) : (
        <div className="flex flex-col lg:flex-row gap-6 items-start">
          {/* 목록 */}
          <div className="flex-1 w-full" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
            {lines.map(({ p, qty }) => {
              const pending = pendingIds.has(p.id);

              return (
              <div
                key={p.id}
                className="flex items-center gap-4 p-4"
                style={{ borderBottom: `1px solid ${C.panelBorder}`, opacity: pending ? 0.65 : 1 }}
              >
                <img
                  src={p.img}
                  alt={p.name}
                  className="object-cover shrink-0"
                  style={{ width: 84, height: 64, filter: "brightness(0.8) saturate(0.65)" }}
                />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      {p.id}
                    </span>
                    <TierBadge tier={p.tier} small />
                  </div>
                  <div className="text-sm truncate" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                    {p.name}
                  </div>
                  <div className="text-xs mt-1" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                    {krw(p.price)} / 개
                  </div>
                </div>
                <div style={{ pointerEvents: pending ? "none" : "auto" }}>
                  <QtyStepper qty={qty} onChange={(n) => void onQty(p.id, n)} />
                </div>
                <div
                  className="text-sm font-bold text-right shrink-0"
                  style={{ width: 90, color: TIERS[p.tier].brightColor, fontFamily: "Share Tech Mono" }}
                >
                  {krw(p.price * qty)}
                </div>
                <button
                  onClick={() => void onRemove(p.id)}
                  disabled={pending}
                  className="text-xs px-2 shrink-0"
                  style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
                  aria-label="삭제"
                >
                  {pending ? "…" : "✕"}
                </button>
              </div>
              );
            })}
          </div>

          {/* 합계 */}
          <div className="w-full lg:w-80 shrink-0 p-5" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
            <div className="text-[10px] uppercase tracking-widest mb-4" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
              // 주문 요약
            </div>
            <SummaryRow label="상품 합계" value={krw(itemsTotal)} />
            <div className="flex justify-between items-baseline py-3 mt-2" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
              <span className="text-xs uppercase tracking-widest" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                총 결제금액
              </span>
              <span className="text-xl font-bold" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
                {krw(itemsTotal)}
              </span>
            </div>
            <button
              onClick={onCheckout}
              className="w-full py-3.5 mt-3 text-sm font-bold uppercase tracking-widest"
              style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
            >
              결제하기 →
            </button>
            <button
              onClick={onContinue}
              className="w-full py-2.5 mt-2 text-xs uppercase tracking-widest"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              쇼핑 계속하기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
