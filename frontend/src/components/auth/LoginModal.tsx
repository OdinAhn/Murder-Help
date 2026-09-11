import { useState, type FormEvent } from "react";
import { ACCOUNTS, MIN_ID, MIN_PW } from "../../lib/accounts";
import { C, krw } from "../../lib/theme";
import { TIERS, tierFor } from "../../lib/tier";
import { Spinner } from "../common/Spinner";

/* ─── login modal ────────────────────────────────────────── */
export function LoginModal({ onLogin, onClose }: { onLogin: (id: string, spent: number) => void; onClose: () => void }) {
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [id, setId] = useState("");
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [isAdult, setIsAdult] = useState(false);
  const [agreed, setAgreed] = useState(false);
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
    onLogin(key, account.spent);
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

    ACCOUNTS[account] = { pw, spent: 0 };
    onLogin(account, 0);
  }

  function goto(next: "login" | "signup") {
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
        </h2>
        <p className="text-xs mb-6" style={{ color: C.textMuted, fontFamily: "Share Tech Mono, monospace" }}>
          {mode === "login" && "// 로그인하시면 회원님의 등급이 자동으로 적용됩니다"}
          {mode === "signup" && "// 신규 회원은 Code Yellow 등급으로 시작합니다"}
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
                  const spent = ACCOUNTS[key].spent;
                  return (
                    <div key={key} className="flex items-center justify-between text-[11px]" style={{ fontFamily: "Share Tech Mono" }}>
                      <span style={{ color: C.textDim }}>{key}</span>
                      <span style={{ color: C.textMuted }}>{krw(spent)}</span>
                      <span style={{ color: TIERS[tierFor(spent, key)].brightColor }}>{TIERS[tierFor(spent, key)].label}</span>
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
      </div>
    </div>
  );
}
