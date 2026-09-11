import { useState } from "react";
import type { Product } from "../../catalog";
import { C, krw } from "../../lib/theme";
import { TIERS } from "../../lib/tier";
import { TierBadge } from "../member/TierBadge";

/* ─── product card ───────────────────────────────────────── */
export function ProductCard({ p, onOpen }: { p: Product; onOpen: () => void }) {
  const [hov, setHov] = useState(false);
  const tierColor = TIERS[p.tier].color;
  const tierBright = TIERS[p.tier].brightColor;

  /* HOT — 인기가 가장 많은 상품 · NEW — 새로 등록된 상품 */
  const badgeColors: Record<string, string> = {
    HOT: C.red,
    NEW: "#1a7a3a",
  };

  return (
    <div
      onClick={onOpen}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      className="cursor-pointer transition-all relative"
      style={{
        background: hov ? "rgba(25,0,0,0.95)" : "rgba(10,0,0,0.82)",
        border: `1px solid ${hov ? tierColor : C.panelBorder}`,
      }}
    >
      {/* image */}
      <div className="relative overflow-hidden" style={{ background: "#060606", height: 180 }}>
        <img
          src={p.img}
          alt={p.name}
          className="w-full h-full object-cover transition-transform duration-500"
          style={{
            transform: hov ? "scale(1.06)" : "scale(1)",
            filter: `brightness(0.8) saturate(0.65)`,
          }}
        />
        {p.badge && (
          <span
            className="absolute top-2 left-2 text-[9px] font-black uppercase tracking-widest px-1.5 py-0.5"
            style={{ background: badgeColors[p.badge] ?? C.red, color: "#fff", fontFamily: "Share Tech Mono" }}
          >
            {p.badge}
          </span>
        )}
        {/* tier indicator strip */}
        <div className="absolute bottom-0 left-0 right-0 h-0.5" style={{ background: tierColor }} />
      </div>

      {/* info */}
      <div className="px-3 py-3">
        <div className="flex items-center justify-between mb-1.5">
          <span
            className="text-[10px]"
            style={{ color: C.textMuted, fontFamily: "Share Tech Mono", letterSpacing: "0.1em" }}
          >
            item No. {p.id}
          </span>
          <TierBadge tier={p.tier} small />
        </div>
        <div className="text-sm mb-2 leading-snug" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
          {p.name}
        </div>
        <div className="text-base font-bold mb-2" style={{ color: tierBright, fontFamily: "Share Tech Mono" }}>
          {krw(p.price)}
        </div>
      </div>
    </div>
  );
}
