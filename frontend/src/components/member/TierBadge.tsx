import type { Tier } from "../../catalog";
import { TIERS } from "../../lib/tier";

export function TierBadge({ tier, small }: { tier: Tier; small?: boolean }) {
  const t = TIERS[tier];
  return (
    <span
      className="uppercase font-bold tracking-widest"
      style={{
        fontFamily: "Share Tech Mono",
        fontSize: small ? 9 : 10,
        color: t.brightColor,
        border: `1px solid ${t.color}`,
        padding: small ? "1px 4px" : "2px 6px",
        background: `${t.color}18`,
      }}
    >
      {t.label.toUpperCase()}
    </span>
  );
}
