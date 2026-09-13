/* ─── 세션 보관 ──────────────────────────────────────────── */
/* localStorage 는 사생활 보호 모드나 차단 설정에서 예외를 던지므로 모두 감싼다 */
export const SESSION_KEY = "murderhelp.session";

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
