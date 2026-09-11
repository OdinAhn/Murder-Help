export function Spinner({ color = "#fff" }: { color?: string }) {
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
