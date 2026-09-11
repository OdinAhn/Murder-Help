import type { Tier } from "../../catalog";
import { C } from "../../lib/theme";
import { TIERS, canAccess } from "../../lib/tier";

export function CodeTab({
  tier, active, userTier, onClick,
}: {
  tier: Tier; active: boolean; userTier: Tier | null; onClick: () => void;
}) {
  const t = TIERS[tier];
  const owned = userTier === tier;
  /* 내 등급보다 높은 탭은 열 수 없다 */
  const locked = !canAccess(userTier, tier);

  return (
    <button
      onClick={locked ? undefined : onClick}
      disabled={locked}
      title={locked ? `${t.label} 등급부터 볼 수 있습니다` : undefined}
      className="flex-1 flex flex-col items-center gap-1.5 py-4 transition-all relative"
      style={{
        background: active ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.28)",
        borderBottom: active ? `2px solid ${t.color}` : "2px solid transparent",
        opacity: locked ? 0.4 : 1,
        cursor: locked ? "not-allowed" : "pointer",
      }}
    >
      <div className="flex items-center gap-2">
        <span
          style={{
            fontFamily: "Share Tech Mono, monospace",
            fontSize: 9,
            color: C.textMuted,
            letterSpacing: "0.25em",
            textTransform: "uppercase",
          }}
        >
          Code
        </span>
        {owned && (
          <span
            className="text-[9px] px-1 py-px"
            style={{ background: t.color, color: "#fff", fontFamily: "Share Tech Mono", letterSpacing: "0.1em" }}
          >
            MY
          </span>
        )}
        {locked && (
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke={C.textMuted} strokeWidth="2.5">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        )}
      </div>
      <span
        className="font-bold tracking-wide uppercase"
        style={{
          fontFamily: "Cinzel, serif",
          fontSize: "clamp(12px,1.8vw,18px)",
          color: active ? t.brightColor : C.textDim,
        }}
      >
        {tier === "red" ? "Red" : tier === "purple" ? "Purple" : "Yellow"}
      </span>
      <div className="w-full px-4">
        <div className="h-px" style={{ background: "rgba(255,255,255,0.07)" }}>
          <div className="h-full" style={{ width: tier === "red" ? "68%" : tier === "purple" ? "45%" : "82%", background: t.color }} />
        </div>
      </div>
      <span className="text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
        {t.priceRange}
      </span>
    </button>
  );
}
