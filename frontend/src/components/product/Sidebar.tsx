import { SUBCATS } from "../../catalog";
import { C } from "../../lib/theme";

export function Sidebar({ category, activeSub, onSub }: { category: string; activeSub: string; onSub: (s: string) => void }) {
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
