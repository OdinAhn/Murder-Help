import type { Tier } from "../../catalog";
import { TIERS } from "../../lib/tier";

export function TierBadge({ tier, small }: { tier: Tier; small?: boolean }) {
  const t = TIERS[tier];
  return (
    <span
      className="uppercase font-bold tracking-wider rounded-full flex items-center justify-center"
      style={{
        fontFamily: "Share Tech Mono, sans-serif",
        fontSize: small ? 9 : 10,
        color: t.brightColor,
        padding: small ? "2px 6px" : "3px 8px",
        background: `${t.color}25`,
        border: `1px solid ${t.color}40`,
        lineHeight: 1,
      }}
    >
      {t.label}
    </span>
  );
}
