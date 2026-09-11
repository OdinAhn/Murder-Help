import type { ReactNode } from "react";
import { C } from "../../lib/theme";

export function PageTitle({ children, note }: { children: ReactNode; note?: string }) {
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
