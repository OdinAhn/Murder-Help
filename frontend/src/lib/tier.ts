import type { Member } from "../api/auth";
import type { Tier } from "../catalog";
import { C } from "./theme";

/* ─── tier system ───────────────────────────────────────── */
export const TIERS: Record<Tier, { label: string; color: string; brightColor: string; desc: string; priceRange: string }> = {
  green:  { label: "Code Green",  color: "#10b981", brightColor: "#34d399", desc: "본사 관리자 전용 / HQ Administration", priceRange: "Admin" },
  red:    { label: "Code Red",    color: C.red,    brightColor: C.redBright,    desc: "VIP 회원 등급 — 프리미엄 한정판 장비",       priceRange: "3,000원~" },
  purple: { label: "Code Purple", color: C.purple, brightColor: C.purpleBright, desc: "중급 회원 등급 — 중급 퍼포먼스 장비",        priceRange: "1,000 ~ 3,000원" },
  yellow: { label: "Code Yellow", color: C.yellow, brightColor: C.yellowBright, desc: "기본 회원 등급 — 엔트리급 BB탄 장비",       priceRange: "~1,000원" },
};

/* ─── 등급 승급 ───────────────────────────────────────────── */
/* 등급은 계정에 고정된 값이 아니라 누적 구매금액에서 계산된다.
   가입 직후 0원 = Code Yellow 에서 시작하고, 주문이 완료될 때마다 쌓인다. */
export const PURPLE_AT = 200_000;
export const RED_AT = 800_000;

export function tierFor(spent: number, id?: string): Tier {
  if (id === "green") return "green";
  if (spent >= RED_AT) return "red";
  if (spent >= PURPLE_AT) return "purple";
  return "yellow";
}

/* 서버는 누적 구매금액 없이 등급만 내려준다. 화면의 등급 계산과 진행 바는 금액 기준이라
   등급을 그 등급의 기준 금액으로 바꿔 쓴다. 로그인과 새로고침 복원에서 같이 쓴다. */
export function spentFromGrade(grade: Member["grade"]) {
  switch (grade) {
    case "GREEN":
      return 1_000_000;
    case "RED":
      return RED_AT;
    case "PURPLE":
      return PURPLE_AT;
    case "YELLOW":
    default:
      return 0;
  }
}

/* 다음 등급까지 남은 금액. 최고 등급이면 null */
export function nextTier(spent: number, id?: string): { tier: Tier; remaining: number } | null {
  if (id === "green") return null;
  if (spent >= RED_AT) return null;
  if (spent >= PURPLE_AT) return { tier: "red", remaining: RED_AT - spent };
  return { tier: "purple", remaining: PURPLE_AT - spent };
}

/* 자기 등급 이하의 상품만 볼 수 있다. 로그인하지 않으면 아무것도 볼 수 없다. */
export const TIER_RANK: Record<Tier, number> = { yellow: 0, purple: 1, red: 2, green: 99 };

export function canAccess(userTier: Tier | null, productTier: Tier) {
  if (!userTier) return false;
  return TIER_RANK[productTier] <= TIER_RANK[userTier];
}
