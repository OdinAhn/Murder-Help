import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { NAV_ITEMS, PRODUCTS, SUBCATS, type Product, type Tier } from "./catalog";

/* ─── palette ─────────────────────────────────────────── */
const C = {
  bg: "radial-gradient(ellipse at top, #5a0a0a 0%, #2d0000 40%, #0e0000 100%)",
  panel: "rgba(0,0,0,0.72)",
  panelBorder: "rgba(180,0,0,0.25)",
  red: "#cc2200",
  redBright: "#e83010",
  redDim: "#8b1a08",
  purple: "#7c3aed",
  purpleBright: "#9d5bf5",
  yellow: "#c8a100",
  yellowBright: "#f0c820",
  text: "#f0e0d8",
  textDim: "#a08070",
  textMuted: "#604040",
  price: "#e83010",
};

/* ─── tier system ───────────────────────────────────────── */
const TIERS: Record<Tier, { label: string; color: string; brightColor: string; desc: string; priceRange: string }> = {
  red:    { label: "Code Red",    color: C.red,    brightColor: C.redBright,    desc: "VIP 회원 등급 — 프리미엄 한정판 장비",       priceRange: "3,000원~" },
  purple: { label: "Code Purple", color: C.purple, brightColor: C.purpleBright, desc: "중급 회원 등급 — 중급 퍼포먼스 장비",        priceRange: "1,000 ~ 3,000원" },
  yellow: { label: "Code Yellow", color: C.yellow, brightColor: C.yellowBright, desc: "기본 회원 등급 — 엔트리급 BB탄 장비",       priceRange: "~1,000원" },
};

function krw(n: number) {
  return "₩" + n.toLocaleString("ko-KR");
}

/* ─── 배송비 ─────────────────────────────────────────────── */
const SHIPPING_FEE = 2500;
const FREE_SHIPPING_OVER = 20000;

/* ─── 등급 승급 ───────────────────────────────────────────── */
/* 등급은 계정에 고정된 값이 아니라 서바이벌 게임 참여 횟수에서 계산된다.
   가입 직후 0회 = Code Yellow 에서 시작한다. */
const PURPLE_AT = 20;
const RED_AT = 80;

function tierFor(plays: number): Tier {
  if (plays >= RED_AT) return "red";
  if (plays >= PURPLE_AT) return "purple";
  return "yellow";
}

/* 다음 등급과 남은 횟수. 최고 등급이면 null */
function nextTier(plays: number): { tier: Tier; remaining: number } | null {
  if (plays < PURPLE_AT) return { tier: "purple", remaining: PURPLE_AT - plays };
  if (plays < RED_AT) return { tier: "red", remaining: RED_AT - plays };
  return null;
}

/* ─── demo accounts ──────────────────────────────────────── */
/* 백엔드가 붙기 전까지 계정은 여기에 있다. 회원가입으로 만든 계정도
   들어가지만 새로고침하면 사라진다 — 비밀번호를 브라우저에 저장하지
   않으려고 일부러 메모리에만 둔다. */
const ACCOUNTS: Record<string, { pw: string; plays: number }> = {
  red: { pw: "1234", plays: RED_AT },
  purple: { pw: "1234", plays: PURPLE_AT },
  yellow: { pw: "1234", plays: 0 },
};

const MIN_ID = 4;
const MIN_PW = 6;
const CODE_LENGTH = 8;

/* ─── 세션 · 장바구니 보관 ───────────────────────────────── */
/* localStorage 는 사생활 보호 모드나 차단 설정에서 예외를 던지므로 모두 감싼다 */
const SESSION_KEY = "murderhelp.session";
const CART_KEY = "murderhelp.cart";

function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function write(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* 저장이 막혀 있으면 이번 세션에서만 유지된다 */
  }
}

function drop(key: string) {
  try {
    localStorage.removeItem(key);
  } catch {
    /* 무시 */
  }
}

/* ─── 세션 ───────────────────────────────────────────────── */
/* 등급은 저장하지 않는다. plays 에서 계산하므로 저장하면 두 값이 어긋난다. */
type Session = {
  id: string;
  plays: number;
  usedCodes: string[];
  remember: boolean;
};

/* ─── 주문 ───────────────────────────────────────────────── */
type CartLine = { id: string; qty: number };

type Receiver = {
  name: string;
  phone: string;
  postcode: string;
  address: string;
  detail: string;
  memo: string;
};

type OrderPayload = {
  items: { id: string; name: string; price: number; qty: number }[];
  itemsTotal: number;
  shipping: number;
  total: number;
  receiver: Receiver;
  memberTier: Tier | null;
};

/* ══════════════════════════════════════════════════════════
   백엔드 연동 지점.
   실제 결제를 붙일 때 고쳐야 하는 곳은 이 함수 하나뿐이다.
   서버에 주문을 만들고 PG 결제창을 띄운 뒤 주문번호를 돌려주면 된다.

     const res = await fetch("/api/orders", {
       method: "POST",
       headers: { "Content-Type": "application/json" },
       body: JSON.stringify(payload),
     });
     if (!res.ok) throw new Error("주문 생성 실패");
     return await res.json();

   결제 키는 서버에만 두어야 하며 이 번들에 넣으면 안 된다.
   ══════════════════════════════════════════════════════════ */
async function placeOrder(payload: OrderPayload): Promise<{ orderNo: string }> {
  await new Promise((resolve) => setTimeout(resolve, 900));
  void payload;
  return { orderNo: "MH" + String(Date.now()).slice(-8) };
}

/* ─── 화면 ───────────────────────────────────────────────── */
type View =
  | { name: "list" }
  | { name: "detail"; id: string }
  | { name: "cart" }
  | { name: "checkout" }
  | { name: "done"; orderNo: string; total: number };

/* ─── 공통 조각 ──────────────────────────────────────────── */
function TierBadge({ tier, small }: { tier: Tier; small?: boolean }) {
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
      {tier === "red" ? "CODE RED" : tier === "purple" ? "CODE PURPLE" : "CODE YELLOW"}
    </span>
  );
}

function Spinner({ color = "#fff" }: { color?: string }) {
  return (
    <span
      className="inline-block align-middle"
      style={{
        width: 12,
        height: 12,
        border: `2px solid ${color}`,
        borderTopColor: "transparent",
        borderRadius: "50%",
        animation: "mh-spin 0.7s linear infinite",
      }}
    />
  );
}

function Field({
  label, value, onChange, placeholder, type = "text",
}: {
  label: string; value: string; onChange: (v: string) => void; placeholder?: string; type?: string;
}) {
  return (
    <label className="block">
      <span className="block text-[10px] uppercase tracking-widest mb-1.5" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full px-4 py-3 text-sm outline-none"
        style={{
          background: "rgba(0,0,0,0.45)",
          border: `1px solid ${C.panelBorder}`,
          color: C.text,
          fontFamily: "Noto Sans KR, sans-serif",
        }}
      />
    </label>
  );
}

function QtyStepper({ qty, onChange }: { qty: number; onChange: (n: number) => void }) {
  const btn = {
    width: 32,
    height: 32,
    border: `1px solid ${C.panelBorder}`,
    color: C.textDim,
    fontFamily: "Share Tech Mono",
    background: "rgba(0,0,0,0.4)",
  };
  return (
    <div className="flex items-center">
      <button type="button" onClick={() => onChange(Math.max(1, qty - 1))} style={btn} aria-label="수량 줄이기">−</button>
      <span className="text-sm text-center" style={{ width: 46, color: C.text, fontFamily: "Share Tech Mono" }}>
        {qty}
      </span>
      <button type="button" onClick={() => onChange(Math.min(99, qty + 1))} style={btn} aria-label="수량 늘리기">+</button>
    </div>
  );
}

function PageTitle({ children, note }: { children: ReactNode; note?: string }) {
  return (
    <div className="mb-6">
      <h1
        className="font-bold uppercase leading-none mb-2"
        style={{ fontFamily: "Cinzel, serif", fontSize: "clamp(20px,3vw,32px)", color: C.text }}
      >
        {children}
      </h1>
      {note && (
        <p className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
          {note}
        </p>
      )}
    </div>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between items-baseline py-1.5">
      <span className="text-xs" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>{label}</span>
      <span className="text-sm" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>{value}</span>
    </div>
  );
}

