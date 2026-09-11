/* ─── palette ─────────────────────────────────────────── */
export const C = {
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

export function krw(n: number) {
  return "₩" + n.toLocaleString("ko-KR");
}
