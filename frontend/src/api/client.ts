/* ─── 인증 토큰 · API 호출 ───────────────────────────────── */
/* accessToken 은 XSS 로 새어 나가지 않도록 메모리에만 둔다.
   refreshToken 은 서버가 httpOnly 쿠키로 내려주므로 JS 에서 건드리지 않는다.

   - 새로고침하면 accessToken 이 사라지므로 앱 시작 시 reissue() 로 한 번 다시 받는다.
   - 사용 중에 accessToken 이 만료되면 API 가 401 을 주는데,
     authFetch 가 재발급을 받고 원래 요청을 한 번 더 보낸다. */

type ApiResponse<T> = {
  code: string;
  message?: string;
  data?: T;
};

type TokenResponse = {
  accessToken: string;
};

let accessToken: string | null = null;
let reissuing: Promise<void> | null = null;
const expiredListeners = new Set<() => void>();

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

/** refreshToken 까지 만료돼 로그인이 끊겼을 때 알림을 받는다. 반환된 함수를 호출하면 구독이 해제된다. */
export function onAuthExpired(listener: () => void): () => void {
  expiredListeners.add(listener);
  return () => {
    expiredListeners.delete(listener);
  };
}

/** POST /api/auth/reissue — refreshToken 쿠키로 accessToken 을 다시 받는다 */
export function reissue(): Promise<void> {
  // 서버는 reissue 할 때마다 refreshToken 을 새 값으로 바꾼다.
  // 동시에 두 번 요청하면 뒤 요청은 이미 바뀐 토큰과 비교돼 실패하므로
  // (여러 API 가 한꺼번에 401 을 받거나, StrictMode 에서 effect 가 두 번 돌 때)
  // 진행 중인 요청이 있으면 그 결과를 같이 기다린다.
  if (!reissuing) {
    reissuing = requestReissue().finally(() => {
      reissuing = null;
    });
  }

  return reissuing;
}

async function requestReissue(): Promise<void> {
  const response = await fetch("/api/auth/reissue", {
    method: "POST",
    credentials: "include",
  });

  const body = (await response.json().catch(() => null)) as ApiResponse<TokenResponse> | null;
  if (!response.ok || !body || body.code !== "SUCCESS" || !body.data?.accessToken) {
    throw new Error(body?.message ?? `토큰 재발급에 실패했습니다. (${response.status})`);
  }

  accessToken = body.data.accessToken;
}

/** 로그인이 필요한 API 는 모두 이 함수로 호출한다 */
export async function authFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const response = await send(url, init);
  if (response.status !== 401) return response;

  // accessToken 만료 — 재발급을 받은 뒤 원래 요청을 한 번만 다시 보낸다
  try {
    await reissue();
  } catch {
    accessToken = null;
    expiredListeners.forEach((listener) => listener());
    throw new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.");
  }

  return send(url, init);
}

function send(url: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

  return fetch(url, { ...init, headers, credentials: "include" });
}