/* ─── login modal ────────────────────────────────────────── */
function LoginModal({ onLogin, onClose }: { onLogin: (id: string, plays: number, remember: boolean) => void; onClose: () => void }) {
  const [mode, setMode] = useState<"login" | "signup" | "forgot" | "sent">("login");
  const [id, setId] = useState("");
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [isAdult, setIsAdult] = useState(false);
  const [agreed, setAgreed] = useState(false);
  const [remember, setRemember] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const fieldStyle = {
    background: "rgba(0,0,0,0.45)",
    border: `1px solid ${C.panelBorder}`,
    color: C.text,
    fontFamily: "Noto Sans KR, sans-serif",
  };

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");

    /* 서버 확인을 기다리는 구간. 실제 인증을 붙이면 이 자리에 API 호출이 들어간다 */
    await new Promise((resolve) => setTimeout(resolve, 700));

    const key = id.trim().toLowerCase();
    const account = ACCOUNTS[key];
    if (!account || account.pw !== pw) {
      setBusy(false);
      setError("아이디 또는 비밀번호가 올바르지 않습니다.");
      return;
    }
    onLogin(key, account.plays, remember);
  }

  async function sendReset(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    if (!id.trim()) {
      setError("아이디를 입력해 주세요.");
      return;
    }
    setBusy(true);
    setError("");
    await new Promise((resolve) => setTimeout(resolve, 700));
    setBusy(false);
    setMode("sent");
  }

  /* 회원가입. 서버가 붙으면 이 검사는 서버 응답으로 대체된다 —
     아이디 중복은 특히 클라이언트에서 판정할 수 없다. */
  async function signup(e: FormEvent) {
    e.preventDefault();
    if (busy) return;

    const account = id.trim().toLowerCase();
    if (account.length < MIN_ID) {
      setError(`아이디는 ${MIN_ID}자 이상이어야 합니다.`);
      return;
    }
    if (ACCOUNTS[account]) {
      setError("이미 사용 중인 아이디입니다.");
      return;
    }
    if (pw.length < MIN_PW) {
      setError(`비밀번호는 ${MIN_PW}자 이상이어야 합니다.`);
      return;
    }
    if (pw !== pw2) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }
    if (!isAdult) {
      setError("만 18세 이상만 가입하실 수 있습니다.");
      return;
    }
    if (!agreed) {
      setError("이용약관 및 개인정보 처리방침에 동의해 주세요.");
      return;
    }

    setBusy(true);
    setError("");
    await new Promise((resolve) => setTimeout(resolve, 900));

    ACCOUNTS[account] = { pw, plays: 0 };
    onLogin(account, 0, true);
  }

  function goto(next: "login" | "signup" | "forgot") {
    setMode(next);
    setError("");
    setPw("");
    setPw2("");
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center"
      style={{ background: "rgba(0,0,0,0.85)", backdropFilter: "blur(4px)" }}
      onClick={busy ? undefined : onClose}
    >
      <div
        className="w-full max-w-sm mx-4 p-8"
        style={{
          background: "rgba(12,0,0,0.97)",
          border: `1px solid ${C.panelBorder}`,
          boxShadow: `0 0 60px rgba(200,30,0,0.2)`,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          className="text-2xl font-bold uppercase mb-1 tracking-wide"
          style={{ fontFamily: "Cinzel, serif", color: C.text }}
        >
          {mode === "login" && "Member Login"}
          {mode === "signup" && "Join MurderHelp"}
          {(mode === "forgot" || mode === "sent") && "Find Password"}
        </h2>
        <p className="text-xs mb-6" style={{ color: C.textMuted, fontFamily: "Share Tech Mono, monospace" }}>
          {mode === "login" && "// 로그인하시면 회원님의 등급이 자동으로 적용됩니다"}
          {mode === "signup" && "// 신규 회원은 Code Yellow 등급으로 시작합니다"}
          {mode === "forgot" && "// 가입하신 아이디로 재설정 링크를 보내드립니다"}
          {mode === "sent" && "// 전송 완료"}
        </p>

        {mode === "login" && (
          <>
            <form onSubmit={submit}>
              <div className="space-y-3 mb-4">
                <input
                  autoFocus
                  value={id}
                  onChange={(e) => { setId(e.target.value); setError(""); }}
                  placeholder="아이디"
                  disabled={busy}
                  className="w-full px-4 py-3 text-sm outline-none"
                  style={fieldStyle}
                />
                <input
                  type="password"
                  value={pw}
                  onChange={(e) => { setPw(e.target.value); setError(""); }}
                  placeholder="비밀번호"
                  disabled={busy}
                  className="w-full px-4 py-3 text-sm outline-none"
                  style={fieldStyle}
                />
              </div>

              <div className="flex items-center justify-between mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={remember}
                    onChange={(e) => setRemember(e.target.checked)}
                    style={{ accentColor: C.red }}
                  />
                  <span className="text-[11px]" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
                    로그인 상태 유지
                  </span>
                </label>
                <button
                  type="button"
                  onClick={() => goto("forgot")}
                  className="text-[11px]"
                  style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif", textDecoration: "underline" }}
                >
                  비밀번호를 잊으셨나요?
                </button>
              </div>

              {error && (
                <p className="text-xs mb-4" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={busy}
                className="w-full py-3 font-bold uppercase text-sm tracking-widest transition-all flex items-center justify-center gap-2"
                style={{
                  background: busy ? C.redDim : C.red,
                  color: "#fff",
                  fontFamily: "Share Tech Mono",
                  border: `1px solid ${busy ? C.redDim : C.redBright}`,
                  cursor: busy ? "wait" : "pointer",
                }}
              >
                {busy ? <><Spinner /> 확인 중…</> : "Login →"}
              </button>
            </form>

            <button
              onClick={onClose}
              disabled={busy}
              className="w-full mt-2 py-2 text-xs uppercase tracking-widest transition-all"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              Cancel
            </button>

            <div className="mt-5 pt-4 flex items-center justify-center gap-2" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
              <span className="text-[11px]" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
                계정이 없으신가요?
              </span>
              <button
                type="button"
                onClick={() => goto("signup")}
                disabled={busy}
                className="text-[11px] font-bold"
                style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif", textDecoration: "underline" }}
              >
                회원가입
              </button>
            </div>

            <div className="mt-5 pt-4" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
              <div className="text-[10px] uppercase tracking-widest mb-2" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                // demo accounts — pw 1234
              </div>
              <div className="space-y-1">
                {(["red", "purple", "yellow"] as const).map((key) => {
                  const plays = ACCOUNTS[key].plays;
                  return (
                    <div key={key} className="flex items-center justify-between text-[11px]" style={{ fontFamily: "Share Tech Mono" }}>
                      <span style={{ color: C.textDim }}>{key}</span>
                      <span style={{ color: C.textMuted }}>{plays}회</span>
                      <span style={{ color: TIERS[tierFor(plays)].brightColor }}>{TIERS[tierFor(plays)].label}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          </>
        )}

        {mode === "signup" && (
          <form onSubmit={signup}>
            <div className="space-y-3 mb-4">
              <input
                autoFocus
                value={id}
                onChange={(e) => { setId(e.target.value); setError(""); }}
                placeholder={`아이디 (${MIN_ID}자 이상)`}
                disabled={busy}
                className="w-full px-4 py-3 text-sm outline-none"
                style={fieldStyle}
              />
              <input
                type="password"
                value={pw}
                onChange={(e) => { setPw(e.target.value); setError(""); }}
                placeholder={`비밀번호 (${MIN_PW}자 이상)`}
                disabled={busy}
                className="w-full px-4 py-3 text-sm outline-none"
                style={fieldStyle}
              />
              <input
                type="password"
                value={pw2}
                onChange={(e) => { setPw2(e.target.value); setError(""); }}
                placeholder="비밀번호 확인"
                disabled={busy}
                className="w-full px-4 py-3 text-sm outline-none"
                style={fieldStyle}
              />
            </div>

            <div className="space-y-2.5 mb-4">
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isAdult}
                  onChange={(e) => { setIsAdult(e.target.checked); setError(""); }}
                  disabled={busy}
                  style={{ accentColor: C.red, marginTop: 2 }}
                />
                <span className="text-[11px] leading-relaxed" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
                  <span style={{ color: C.redBright }}>[필수]</span> 만 18세 이상입니다.
                  BB탄 전용 에어소프트 제품은 만 18세 미만에게 판매하지 않습니다.
                </span>
              </label>
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={agreed}
                  onChange={(e) => { setAgreed(e.target.checked); setError(""); }}
                  disabled={busy}
                  style={{ accentColor: C.red, marginTop: 2 }}
                />
                <span className="text-[11px] leading-relaxed" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
                  <span style={{ color: C.redBright }}>[필수]</span> 이용약관 및 개인정보 처리방침에 동의합니다.
                </span>
              </label>
            </div>

            {error && (
              <p className="text-xs mb-4" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={busy}
              className="w-full py-3 font-bold uppercase text-sm tracking-widest transition-all flex items-center justify-center gap-2"
              style={{
                background: busy ? C.redDim : C.red,
                color: "#fff",
                fontFamily: "Share Tech Mono",
                border: `1px solid ${busy ? C.redDim : C.redBright}`,
                cursor: busy ? "wait" : "pointer",
              }}
            >
              {busy ? <><Spinner /> 가입 중…</> : "가입하기 →"}
            </button>

            <button
              type="button"
              onClick={() => goto("login")}
              disabled={busy}
              className="w-full mt-2 py-2 text-xs uppercase tracking-widest"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              ← 이미 계정이 있습니다
            </button>
          </form>
        )}

        {mode === "forgot" && (
          <form onSubmit={sendReset}>
            <input
              autoFocus
              value={id}
              onChange={(e) => { setId(e.target.value); setError(""); }}
              placeholder="아이디"
              disabled={busy}
              className="w-full px-4 py-3 text-sm outline-none mb-4"
              style={fieldStyle}
            />

            {error && (
              <p className="text-xs mb-4" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={busy}
              className="w-full py-3 font-bold uppercase text-sm tracking-widest transition-all flex items-center justify-center gap-2"
              style={{
                background: busy ? C.redDim : C.red,
                color: "#fff",
                fontFamily: "Share Tech Mono",
                border: `1px solid ${busy ? C.redDim : C.redBright}`,
                cursor: busy ? "wait" : "pointer",
              }}
            >
              {busy ? <><Spinner /> 전송 중…</> : "재설정 링크 보내기 →"}
            </button>

            <button
              type="button"
              onClick={() => goto("login")}
              disabled={busy}
              className="w-full mt-2 py-2 text-xs uppercase tracking-widest"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              ← Back to login
            </button>
          </form>
        )}

        {mode === "sent" && (
          <>
            <div className="px-4 py-5 mb-4" style={{ border: `1px solid ${C.panelBorder}`, background: "rgba(0,0,0,0.4)" }}>
              <p className="text-sm leading-relaxed" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                <span style={{ color: C.redBright }}>{id.trim()}</span> 계정으로 비밀번호 재설정 링크를 보냈습니다.
                링크는 30분간 유효합니다.
              </p>
            </div>
            <p className="text-[11px] mb-4" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
              메일이 오지 않으면 스팸함을 확인해 주세요.
            </p>
            <button
              onClick={() => goto("login")}
              className="w-full py-3 font-bold uppercase text-sm tracking-widest"
              style={{ background: C.red, color: "#fff", fontFamily: "Share Tech Mono", border: `1px solid ${C.redBright}` }}
            >
              로그인으로 돌아가기
            </button>
          </>
        )}
      </div>
    </div>
  );
}

/* ─── 등급 진행 바 ───────────────────────────────────────── */
function TierProgress({ plays }: { plays: number }) {
  const next = nextTier(plays);
  const base = plays < PURPLE_AT ? 0 : PURPLE_AT;
  const target = plays < PURPLE_AT ? PURPLE_AT : RED_AT;
  const pct = next ? ((plays - base) / (target - base)) * 100 : 100;
  const color = TIERS[next ? next.tier : "red"].color;

  return (
    <div className="w-full">
      <div className="flex justify-between items-baseline mb-1.5">
        <span className="text-[10px] uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
          게임 참여 {plays}회
        </span>
        <span className="text-[10px]" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
          {next ? `${TIERS[next.tier].label}까지 ${next.remaining}회` : "최고 등급 달성"}
        </span>
      </div>
      <div className="h-1" style={{ background: "rgba(255,255,255,0.08)" }}>
        <div className="h-full transition-all" style={{ width: `${Math.min(pct, 100)}%`, background: color }} />
      </div>
    </div>
  );
}

/* ─── 참여 코드 등록 ─────────────────────────────────────── */
function CodeModal({
  session, onRedeem, onClose,
}: {
  session: Session;
  onRedeem: (code: string) => void;
  onClose: () => void;
}) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState<null | { plays: number; upgraded: Tier | null }>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;

    const value = code.trim().toUpperCase();
    if (!new RegExp(`^[A-Z0-9]{${CODE_LENGTH}}$`).test(value)) {
      setError(`참여 코드는 영문·숫자 ${CODE_LENGTH}자리입니다.`);
      return;
    }
    if (session.usedCodes.includes(value)) {
      setError("이미 등록된 코드입니다.");
      return;
    }

    setBusy(true);
    setError("");
    /* 서버가 붙으면 여기서 코드의 유효성을 서버에 확인해야 한다.
       발급되지 않은 코드나 남이 쓴 코드는 클라이언트가 알 수 없다. */
    await new Promise((resolve) => setTimeout(resolve, 800));

    const before = tierFor(session.plays);
    const after = tierFor(session.plays + 1);
    onRedeem(value);
    setBusy(false);
    setDone({ plays: session.plays + 1, upgraded: after !== before ? after : null });
  }

  function again() {
    setCode("");
    setError("");
    setDone(null);
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center"
      style={{ background: "rgba(0,0,0,0.85)", backdropFilter: "blur(4px)" }}
      onClick={busy ? undefined : onClose}
    >
      <div
        className="w-full max-w-sm mx-4 p-8"
        style={{
          background: "rgba(12,0,0,0.97)",
          border: `1px solid ${C.panelBorder}`,
          boxShadow: `0 0 60px rgba(200,30,0,0.2)`,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-2xl font-bold uppercase mb-1 tracking-wide" style={{ fontFamily: "Cinzel, serif", color: C.text }}>
          Game Code
        </h2>
        <p className="text-xs mb-6" style={{ color: C.textMuted, fontFamily: "Share Tech Mono, monospace" }}>
          // 서바이벌 게임 참여 쿠폰의 {CODE_LENGTH}자리 코드를 입력하세요
        </p>

        {!done && (
          <>
            <div className="mb-5">
              <TierProgress plays={session.plays} />
            </div>

            <form onSubmit={submit}>
              <input
                autoFocus
                value={code}
                onChange={(e) => { setCode(e.target.value.toUpperCase()); setError(""); }}
                placeholder="A1B2C3D4"
                maxLength={CODE_LENGTH}
                disabled={busy}
                className="w-full px-4 py-3 text-center outline-none mb-4"
                style={{
                  background: "rgba(0,0,0,0.45)",
                  border: `1px solid ${C.panelBorder}`,
                  color: C.text,
                  fontFamily: "Share Tech Mono, monospace",
                  fontSize: 20,
                  letterSpacing: "0.3em",
                }}
              />

              {error && (
                <p className="text-xs mb-4" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={busy}
                className="w-full py-3 font-bold uppercase text-sm tracking-widest transition-all flex items-center justify-center gap-2"
                style={{
                  background: busy ? C.redDim : C.red,
                  color: "#fff",
                  fontFamily: "Share Tech Mono",
                  border: `1px solid ${busy ? C.redDim : C.redBright}`,
                  cursor: busy ? "wait" : "pointer",
                }}
              >
                {busy ? <><Spinner /> 확인 중…</> : "참여 등록 →"}
              </button>
            </form>

            <button
              onClick={onClose}
              disabled={busy}
              className="w-full mt-2 py-2 text-xs uppercase tracking-widest"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              Cancel
            </button>

            <p className="text-[10px] mt-5 leading-relaxed" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
              참여 {PURPLE_AT}회에 Code Purple, {RED_AT}회에 Code Red로 자동 승급됩니다.
              코드 하나는 한 번만 등록할 수 있습니다.
            </p>
          </>
        )}

        {done && (
          <>
            {done.upgraded ? (
              <div
                className="px-5 py-6 mb-4 text-center"
                style={{ border: `1px solid ${TIERS[done.upgraded].color}`, background: `${TIERS[done.upgraded].color}18` }}
              >
                <div
                  className="text-lg font-bold uppercase mb-3"
                  style={{ fontFamily: "Cinzel, serif", color: TIERS[done.upgraded].brightColor }}
                >
                  Tier Up
                </div>
                <div className="flex justify-center mb-3">
                  <TierBadge tier={done.upgraded} />
                </div>
                <p className="text-sm leading-relaxed" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                  참여 {done.plays}회를 달성해 <strong>{TIERS[done.upgraded].label}</strong> 등급으로 올라갔습니다.
                </p>
              </div>
            ) : (
              <div className="px-5 py-6 mb-4" style={{ border: `1px solid ${C.panelBorder}`, background: "rgba(0,0,0,0.4)" }}>
                <p className="text-sm leading-relaxed mb-4" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                  참여가 기록되었습니다. 누적 <strong>{done.plays}회</strong>입니다.
                </p>
                <TierProgress plays={done.plays} />
              </div>
            )}

            <button
              onClick={again}
              className="w-full py-3 font-bold uppercase text-sm tracking-widest"
              style={{ background: C.red, color: "#fff", fontFamily: "Share Tech Mono", border: `1px solid ${C.redBright}` }}
            >
              코드 더 등록하기
            </button>
            <button
              onClick={onClose}
              className="w-full mt-2 py-2 text-xs uppercase tracking-widest"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              Close
            </button>
          </>
        )}
      </div>
    </div>
  );
}

/* ─── product card ───────────────────────────────────────── */
function ProductCard({ p, onOpen }: { p: Product; onOpen: () => void }) {
  const [hov, setHov] = useState(false);
  const tierColor = TIERS[p.tier].color;
  const tierBright = TIERS[p.tier].brightColor;

  const badgeColors: Record<string, string> = {
    HOT: C.red,
    NEW: "#1a7a3a",
    VIP: C.redBright,
  };

  return (
    <div
      onClick={onOpen}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      className="cursor-pointer transition-all relative"
      style={{
        background: hov ? "rgba(25,0,0,0.95)" : "rgba(10,0,0,0.82)",
        border: `1px solid ${hov ? tierColor : C.panelBorder}`,
      }}
    >
      {/* image */}
      <div className="relative overflow-hidden" style={{ background: "#060606", height: 180 }}>
        <img
          src={p.img}
          alt={p.name}
          className="w-full h-full object-cover transition-transform duration-500"
          style={{
            transform: hov ? "scale(1.06)" : "scale(1)",
            filter: `brightness(0.8) saturate(0.65)`,
          }}
        />
        {p.badge && (
          <span
            className="absolute top-2 left-2 text-[9px] font-black uppercase tracking-widest px-1.5 py-0.5"
            style={{ background: badgeColors[p.badge] ?? C.red, color: "#fff", fontFamily: "Share Tech Mono" }}
          >
            {p.badge}
          </span>
        )}
        {/* tier indicator strip */}
        <div className="absolute bottom-0 left-0 right-0 h-0.5" style={{ background: tierColor }} />
      </div>

      {/* info */}
      <div className="px-3 py-3">
        <div className="flex items-center justify-between mb-1.5">
          <span
            className="text-[10px]"
            style={{ color: C.textMuted, fontFamily: "Share Tech Mono", letterSpacing: "0.1em" }}
          >
            item No. {p.id}
          </span>
          <TierBadge tier={p.tier} small />
        </div>
        <div className="text-sm mb-2 leading-snug" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
          {p.name}
        </div>
        <div className="text-base font-bold mb-2" style={{ color: tierBright, fontFamily: "Share Tech Mono" }}>
          {krw(p.price)}
        </div>
        {/* progress */}
        <div className="h-px mb-1" style={{ background: C.panelBorder }}>
          <div className="h-full" style={{ width: `${Math.min(p.funded, 100)}%`, background: tierColor }} />
        </div>
        <div className="flex justify-between text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
          <span>{p.funded}%</span>
          <span>{p.backers} backers</span>
        </div>
      </div>
    </div>
  );
}

/* ─── product detail ─────────────────────────────────────── */
function ProductDetail({
  p, onBack, onAddToCart, onBuyNow,
}: {
  p: Product;
  onBack: () => void;
  onAddToCart: (qty: number) => void;
  onBuyNow: (qty: number) => void;
}) {
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);
  const t = TIERS[p.tier];

  useEffect(() => {
    setQty(1);
    setAdded(false);
    window.scrollTo({ top: 0 });
  }, [p.id]);

  function add() {
    onAddToCart(qty);
    setAdded(true);
    window.setTimeout(() => setAdded(false), 1800);
  }

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-6">
      <button
        onClick={onBack}
        className="text-xs uppercase tracking-widest mb-4"
        style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
      >
        ← 목록으로
      </button>

      <div className="flex flex-col md:flex-row gap-0" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
        {/* 사진 */}
        <div className="md:w-[52%] shrink-0 relative" style={{ background: "#060606" }}>
          <img
            src={p.img}
            alt={p.name}
            className="w-full object-cover"
            style={{ height: 420, filter: "brightness(0.85) saturate(0.7)" }}
          />
          {p.badge && (
            <span
              className="absolute top-3 left-3 text-[10px] font-black uppercase tracking-widest px-2 py-1"
              style={{
                background: p.badge === "NEW" ? "#1a7a3a" : p.badge === "VIP" ? C.redBright : C.red,
                color: "#fff",
                fontFamily: "Share Tech Mono",
              }}
            >
              {p.badge}
            </span>
          )}
          <div className="absolute bottom-0 left-0 right-0" style={{ height: 3, background: t.color }} />
        </div>

        {/* 정보 */}
        <div className="flex-1 p-6 md:p-8">
          <div
            className="flex items-center gap-2 mb-3 text-[10px] uppercase tracking-widest"
            style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
          >
            <span>{p.category}</span>
            <span style={{ color: C.redDim }}>/</span>
            <span>{p.sub}</span>
            <span style={{ color: C.redDim }}>/</span>
            <span>item No. {p.id}</span>
          </div>

          <div className="mb-3">
            <TierBadge tier={p.tier} />
          </div>

          <h1 className="text-2xl leading-snug mb-4" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
            {p.name}
          </h1>

          <div className="text-3xl font-bold mb-5" style={{ color: t.brightColor, fontFamily: "Share Tech Mono" }}>
            {krw(p.price)}
          </div>

          {/* 펀딩 */}
          <div className="mb-6">
            <div className="h-px mb-1.5" style={{ background: C.panelBorder }}>
              <div className="h-full" style={{ width: `${Math.min(p.funded, 100)}%`, background: t.color }} />
            </div>
            <div className="flex justify-between text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
              <span>{p.funded}% 달성</span>
              <span>{p.backers} backers</span>
            </div>
          </div>

          <p className="text-sm leading-relaxed mb-6" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
            {p.desc}
          </p>

          {/* 제원 */}
          <div className="mb-7" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
            {p.specs.map(([label, value]) => (
              <div key={label} className="flex gap-4 py-2.5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                <span
                  className="text-[10px] uppercase tracking-widest shrink-0"
                  style={{ color: C.textMuted, fontFamily: "Share Tech Mono", width: 92, paddingTop: 2 }}
                >
                  {label}
                </span>
                <span className="text-xs" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                  {value}
                </span>
              </div>
            ))}
          </div>

          {/* 수량 + 구매 */}
          <div className="flex items-center gap-4 mb-4">
            <span className="text-[10px] uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
              수량
            </span>
            <QtyStepper qty={qty} onChange={setQty} />
            <span className="text-sm ml-auto" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
              합계 <span style={{ color: t.brightColor }}>{krw(p.price * qty)}</span>
            </span>
          </div>

          <div className="flex gap-3">
            <button
              onClick={add}
              className="flex-1 py-3.5 text-sm font-bold uppercase tracking-widest transition-all"
              style={{
                border: `1px solid ${added ? t.color : C.panelBorder}`,
                color: added ? t.brightColor : C.text,
                background: "rgba(0,0,0,0.4)",
                fontFamily: "Share Tech Mono",
              }}
            >
              {added ? "담았습니다 ✓" : "장바구니에 담기"}
            </button>
            <button
              onClick={() => onBuyNow(qty)}
              className="flex-1 py-3.5 text-sm font-bold uppercase tracking-widest transition-all"
              style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
            >
              결제하기 →
            </button>
          </div>

          <p className="text-[10px] mt-4" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
            BB탄 전용 에어소프트 제품입니다. 만 18세 이상만 구매하실 수 있습니다.
          </p>
        </div>
      </div>
    </div>
  );
}

/* ─── cart ───────────────────────────────────────────────── */
function CartView({
  lines, onQty, onRemove, onContinue, onCheckout,
}: {
  lines: { p: Product; qty: number }[];
  onQty: (id: string, qty: number) => void;
  onRemove: (id: string) => void;
  onContinue: () => void;
  onCheckout: () => void;
}) {
  const itemsTotal = lines.reduce((sum, l) => sum + l.p.price * l.qty, 0);
  const shipping = itemsTotal === 0 || itemsTotal >= FREE_SHIPPING_OVER ? 0 : SHIPPING_FEE;

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
      <PageTitle note={`// ${lines.length}개 품목`}>Cart</PageTitle>

      {lines.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4" style={{ border: `1px dashed ${C.panelBorder}` }}>
          <div className="text-2xl font-bold uppercase" style={{ fontFamily: "Cinzel, serif", color: C.redDim }}>
            Cart Is Empty
          </div>
          <button
            onClick={onContinue}
            className="px-8 py-2.5 text-xs font-bold uppercase tracking-widest"
            style={{ border: `1px solid ${C.panelBorder}`, color: C.textDim, fontFamily: "Share Tech Mono" }}
          >
            쇼핑 계속하기 →
          </button>
        </div>
      ) : (
        <div className="flex flex-col lg:flex-row gap-6 items-start">
          {/* 목록 */}
          <div className="flex-1 w-full" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
            {lines.map(({ p, qty }) => (
              <div key={p.id} className="flex items-center gap-4 p-4" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                <img
                  src={p.img}
                  alt={p.name}
                  className="object-cover shrink-0"
                  style={{ width: 84, height: 64, filter: "brightness(0.8) saturate(0.65)" }}
                />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      {p.id}
                    </span>
                    <TierBadge tier={p.tier} small />
                  </div>
                  <div className="text-sm truncate" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                    {p.name}
                  </div>
                  <div className="text-xs mt-1" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                    {krw(p.price)} / 개
                  </div>
                </div>
                <QtyStepper qty={qty} onChange={(n) => onQty(p.id, n)} />
                <div
                  className="text-sm font-bold text-right shrink-0"
                  style={{ width: 90, color: TIERS[p.tier].brightColor, fontFamily: "Share Tech Mono" }}
                >
                  {krw(p.price * qty)}
                </div>
                <button
                  onClick={() => onRemove(p.id)}
                  className="text-xs px-2 shrink-0"
                  style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
                  aria-label="삭제"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>

          {/* 합계 */}
          <div className="w-full lg:w-80 shrink-0 p-5" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
            <div className="text-[10px] uppercase tracking-widest mb-4" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
              // 주문 요약
            </div>
            <SummaryRow label="상품 합계" value={krw(itemsTotal)} />
            <SummaryRow label="배송비" value={shipping === 0 ? "무료" : krw(shipping)} />
            {shipping > 0 && (
              <p className="text-[10px] mt-1 mb-3" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
                {krw(FREE_SHIPPING_OVER - itemsTotal)} 더 담으면 무료배송
              </p>
            )}
            <div className="flex justify-between items-baseline py-3 mt-2" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
              <span className="text-xs uppercase tracking-widest" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                총 결제금액
              </span>
              <span className="text-xl font-bold" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
                {krw(itemsTotal + shipping)}
              </span>
            </div>
            <button
              onClick={onCheckout}
              className="w-full py-3.5 mt-3 text-sm font-bold uppercase tracking-widest"
              style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
            >
              결제하기 →
            </button>
            <button
              onClick={onContinue}
              className="w-full py-2.5 mt-2 text-xs uppercase tracking-widest"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              쇼핑 계속하기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

/* ─── checkout ───────────────────────────────────────────── */
function CheckoutView({
  lines, userTier, onBack, onDone,
}: {
  lines: { p: Product; qty: number }[];
  userTier: Tier | null;
  onBack: () => void;
  onDone: (orderNo: string, total: number) => void;
}) {
  const [r, setR] = useState<Receiver>({ name: "", phone: "", postcode: "", address: "", detail: "", memo: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const itemsTotal = lines.reduce((sum, l) => sum + l.p.price * l.qty, 0);
  const shipping = itemsTotal >= FREE_SHIPPING_OVER ? 0 : SHIPPING_FEE;
  const total = itemsTotal + shipping;

  const set = (k: keyof Receiver) => (v: string) => setR((prev) => ({ ...prev, [k]: v }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;

    if (!r.name.trim() || !r.phone.trim() || !r.address.trim()) {
      setError("받는 분, 연락처, 주소는 필수 항목입니다.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const { orderNo } = await placeOrder({
        items: lines.map(({ p, qty }) => ({ id: p.id, name: p.name, price: p.price, qty })),
        itemsTotal,
        shipping,
        total,
        receiver: r,
        memberTier: userTier,
      });
      onDone(orderNo, total);
    } catch {
      setBusy(false);
      setError("주문을 접수하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }
  }

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
      <button
        onClick={onBack}
        className="text-xs uppercase tracking-widest mb-4"
        style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
      >
        ← 장바구니로
      </button>

      <PageTitle note="// 배송 정보를 입력해 주세요">Checkout</PageTitle>

      <form onSubmit={submit} className="flex flex-col lg:flex-row gap-6 items-start">
        {/* 배송 정보 */}
        <div className="flex-1 w-full p-6" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
          <div className="text-[10px] uppercase tracking-widest mb-5" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
            // 배송지
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <Field label="받는 분 *" value={r.name} onChange={set("name")} placeholder="홍길동" />
            <Field label="연락처 *" value={r.phone} onChange={set("phone")} placeholder="010-0000-0000" />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <Field label="우편번호" value={r.postcode} onChange={set("postcode")} placeholder="00000" />
            <div className="md:col-span-2">
              <Field label="주소 *" value={r.address} onChange={set("address")} placeholder="시/도 구/군 도로명" />
            </div>
          </div>

          <div className="mb-4">
            <Field label="상세 주소" value={r.detail} onChange={set("detail")} placeholder="동 · 호수" />
          </div>

          <Field label="배송 요청사항" value={r.memo} onChange={set("memo")} placeholder="부재 시 경비실에 맡겨주세요" />

          <div className="mt-6 pt-5 flex items-start gap-3" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
            <span
              className="text-[10px] uppercase tracking-widest shrink-0"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono", paddingTop: 2 }}
            >
              연령 확인
            </span>
            <span className="text-xs leading-relaxed" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
              BB탄 전용 에어소프트 제품으로, 만 18세 이상만 구매하실 수 있습니다. 주문 시 연령 확인에 동의한 것으로 봅니다.
            </span>
          </div>
        </div>

        {/* 주문 요약 */}
        <div className="w-full lg:w-80 shrink-0 p-5" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
          <div className="text-[10px] uppercase tracking-widest mb-4" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
            // 주문 요약
          </div>

          <div className="mb-4" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
            {lines.map(({ p, qty }) => (
              <div key={p.id} className="flex justify-between items-baseline gap-3 py-1.5">
                <span className="text-xs truncate" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>
                  {p.name} <span style={{ color: C.textMuted }}>× {qty}</span>
                </span>
                <span className="text-xs shrink-0" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
                  {krw(p.price * qty)}
                </span>
              </div>
            ))}
          </div>

          <SummaryRow label="상품 합계" value={krw(itemsTotal)} />
          <SummaryRow label="배송비" value={shipping === 0 ? "무료" : krw(shipping)} />

          <div className="flex justify-between items-baseline py-3 mt-2" style={{ borderTop: `1px solid ${C.panelBorder}` }}>
            <span className="text-xs uppercase tracking-widest" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
              총 결제금액
            </span>
            <span className="text-xl font-bold" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
              {krw(total)}
            </span>
          </div>

          {error && (
            <p className="text-xs mb-3" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="w-full py-3.5 mt-2 text-sm font-bold uppercase tracking-widest flex items-center justify-center gap-2"
            style={{
              background: busy ? C.redDim : C.red,
              color: "#fff",
              border: `1px solid ${busy ? C.redDim : C.redBright}`,
              fontFamily: "Share Tech Mono",
              cursor: busy ? "wait" : "pointer",
            }}
          >
            {busy ? <><Spinner /> 결제 처리 중…</> : `${krw(total)} 결제하기 →`}
          </button>
        </div>
      </form>
    </div>
  );
}

/* ─── order complete ─────────────────────────────────────── */
function OrderDone({ orderNo, total, onHome }: { orderNo: string; total: number; onHome: () => void }) {
  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-16">
      <div className="max-w-md mx-auto p-10 text-center" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
        <div className="text-2xl font-bold uppercase mb-3" style={{ fontFamily: "Cinzel, serif", color: C.text }}>
          Order Complete
        </div>
        <p className="text-sm mb-8" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
          주문이 정상적으로 접수되었습니다.
        </p>

        <div className="py-4 mb-6" style={{ borderTop: `1px solid ${C.panelBorder}`, borderBottom: `1px solid ${C.panelBorder}` }}>
          <SummaryRow label="주문번호" value={orderNo} />
          <SummaryRow label="결제금액" value={krw(total)} />
        </div>

        <button
          onClick={onHome}
          className="w-full py-3 text-sm font-bold uppercase tracking-widest"
          style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
        >
          쇼핑 계속하기 →
        </button>
      </div>
    </div>
  );
}

/* ─── sidebar ────────────────────────────────────────────── */
function Sidebar({ category, activeSub, onSub }: { category: string; activeSub: string; onSub: (s: string) => void }) {
  const subs = SUBCATS[category] ?? [];
  return (
    <aside className="w-48 shrink-0 hidden md:block" style={{ borderRight: `1px solid ${C.panelBorder}` }}>
      <h2
        className="px-4 pt-5 pb-3 font-bold uppercase"
        style={{
          fontFamily: "Cinzel, serif",
          color: C.redBright,
          fontSize: "clamp(22px,3vw,38px)",
          borderBottom: `1px solid ${C.panelBorder}`,
        }}
      >
        {category}
      </h2>
      <ul className="px-4 py-4 space-y-1">
        {subs.map((s) => {
          const isAll = s === "전체";
          const isActive = s === activeSub;
          return (
            <li key={s}>
              <button
                onClick={() => onSub(s)}
                className="text-left w-full transition-colors py-0.5"
                style={{
                  fontFamily: "Noto Sans KR, sans-serif",
                  fontSize: isAll ? 13 : 12,
                  fontWeight: isActive ? 700 : isAll ? 500 : 400,
                  color: isActive ? C.redBright : isAll ? C.text : C.textDim,
                  paddingLeft: isAll ? 0 : 10,
                }}
                onMouseEnter={(e) => {
                  if (!isActive) (e.currentTarget as HTMLButtonElement).style.color = C.text;
                }}
                onMouseLeave={(e) => {
                  if (!isActive) (e.currentTarget as HTMLButtonElement).style.color = isAll ? C.text : C.textDim;
                }}
              >
                {!isAll && <span style={{ color: C.redDim, marginRight: 4 }}>·</span>}
                {s}
              </button>
            </li>
          );
        })}
      </ul>
    </aside>
  );
}

/* ─── code tab ───────────────────────────────────────────── */
function CodeTab({
  tier, active, userTier, onClick,
}: {
  tier: Tier; active: boolean; userTier: Tier | null; onClick: () => void;
}) {
  const t = TIERS[tier];
  const owned = userTier === tier;
  return (
    <button
      onClick={onClick}
      className="flex-1 flex flex-col items-center gap-1.5 py-4 transition-all relative"
      style={{
        background: active ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.28)",
        borderBottom: active ? `2px solid ${t.color}` : "2px solid transparent",
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

/* ─── main app ───────────────────────────────────────────── */
export default function App() {
  const [session, setSession] = useState<Session | null>(() => read<Session | null>(SESSION_KEY, null));
  const [cart, setCart] = useState<CartLine[]>(() => read<CartLine[]>(CART_KEY, []));
  const [view, setView] = useState<View>({ name: "list" });
  const [showLogin, setShowLogin] = useState(false);
  const [showCode, setShowCode] = useState(false);
  const [afterLogin, setAfterLogin] = useState<null | "checkout">(null);
  const [activeCodeTab, setActiveCodeTab] = useState<Tier>(() => {
    const saved = read<Session | null>(SESSION_KEY, null);
    return saved ? tierFor(saved.plays) : "red";
  });

  /* 등급은 저장된 값이 아니라 참여 횟수에서 계산한다 */
  const userTier: Tier | null = session ? tierFor(session.plays) : null;
  const [activeNav, setActiveNav] = useState("Guns");
  const [activeSub, setActiveSub] = useState("전체");
  const [menuOpen, setMenuOpen] = useState(false);

  /* 장바구니는 로그인 상태 유지 여부와 무관하게 이 브라우저에 남는다 */
  useEffect(() => {
    write(CART_KEY, cart);
  }, [cart]);

  /* 참여 횟수가 늘어나도 같은 자리에서 저장된다 */
  useEffect(() => {
    if (session?.remember) write(SESSION_KEY, session);
    else drop(SESSION_KEY);
  }, [session]);

  function handleLogin(id: string, plays: number, remember: boolean) {
    setSession({ id, plays, usedCodes: [], remember });
    setActiveCodeTab(tierFor(plays));
    setShowLogin(false);

    if (afterLogin === "checkout") {
      setAfterLogin(null);
      setView({ name: "checkout" });
    }
  }

  function handleLogout() {
    setSession(null);
    setActiveCodeTab("red");
  }

  /* 참여 코드 등록 — 승급하면 해당 등급 탭으로 옮겨 준다 */
  function redeemCode(code: string) {
    if (!session) return;
    const plays = session.plays + 1;
    setSession({ ...session, plays, usedCodes: [...session.usedCodes, code] });
    if (tierFor(plays) !== tierFor(session.plays)) setActiveCodeTab(tierFor(plays));
  }

  function changeNav(cat: string) {
    setActiveNav(cat);
    setActiveSub("전체");
    setMenuOpen(false);
    setView({ name: "list" });
  }

  /* ── 장바구니 ── */
  const cartLines = cart
    .map((line) => {
      const p = PRODUCTS.find((item) => item.id === line.id);
      return p ? { p, qty: line.qty } : null;
    })
    .filter((l): l is { p: Product; qty: number } => l !== null);

  const cartCount = cart.reduce((sum, l) => sum + l.qty, 0);

  function addToCart(id: string, qty: number) {
    setCart((prev) => {
      const found = prev.find((l) => l.id === id);
      if (found) return prev.map((l) => (l.id === id ? { ...l, qty: Math.min(99, l.qty + qty) } : l));
      return [...prev, { id, qty }];
    });
  }

  function setQty(id: string, qty: number) {
    setCart((prev) => prev.map((l) => (l.id === id ? { ...l, qty } : l)));
  }

  function removeLine(id: string) {
    setCart((prev) => prev.filter((l) => l.id !== id));
  }

  /* 결제는 로그인이 필요하다 */
  function goCheckout() {
    if (cartLines.length === 0) return;
    if (!userTier) {
      setAfterLogin("checkout");
      setShowLogin(true);
      return;
    }
    setView({ name: "checkout" });
  }

  function buyNow(id: string, qty: number) {
    addToCart(id, qty);
    if (!userTier) {
      setAfterLogin("checkout");
      setShowLogin(true);
      return;
    }
    setView({ name: "checkout" });
  }

  function finishOrder(orderNo: string, total: number) {
    setCart([]);
    setView({ name: "done", orderNo, total });
    window.scrollTo({ top: 0 });
  }

  /* filter products: category + sub + code tab tier */
  const filtered = PRODUCTS.filter((p) => {
    if (p.category !== activeNav) return false;
    if (activeSub !== "전체" && p.sub !== activeSub) return false;
    if (p.tier !== activeCodeTab) return false;
    return true;
  });

  const detailProduct = view.name === "detail" ? PRODUCTS.find((p) => p.id === view.id) ?? null : null;
  const onListPage = view.name === "list";

  return (
    <div className="min-h-full" style={{ background: C.bg, color: C.text, fontFamily: "Noto Sans KR, sans-serif", minHeight: "100vh" }}>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>

      {showLogin && (
        <LoginModal onLogin={handleLogin} onClose={() => { setShowLogin(false); setAfterLogin(null); }} />
      )}

      {showCode && session && (
        <CodeModal session={session} onRedeem={redeemCode} onClose={() => setShowCode(false)} />
      )}

      {/* ── HEADER ─────────────────────────────────── */}
      <header
        className="sticky top-0 z-50"
        style={{ background: "rgba(12,0,0,0.93)", backdropFilter: "blur(8px)", borderBottom: `1px solid ${C.panelBorder}` }}
      >
        <div className="max-w-[1280px] mx-auto px-4 md:px-8">
          <div className="flex items-center justify-between h-14">
            {/* Logo */}
            <button className="flex items-center gap-3" onClick={() => setView({ name: "list" })}>
              <div className="w-7 h-7 flex items-center justify-center text-[10px] font-black"
                style={{ background: C.red, color: "#fff", fontFamily: "Cinzel, serif" }}>
                MH
              </div>
              <span className="font-bold uppercase tracking-wider text-base" style={{ fontFamily: "Cinzel, serif" }}>
                Murder<span style={{ color: C.redBright }}>Help</span>
              </span>
            </button>

            {/* Desktop nav */}
            <nav className="hidden md:flex items-center h-full">
              {NAV_ITEMS.map((item, i) => {
                const active = onListPage && activeNav === item;
                return (
                  <button
                    key={item}
                    onClick={() => changeNav(item)}
                    className="px-4 h-14 text-sm font-semibold uppercase tracking-widest transition-colors relative"
                    style={{
                      fontFamily: "Cinzel, serif",
                      fontSize: 12,
                      color: active ? C.text : C.textDim,
                      borderBottom: active ? `2px solid ${C.redBright}` : "2px solid transparent",
                    }}
                    onMouseEnter={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.color = C.text; }}
                    onMouseLeave={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.color = C.textDim; }}
                  >
                    {item}
                    {i < NAV_ITEMS.length - 1 && (
                      <span className="absolute right-0 top-1/2 -translate-y-1/2 text-xs" style={{ color: C.redDim }}>|</span>
                    )}
                  </button>
                );
              })}
            </nav>

            {/* Right actions */}
            <div className="flex items-center gap-3">
              {/* search */}
              <div className="hidden md:flex items-center gap-2 px-3 py-1.5 text-xs"
                style={{ background: "rgba(0,0,0,0.5)", border: `1px solid ${C.panelBorder}` }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke={C.textMuted} strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input placeholder="검색..." className="bg-transparent outline-none w-20 text-xs"
                  style={{ color: C.textDim, fontFamily: "Noto Sans KR" }} />
              </div>

              {/* cart */}
              <button
                onClick={() => setView({ name: "cart" })}
                className="relative flex items-center justify-center transition-all"
                style={{ width: 34, height: 30, border: `1px solid ${C.panelBorder}`, color: C.textDim }}
                aria-label="장바구니"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                  <circle cx="9" cy="20" r="1.4" /><circle cx="18" cy="20" r="1.4" />
                  <path d="M2 3h3l2.4 12.2a1.5 1.5 0 0 0 1.5 1.2h8.6a1.5 1.5 0 0 0 1.5-1.2L21 7H6" />
                </svg>
                {cartCount > 0 && (
                  <span
                    className="absolute -top-1.5 -right-1.5 text-[9px] font-bold flex items-center justify-center"
                    style={{
                      minWidth: 16, height: 16, padding: "0 3px",
                      background: C.red, color: "#fff", fontFamily: "Share Tech Mono",
                    }}
                  >
                    {cartCount}
                  </span>
                )}
              </button>

              {/* auth */}
              {userTier ? (
                <div className="flex items-center gap-2">
                  <TierBadge tier={userTier} />
                  <button
                    onClick={handleLogout}
                    className="text-xs px-3 py-1.5 uppercase tracking-wider transition-all"
                    style={{ border: `1px solid ${C.panelBorder}`, color: C.textMuted, fontFamily: "Share Tech Mono" }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.color = C.text; (e.currentTarget as HTMLButtonElement).style.borderColor = C.red; }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.color = C.textMuted; (e.currentTarget as HTMLButtonElement).style.borderColor = C.panelBorder; }}
                  >
                    Logout
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setShowLogin(true)}
                  className="text-xs font-bold px-4 py-1.5 uppercase tracking-wider transition-all"
                  style={{ background: C.red, color: "#fff", fontFamily: "Share Tech Mono" }}
                  onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = C.redBright; }}
                  onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = C.red; }}
                >
                  Login
                </button>
              )}

              {/* mobile menu */}
              <button className="md:hidden" style={{ color: C.textDim }} onClick={() => setMenuOpen(!menuOpen)}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        {menuOpen && (
          <div style={{ background: "rgba(12,0,0,0.98)", borderTop: `1px solid ${C.panelBorder}` }}>
            {NAV_ITEMS.map((item) => (
              <button key={item} onClick={() => changeNav(item)}
                className="block w-full text-left px-6 py-3 text-sm uppercase tracking-widest"
                style={{ fontFamily: "Cinzel, serif", color: activeNav === item ? C.redBright : C.textDim, borderBottom: `1px solid ${C.panelBorder}` }}>
                {item}
              </button>
            ))}
          </div>
        )}
      </header>

      {onListPage && (
        <>
          {/* ── CODE TABS ──────────────────────────────── */}
          <div className="flex" style={{ background: "rgba(0,0,0,0.55)", borderBottom: `1px solid ${C.panelBorder}` }}>
            {(["red", "purple", "yellow"] as Tier[]).map((tier) => (
              <CodeTab
                key={tier}
                tier={tier}
                active={activeCodeTab === tier}
                userTier={userTier}
                onClick={() => setActiveCodeTab(tier)}
              />
            ))}
          </div>

          {/* ── TIER INFO BANNER ────────────────────────── */}
          {!userTier && (
            <div
              className="py-3 px-6 flex items-center justify-between gap-4"
              style={{ background: "rgba(80,0,0,0.35)", borderBottom: `1px solid ${C.panelBorder}` }}
            >
              <div className="flex items-center gap-3">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={C.redBright} strokeWidth="2">
                  <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                  로그인하시면 회원님의 등급이 자동으로 적용됩니다.
                  &nbsp;·&nbsp; Code Red = VIP, Code Purple = 중급, Code Yellow = 엔트리
                </span>
              </div>
            </div>
          )}

          {session && userTier && (
            <div
              className="py-2.5 px-6 flex items-center gap-4 flex-wrap"
              style={{
                background: `${TIERS[userTier].color}18`,
                borderBottom: `1px solid ${TIERS[userTier].color}44`,
              }}
            >
              <TierBadge tier={userTier} />
              <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                {TIERS[userTier].desc}
              </span>
              <div className="flex-1" style={{ minWidth: 220, maxWidth: 420 }}>
                <TierProgress plays={session.plays} />
              </div>
              <button
                onClick={() => setShowCode(true)}
                className="shrink-0 text-xs font-bold px-3 py-1.5 uppercase tracking-wider transition-all"
                style={{
                  border: `1px solid ${C.redDim}`,
                  background: "rgba(0,0,0,0.4)",
                  color: C.text,
                  fontFamily: "Share Tech Mono",
                }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.borderColor = C.redBright; }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.borderColor = C.redDim; }}
              >
                게임 코드 등록 →
              </button>
            </div>
          )}

          {/* ── HERO ────────────────────────────────────── */}
          <div className="relative overflow-hidden" style={{ minHeight: 180 }}>
            <div
              className="absolute inset-0"
              style={{
                backgroundImage: `url(https://images.unsplash.com/photo-1687349150019-003d3ea38d79?w=1400&h=250&fit=crop&auto=format)`,
                backgroundSize: "cover",
                backgroundPosition: "center 30%",
                filter: "brightness(0.18) saturate(0.3)",
              }}
            />
            <div className="absolute inset-0" style={{ background: "linear-gradient(to right, rgba(15,0,0,0.95) 0%, transparent 50%, rgba(15,0,0,0.95) 100%)" }} />
            <div className="relative max-w-[1280px] mx-auto px-4 md:px-8 py-10">
              <div className="text-xs uppercase tracking-[0.3em] mb-2" style={{ color: TIERS[activeCodeTab].color, fontFamily: "Share Tech Mono" }}>
                // {TIERS[activeCodeTab].label.toUpperCase()} — {TIERS[activeCodeTab].desc}
              </div>
              <h1
                className="font-bold uppercase leading-none mb-2"
                style={{
                  fontFamily: "Cinzel, serif",
                  fontSize: "clamp(22px,4vw,48px)",
                  color: C.text,
                  textShadow: `0 0 30px ${TIERS[activeCodeTab].color}55`,
                }}
              >
                {activeCodeTab === "red" && "VIP PREMIUM COLLECTION"}
                {activeCodeTab === "purple" && "MID-TIER TACTICAL GEAR"}
                {activeCodeTab === "yellow" && "ENTRY GRADE ARSENAL"}
              </h1>
              <p className="text-sm" style={{ color: C.textDim, fontFamily: "Noto Sans KR" }}>
                {TIERS[activeCodeTab].priceRange} 범위 · {activeNav} 카테고리
              </p>
            </div>
          </div>

          {/* ── MAIN ────────────────────────────────────── */}
          <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-6">
            <div className="flex gap-0" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
              <Sidebar category={activeNav} activeSub={activeSub} onSub={setActiveSub} />

              <div className="flex-1 p-5">
                {/* mobile subcats */}
                <div className="flex md:hidden gap-2 flex-wrap mb-4">
                  {(SUBCATS[activeNav] ?? []).map((s) => (
                    <button key={s} onClick={() => setActiveSub(s)}
                      className="text-[10px] uppercase tracking-widest px-2.5 py-1 transition-all"
                      style={{
                        fontFamily: "Share Tech Mono",
                        background: activeSub === s ? C.red : "rgba(0,0,0,0.5)",
                        color: activeSub === s ? "#fff" : C.textMuted,
                        border: `1px solid ${activeSub === s ? C.red : C.panelBorder}`,
                      }}
                    >
                      {s}
                    </button>
                  ))}
                </div>

                {/* sort row */}
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <span className="text-xs uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      {activeSub === "전체" ? activeNav : activeSub}
                    </span>
                    <TierBadge tier={activeCodeTab} small />
                    <span
                      className="text-xs px-1.5 py-0.5"
                      style={{ background: "rgba(200,30,0,0.12)", color: C.red, fontFamily: "Share Tech Mono", border: `1px solid ${C.redDim}` }}
                    >
                      {filtered.length}
                    </span>
                  </div>
                  <select
                    className="text-[10px] uppercase tracking-wider px-2 py-1 outline-none"
                    style={{ background: "rgba(0,0,0,0.5)", color: C.textMuted, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
                  >
                    <option>POPULAR</option>
                    <option>PRICE ↑</option>
                    <option>PRICE ↓</option>
                    <option>NEWEST</option>
                  </select>
                </div>

                {/* grid */}
                {filtered.length > 0 ? (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                    {filtered.map((p) => (
                      <ProductCard key={p.id} p={p} onOpen={() => setView({ name: "detail", id: p.id })} />
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-16" style={{ border: `1px dashed ${C.panelBorder}` }}>
                    <div className="text-3xl font-bold uppercase mb-2" style={{ fontFamily: "Cinzel, serif", color: C.redDim }}>
                      NO ITEMS
                    </div>
                    <div className="text-xs" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      // {activeSub} — {TIERS[activeCodeTab].label} 등급 아이템 없음
                    </div>
                  </div>
                )}

                {filtered.length > 0 && (
                  <div className="text-center mt-8">
                    <button
                      className="px-10 py-2.5 text-xs font-bold uppercase tracking-widest transition-all"
                      style={{ border: `1px solid ${C.panelBorder}`, color: C.textDim, fontFamily: "Share Tech Mono" }}
                      onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.borderColor = C.red; (e.currentTarget as HTMLButtonElement).style.color = C.text; }}
                      onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.borderColor = C.panelBorder; (e.currentTarget as HTMLButtonElement).style.color = C.textDim; }}
                    >
                      더 보기 →
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      {view.name === "detail" && detailProduct && (
        <ProductDetail
          p={detailProduct}
          onBack={() => setView({ name: "list" })}
          onAddToCart={(qty) => addToCart(detailProduct.id, qty)}
          onBuyNow={(qty) => buyNow(detailProduct.id, qty)}
        />
      )}

      {view.name === "cart" && (
        <CartView
          lines={cartLines}
          onQty={setQty}
          onRemove={removeLine}
          onContinue={() => setView({ name: "list" })}
          onCheckout={goCheckout}
        />
      )}

      {view.name === "checkout" && (
        <CheckoutView
          lines={cartLines}
          userTier={userTier}
          onBack={() => setView({ name: "cart" })}
          onDone={finishOrder}
        />
      )}

      {view.name === "done" && (
        <OrderDone orderNo={view.orderNo} total={view.total} onHome={() => setView({ name: "list" })} />
      )}

      {/* ── FOOTER ──────────────────────────────────── */}
      <footer className="mt-6 border-t" style={{ borderColor: C.panelBorder, background: "rgba(0,0,0,0.7)" }}>
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-10">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
            {[
              { h: "SHOP", links: ["Guns", "Weapons", "Bombs", "Gear", "Ammo"] },
              { h: "SUPPORT", links: ["주문 조회", "반품/교환", "보증", "고객센터", "FAQ"] },
              { h: "MEMBERSHIP", links: ["Code Red", "Code Purple", "Code Yellow", "등급 안내", "혜택 비교"] },
              { h: "COMPANY", links: ["About", "Blog", "Careers", "Legal"] },
            ].map((col) => (
              <div key={col.h}>
                <div className="text-[10px] uppercase tracking-[0.2em] mb-3 font-semibold"
                  style={{ color: C.red, fontFamily: "Share Tech Mono" }}>
                  {col.h}
                </div>
                <ul className="space-y-1.5">
                  {col.links.map((l) => (
                    <li key={l}>
                      <a href="#" className="text-xs transition-colors"
                        style={{ color: C.textMuted, fontFamily: "Noto Sans KR" }}
                        onMouseEnter={(e) => { (e.currentTarget as HTMLAnchorElement).style.color = C.text; }}
                        onMouseLeave={(e) => { (e.currentTarget as HTMLAnchorElement).style.color = C.textMuted; }}>
                        {l}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          <div
            className="flex flex-col md:flex-row items-start md:items-center justify-between gap-2 pt-6 border-t text-[10px]"
            style={{ borderColor: C.panelBorder, color: C.textMuted, fontFamily: "Share Tech Mono" }}
          >
            <span>
              <span style={{ color: C.text, fontFamily: "Cinzel, serif", fontSize: 13 }}>MurderHelp</span>
              {" "}© 2026 All rights reserved.
            </span>
            <div className="flex items-center gap-3">
              <span>BB탄 전용 · 만 18세 이상</span>
              <span className="px-2 py-0.5" style={{ border: `1px solid ${C.panelBorder}`, color: C.red }}>
                AIRSOFT ONLY
              </span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
