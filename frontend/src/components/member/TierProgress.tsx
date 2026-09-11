import { C, krw } from "../../lib/theme";
import { PURPLE_AT, RED_AT, TIERS, nextTier } from "../../lib/tier";

/* ─── 등급 진행 바 ───────────────────────────────────────── */
export function TierProgress({ spent }: { spent: number }) {
  const next = nextTier(spent);
  const base = spent < PURPLE_AT ? 0 : PURPLE_AT;
  const target = spent < PURPLE_AT ? PURPLE_AT : RED_AT;
  const pct = next ? ((spent - base) / (target - base)) * 100 : 100;
  const color = TIERS[next ? next.tier : "red"].color;

  return (
    <div className="w-full">
      <div className="flex justify-between items-baseline mb-1.5">
        <span className="text-[10px] uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
          누적 구매 {krw(spent)}
        </span>
        <span className="text-[10px]" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
          {next ? `${TIERS[next.tier].label}까지 ${krw(next.remaining)}` : "최고 등급 달성"}
        </span>
      </div>
      <div className="h-1" style={{ background: "rgba(255,255,255,0.08)" }}>
        <div className="h-full transition-all" style={{ width: `${Math.min(pct, 100)}%`, background: color }} />
      </div>
    </div>
  );
}
