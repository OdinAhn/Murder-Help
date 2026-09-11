import { C } from "../../lib/theme";

export function QtyStepper({ qty, onChange }: { qty: number; onChange: (n: number) => void }) {
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
