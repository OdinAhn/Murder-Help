import { useEffect, useState } from "react";
import type { Product } from "../../catalog";
import { C, krw } from "../../lib/theme";
import { TIERS } from "../../lib/tier";
import { QtyStepper } from "../common/QtyStepper";
import { TierBadge } from "../member/TierBadge";

/* ─── product detail ─────────────────────────────────────── */
export function ProductDetail({
  p, onBack, onAddToCart, onBuyNow,
}: {
  p: Product;
  onBack: () => void;
  /* 로그인이 필요해 담기지 않으면 false 를 돌려준다 */
  onAddToCart: (qty: number) => boolean;
  onBuyNow: (qty: number) => void;
}) {
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);
  const t = TIERS[p.tier];

  useEffect(() => {
    setQty(1);
    setAdded(false);
    window.scrollTo({ top: 0 });
  }, [p.id]);

  function add() {
    if (!onAddToCart(qty)) return;
    setAdded(true);
    window.setTimeout(() => setAdded(false), 1800);
  }

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-6">
      <button
        onClick={onBack}
        className="text-xs uppercase tracking-widest mb-4"
        style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
      >
        ← 목록으로
      </button>

      <div className="flex flex-col md:flex-row gap-0" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
        {/* 사진 */}
        <div className="md:w-[52%] shrink-0 relative" style={{ background: "#060606" }}>
          <img
            src={p.img}
            alt={p.name}
            className="w-full object-cover"
            style={{ height: 420, filter: "brightness(0.85) saturate(0.7)" }}
          />
          {p.badge && (
            <span
              className="absolute top-3 left-3 text-[10px] font-black uppercase tracking-widest px-2 py-1"
              style={{
                background: p.badge === "NEW" ? "#1a7a3a" : C.red,
                color: "#fff",
                fontFamily: "Share Tech Mono",
              }}
            >
              {p.badge}
            </span>
          )}
          <div className="absolute bottom-0 left-0 right-0" style={{ height: 3, background: t.color }} />
        </div>

        {/* 정보 */}
        <div className="flex-1 p-6 md:p-8">
          <div
            className="flex items-center gap-2 mb-3 text-[10px] uppercase tracking-widest"
            style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
          >
            <span>{p.category}</span>
            <span style={{ color: C.redDim }}>/</span>
            <span>{p.sub}</span>
            <span style={{ color: C.redDim }}>/</span>
            <span>item No. {p.id}</span>
          </div>

          <div className="mb-3">
            <TierBadge tier={p.tier} />
          </div>

          <h1 className="text-2xl leading-snug mb-4" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
            {p.name}
          </h1>

          <div className="text-3xl font-bold mb-5" style={{ color: t.brightColor, fontFamily: "Share Tech Mono" }}>
            {krw(p.price)}
          </div>

          <p className="text-sm leading-relaxed mb-6" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
            {p.desc || "상세 설명은 준비 중입니다."}
          </p>

          {/* 제원 */}
          <div className="mb-7" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
            {p.specs.map(([label, value]) => (
              <div key={label} className="flex gap-4 py-2.5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                <span
                  className="text-[10px] uppercase tracking-widest shrink-0"
                  style={{ color: C.textMuted, fontFamily: "Share Tech Mono", width: 92, paddingTop: 2 }}
                >
                  {label}
                </span>
                <span className="text-xs" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                  {value}
                </span>
              </div>
            ))}
          </div>

          {/* 수량 + 구매 */}
          <div className="flex items-center gap-4 mb-4">
            <span className="text-[10px] uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
              수량
            </span>
            <QtyStepper qty={qty} onChange={setQty} />
            <span className="text-sm ml-auto" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
              합계 <span style={{ color: t.brightColor }}>{krw(p.price * qty)}</span>
            </span>
          </div>

          <div className="flex gap-3">
            <button
              onClick={add}
              className="flex-1 py-3.5 text-sm font-bold uppercase tracking-widest transition-all"
              style={{
                border: `1px solid ${added ? t.color : C.panelBorder}`,
                color: added ? t.brightColor : C.text,
                background: "rgba(0,0,0,0.4)",
                fontFamily: "Share Tech Mono",
              }}
            >
              {added ? "담았습니다 ✓" : "장바구니에 담기"}
            </button>
            <button
              onClick={() => onBuyNow(qty)}
              className="flex-1 py-3.5 text-sm font-bold uppercase tracking-widest transition-all"
              style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
            >
              결제하기 →
            </button>
          </div>

          <p className="text-[10px] mt-4" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
            BB탄 전용 에어소프트 제품입니다. 만 18세 이상만 구매하실 수 있습니다.
          </p>
        </div>
      </div>
    </div>
  );
}
