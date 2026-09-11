/* ─── 세션 · 장바구니 보관 ───────────────────────────────── */
/* localStorage 는 사생활 보호 모드나 차단 설정에서 예외를 던지므로 모두 감싼다 */
export const SESSION_KEY = "murderhelp.session";

/* 장바구니는 계정마다 따로 보관한다. 로그아웃해도 남아 있다가
   같은 계정으로 다시 로그인하면 그대로 돌아온다. */
export const cartKeyFor = (accountId: string) => `murderhelp.cart.${accountId}`;

export function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

export function write(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* 저장이 막혀 있으면 이번 세션에서만 유지된다 */
  }
}

export function drop(key: string) {
  try {
    localStorage.removeItem(key);
  } catch {
    /* 무시 */
  }
}
