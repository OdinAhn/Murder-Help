import { useState, type FormEvent } from "react";
import { login, signup, type Member } from "../../api/auth";
import { C } from "../../lib/theme";
import { Spinner } from "../common/Spinner";

type LoginModalProps = {
  // 기존 App.tsx의 handleLogin(id, spent)과 호환되도록 유지
  onLogin: (id: string, spent: number) => void;
  onClose: () => void;
};

function spentFromGrade(grade: Member["grade"]) {
  switch (grade) {
    case "GREEN":
      return 1_000_000;
    case "RED":
      return 800_000;
    case "PURPLE":
      return 200_000;
    case "YELLOW":
    default:
      return 0;
  }
}

export function LoginModal({ onLogin, onClose }: LoginModalProps) {
  const [mode, setMode] = useState<"login" | "signup">("login");

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [phone, setPhone] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const fieldStyle = {
    background: "rgba(0,0,0,0.45)",
    border: `1px solid ${C.panelBorder}`,
    color: C.text,
    fontFamily: "Noto Sans KR, sans-serif",
  };

  function moveTo(nextMode: "login" | "signup") {
    setMode(nextMode);
    setError("");
    setPassword("");
    setPasswordConfirm("");
  }

  function completeLogin(member: Member) {
    onLogin(String(member.id), spentFromGrade(member.grade));
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault();
    if (busy) return;

    setBusy(true);
    setError("");

    try {
      const member = await login(email.trim().toLowerCase(), password);
      completeLogin(member);
    } catch (error) {
      setError(error instanceof Error ? error.message : "로그인에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  async function handleSignup(event: FormEvent) {
    event.preventDefault();
    if (busy) return;

    const normalizedName = name.trim();
    const normalizedEmail = email.trim().toLowerCase();
    const normalizedPhone = phone.trim();

    if (!normalizedName) {
      setError("이름을 입력해 주세요.");
      return;
    }

    if (!normalizedEmail) {
      setError("이메일을 입력해 주세요.");
      return;
    }

    if (password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    if (!/^010-\d{4}-\d{4}$/.test(normalizedPhone)) {
      setError("전화번호는 010-1234-5678 형식으로 입력해 주세요.");
      return;
    }

    setBusy(true);
    setError("");

    try {
      await signup({
        name: normalizedName,
        email: normalizedEmail,
        password,
        phone: normalizedPhone,
      });

      // 가입 성공 후 같은 계정으로 실제 서버 로그인
      const member = await login(normalizedEmail, password);
      completeLogin(member);
    } catch (error) {
      setError(error instanceof Error ? error.message : "회원가입에 실패했습니다.");
    } finally {
      setBusy(false);
    }
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
              boxShadow: "0 0 60px rgba(200,30,0,0.2)",
            }}
            onClick={(event) => event.stopPropagation()}
        >
          <h2
              className="text-2xl font-bold uppercase mb-1 tracking-wide"
              style={{ fontFamily: "Cinzel, serif", color: C.text }}
          >
            {mode === "login" ? "Member Login" : "Join MurderHelp"}
          </h2>

          <p
              className="text-xs mb-6"
              style={{ color: C.textMuted, fontFamily: "Share Tech Mono, monospace" }}
          >
            {mode === "login"
                ? "// 이메일과 비밀번호로 로그인합니다"
                : "// 가입 후 자동으로 로그인됩니다"}
          </p>

          {mode === "login" && (
              <>
                <form onSubmit={handleLogin}>
                  <div className="space-y-3 mb-4">
                    <input
                        autoFocus
                        type="email"
                        value={email}
                        onChange={(event) => {
                          setEmail(event.target.value);
                          setError("");
                        }}
                        placeholder="이메일"
                        disabled={busy}
                        className="w-full px-4 py-3 text-sm outline-none"
                        style={fieldStyle}
                    />

                    <input
                        type="password"
                        value={password}
                        onChange={(event) => {
                          setPassword(event.target.value);
                          setError("");
                        }}
                        placeholder="비밀번호"
                        disabled={busy}
                        className="w-full px-4 py-3 text-sm outline-none"
                        style={fieldStyle}
                    />
                  </div>

                  {error && (
                      <p
                          className="text-xs mb-4"
                          style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}
                      >
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
                    {busy ? <><Spinner /> 로그인 중…</> : "Login →"}
                  </button>
                </form>

                <button
                    type="button"
                    onClick={onClose}
                    disabled={busy}
                    className="w-full mt-2 py-2 text-xs uppercase tracking-widest transition-all"
                    style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
                >
                  Cancel
                </button>

                <div
                    className="mt-5 pt-4 flex items-center justify-center gap-2"
                    style={{ borderTop: `1px solid ${C.panelBorder}` }}
                >
              <span
                  className="text-[11px]"
                  style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}
              >
                계정이 없으신가요?
              </span>

                  <button
                      type="button"
                      onClick={() => moveTo("signup")}
                      disabled={busy}
                      className="text-[11px] font-bold"
                      style={{
                        color: C.redBright,
                        fontFamily: "Noto Sans KR, sans-serif",
                        textDecoration: "underline",
                      }}
                  >
                    회원가입
                  </button>
                </div>
              </>
          )}

          {mode === "signup" && (
              <form onSubmit={handleSignup}>
                <div className="space-y-3 mb-4">
                  <input
                      autoFocus
                      value={name}
                      onChange={(event) => {
                        setName(event.target.value);
                        setError("");
                      }}
                      placeholder="이름"
                      disabled={busy}
                      className="w-full px-4 py-3 text-sm outline-none"
                      style={fieldStyle}
                  />

                  <input
                      type="email"
                      value={email}
                      onChange={(event) => {
                        setEmail(event.target.value);
                        setError("");
                      }}
                      placeholder="이메일"
                      disabled={busy}
                      className="w-full px-4 py-3 text-sm outline-none"
                      style={fieldStyle}
                  />

                  <input
                      type="password"
                      value={password}
                      onChange={(event) => {
                        setPassword(event.target.value);
                        setError("");
                      }}
                      placeholder="비밀번호 (8자 이상)"
                      disabled={busy}
                      className="w-full px-4 py-3 text-sm outline-none"
                      style={fieldStyle}
                  />

                  <input
                      type="password"
                      value={passwordConfirm}
                      onChange={(event) => {
                        setPasswordConfirm(event.target.value);
                        setError("");
                      }}
                      placeholder="비밀번호 확인"
                      disabled={busy}
                      className="w-full px-4 py-3 text-sm outline-none"
                      style={fieldStyle}
                  />

                  <input
                      value={phone}
                      onChange={(event) => {
                        setPhone(event.target.value);
                        setError("");
                      }}
                      placeholder="전화번호 (010-1234-5678)"
                      disabled={busy}
                      className="w-full px-4 py-3 text-sm outline-none"
                      style={fieldStyle}
                  />
                </div>

                {error && (
                    <p
                        className="text-xs mb-4"
                        style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}
                    >
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
                    onClick={() => moveTo("login")}
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