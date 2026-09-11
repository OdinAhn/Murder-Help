import type { Tier } from "../../catalog";
import { C, krw } from "../../lib/theme";
import { PURPLE_AT, RED_AT } from "../../lib/tier";
import { TierBadge } from "../member/TierBadge";

/* ─── 로그인 게이트 ──────────────────────────────────────── */
/* 로그인 전에는 상품을 볼 수 없다. 목록 대신 이 화면만 보여준다. */
export function Gate({ onLogin }: { onLogin: () => void }) {
  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-20">
      <div
        className="max-w-md mx-auto p-10 text-center"
        style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}
      >
        <div className="flex justify-center mb-5">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke={C.redBright} strokeWidth="1.5">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>

        <div className="text-2xl font-bold uppercase mb-3" style={{ fontFamily: "Cinzel, serif", color: C.text }}>
          Members Only
        </div>
        <p className="text-sm leading-relaxed mb-8" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
          로그인하셔야 상품을 보실 수 있습니다.<br />
          회원 등급에 따라 열람 가능한 상품이 달라집니다.
        </p>

        <div className="mb-8" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
          {(["red", "purple", "yellow"] as Tier[]).map((tier) => (
            <div
              key={tier}
              className="flex items-center justify-between py-2.5"
              style={{ borderBottom: `1px solid ${C.panelBorder}` }}
            >
              <TierBadge tier={tier} small />
              <span className="text-[11px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                {tier === "yellow" ? "기본 등급" : `누적 구매 ${krw(tier === "purple" ? PURPLE_AT : RED_AT)} 이상`}
              </span>
            </div>
          ))}
        </div>

        <button
          onClick={onLogin}
          className="w-full py-3.5 text-sm font-bold uppercase tracking-widest"
          style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
        >
          Login →
        </button>
        <p className="text-[10px] mt-4" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
          BB탄 전용 에어소프트 제품입니다. 만 18세 이상만 가입하실 수 있습니다.
        </p>
      </div>
    </div>
  );
}
