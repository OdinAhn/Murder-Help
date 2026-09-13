import { C } from "../../lib/theme";

/* max — 재고보다 많이 담지 못하게 + 버튼을 막는다 */
export function QtyStepper({ qty, onChange, max = 99 }: { qty: number; onChange: (n: number) => void; max?: number }) {
  const atMax = qty >= max;
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
      <button
        type="button"
        onClick={() => onChange(qty + 1)}
        disabled={atMax}
        style={{ ...btn, opacity: atMax ? 0.35 : 1, cursor: atMax ? "not-allowed" : "pointer" }}
        aria-label="수량 늘리기"
      >
        +
      </button>
    </div>
  );
}
