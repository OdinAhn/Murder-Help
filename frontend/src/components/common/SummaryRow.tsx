import { C } from "../../lib/theme";

export function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between items-baseline py-1.5">
      <span className="text-xs" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif" }}>{label}</span>
      <span className="text-sm" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>{value}</span>
    </div>
  );
}
