import { useState } from "react";
import { refreshProductRankings } from "../../api/products";
import { C } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";

export function ProductRankingAdmin({ onBack }: { onBack: () => void }) {
  const [refreshing, setRefreshing] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function handleRefresh() {
    if (refreshing) return;
    if (!window.confirm("최근 7일 주문을 기준으로 상품 추천을 다시 집계할까요?")) return;

    setRefreshing(true);
    setMessage("");
    setError("");

    try {
      await refreshProductRankings();
      setMessage("상품 추천을 갱신했습니다. 메인 화면에서 최신 추천 목록을 확인할 수 있습니다.");
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "상품 추천 갱신에 실패했습니다.");
    } finally {
      setRefreshing(false);
    }
  }

  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
      <button
        onClick={onBack}
        className="text-xs uppercase tracking-widest mb-4"
        style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
      >
        ← 목록으로
      </button>

      <div className="max-w-2xl p-6 md:p-9" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
        <PageTitle note="GREEN ADMIN ONLY">상품 추천 관리</PageTitle>

        <div className="p-5" style={{ background: "rgba(0, 0, 0, 0.18)", border: `1px solid ${C.panelBorder}` }}>
          <h2 className="text-base mb-2" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif" }}>
            인기 상품 추천 갱신
          </h2>
          <p className="text-sm leading-6 mb-5" style={{ color: C.textDim }}>
            최근 7일간 배송 완료된 주문을 다시 집계해 Redis의 등급별 인기 상품 추천 목록을 즉시 갱신합니다.
          </p>

          <button
            type="button"
            onClick={() => void handleRefresh()}
            disabled={refreshing}
            className="w-full sm:w-auto px-5 py-3 text-sm font-bold transition-opacity"
            style={{
              background: refreshing ? C.redDim : C.red,
              color: "#fff",
              border: `1px solid ${refreshing ? C.redDim : C.redBright}`,
              fontFamily: "Share Tech Mono",
              cursor: refreshing ? "wait" : "pointer",
            }}
          >
            {refreshing ? "추천 갱신 중…" : "상품 추천 갱신"}
          </button>

          {message && <p className="mt-4 text-sm" style={{ color: "#34d399" }}>{message}</p>}
          {error && <p className="mt-4 text-sm" style={{ color: C.redBright }}>{error}</p>}
        </div>
      </div>
    </div>
  );
}
