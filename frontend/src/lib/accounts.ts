import { PURPLE_AT, RED_AT } from "./tier";

/* ─── demo accounts ──────────────────────────────────────── */
/* 백엔드가 붙기 전까지 계정은 여기에 있다. 회원가입으로 만든 계정도
   들어가지만 새로고침하면 사라진다 — 비밀번호를 브라우저에 저장하지
   않으려고 일부러 메모리에만 둔다. */
export const ACCOUNTS: Record<string, { pw: string; spent: number }> = {
  green: { pw: "1234", spent: 999999 },
  red: { pw: "1234", spent: RED_AT },
  purple: { pw: "1234", spent: PURPLE_AT },
  yellow: { pw: "1234", spent: 0 },
};

export const MIN_ID = 4;
export const MIN_PW = 6;
