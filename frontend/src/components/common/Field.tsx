import { C } from "../../lib/theme";

export function Field({
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
