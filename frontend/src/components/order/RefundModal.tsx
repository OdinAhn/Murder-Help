import { useEffect, useMemo, useState } from "react";
import { fetchRefundableItems, requestRefund, type RefundableItem } from "../../api/refund";
import { C, krw } from "../../lib/theme";
import { Spinner } from "../common/Spinner";

const REASONS = [
  { value: "SIMPLE_CHANGE", label: "단순 변심" },
  { value: "SIZE_COLOR_MISMATCH", label: "색상/사이즈가 마음에 안 듦" },
  { value: "WANT_EXCHANGE", label: "다른 상품으로 교환 희망" },
  { value: "ORDER_MISTAKE", label: "주문 실수 (수량/옵션 잘못 선택)" },
  { value: "DEFECTIVE", label: "상품 불량 / 파손" },
  { value: "DESCRIPTION_MISMATCH", label: "설명과 다른 상품" },
  { value: "WRONG_DELIVERY", label: "오배송 (다른 상품이 옴)" },
  { value: "DELIVERY_DELAY", label: "배송 지연 / 배송 누락" },
  { value: "QUANTITY_SHORT", label: "수량 부족" },
  { value: "ETC", label: "기타" },
] as const;

type RefundLine = RefundableItem & { selectedQuantity: number };

export function RefundModal({
                              paymentId,
                              onClose,
                              onSuccess,
                            }: {
  paymentId: number;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [lines, setLines] = useState<RefundLine[] | null>(null);
  const [loadError, setLoadError] = useState("");
  const [reason, setReason] = useState("");
  const [detail, setDetail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  useEffect(() => {
    let active = true;
    fetchRefundableItems(paymentId)
        .then((items) => {
          if (!active) return;
          setLines(items.map((item) => ({ ...item, selectedQuantity: 0 })));
        })
        .catch((error: unknown) => {
          if (!active) return;
          setLoadError(error instanceof Error ? error.message : "환불 가능 상품을 불러오지 못했습니다.");
        });
    return () => {
      active = false;
    };
  }, [paymentId]);

  const refundableLines = useMemo(() => (lines ?? []).filter((l) => l.remainQuantity > 0), [lines]);
  const totalQuantity = useMemo(
      () => refundableLines.reduce((sum, l) => sum + l.selectedQuantity, 0),
      [refundableLines],
  );
  const totalAmount = useMemo(
      () => refundableLines.reduce((sum, l) => sum + l.selectedQuantity * l.orderPrice, 0),
      [refundableLines],
  );

  function changeQuantity(orderItemId: number, next: number) {
    setLines((prev) =>
        (prev ?? []).map((l) =>
            l.orderItemId === orderItemId
                ? { ...l, selectedQuantity: Math.max(0, Math.min(next, l.remainQuantity)) }
                : l,
        ),
    );
  }

  const canSubmit = totalQuantity > 0 && reason !== "" && !submitting;

  async function submit() {
    if (!canSubmit) return;

    setSubmitting(true);
    setSubmitError("");

    const reasonLabel = REASONS.find((r) => r.value === reason)?.label ?? reason;
    const cancelReason = detail.trim() ? `${reasonLabel}: ${detail.trim()}` : reasonLabel;

    try {
      await requestRefund({
        paymentId,
        cancelReason,
        items: refundableLines
            .filter((l) => l.selectedQuantity > 0)
            .map((l) => ({ orderItemId: l.orderItemId, requestQuantity: l.selectedQuantity })),
      });
      onSuccess();
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "환불 신청에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
      <div
          className="fixed inset-0 z-[100] flex items-center justify-center"
          style={{ background: "rgba(0,0,0,0.75)" }}
          onClick={submitting ? undefined : onClose}
      >
        <div
            className="w-full max-w-sm mx-4"
            style={{ background: "rgba(10,0,0,0.97)", border: `1px solid ${C.panelBorder}` }}
            onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div
              className="flex items-center justify-between px-5 py-4"
              style={{ borderBottom: `1px solid ${C.panelBorder}` }}
          >
          <span className="text-sm font-bold" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
            환불 신청
          </span>
            <button
                onClick={onClose}
                disabled={submitting}
                className="text-lg leading-none"
                style={{ color: C.textMuted }}
                aria-label="닫기"
            >
              ✕
            </button>
          </div>

          {loadError ? (
              <div className="p-5">
                <p className="text-xs" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
                  {loadError}
                </p>
              </div>
          ) : !lines ? (
              <div className="flex items-center justify-center gap-2 py-16">
                <Spinner color={C.redBright} />
                <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
              불러오는 중...
            </span>
              </div>
          ) : refundableLines.length === 0 ? (
              <div className="p-5">
                <p className="text-xs" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
                  환불 가능한 상품이 없습니다.
                </p>
              </div>
          ) : (
              <>
                {/* 환불 수량 선택 */}
                <div className="px-5 pt-4 pb-2">
                  <p className="text-[11px]" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                    환불 수량 선택
                  </p>
                </div>

                <div style={{ borderTop: `1px solid ${C.panelBorder}` }}>
                  {refundableLines.map((line) => (
                      <div
                          key={line.orderItemId}
                          className="flex items-center gap-3 px-5 py-3"
                          style={{ borderBottom: `1px solid ${C.panelBorder}` }}
                      >
                        <div
                            className="w-9 h-9 shrink-0"
                            style={{ background: "#060606", border: `1px solid ${C.panelBorder}` }}
                        />
                        <div className="flex-1 min-w-0">
                          <div className="text-xs truncate" style={{ color: C.text }}>
                            {line.productName}
                          </div>
                          <div className="text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                            단가 {krw(line.orderPrice)}
                          </div>
                        </div>
                        <div className="flex items-center gap-1 shrink-0">
                          <StepButton
                              disabled={line.selectedQuantity <= 0}
                              onClick={() => changeQuantity(line.orderItemId, line.selectedQuantity - 1)}
                          >
                            −
                          </StepButton>
                          <span
                              className="text-xs text-center"
                              style={{ width: 40, color: C.text, fontFamily: "Share Tech Mono" }}
                          >
                      {line.selectedQuantity} / {line.remainQuantity}
                    </span>
                          <StepButton
                              disabled={line.selectedQuantity >= line.remainQuantity}
                              onClick={() => changeQuantity(line.orderItemId, line.selectedQuantity + 1)}
                          >
                            +
                          </StepButton>
                        </div>
                      </div>
                  ))}
                </div>

                {/* 선택 요약 */}
                <div
                    className="flex items-center justify-between px-5 py-3"
                    style={{ borderBottom: `1px solid ${C.panelBorder}` }}
                >
              <span className="text-[11px]" style={{ color: C.textDim }}>
                총 환불 수량 {totalQuantity}개
              </span>
                  <span className="text-sm font-bold" style={{ color: C.redBright, fontFamily: "Share Tech Mono" }}>
                환불 예정 금액 {krw(totalAmount)}
              </span>
                </div>

                {/* 환불 사유 */}
                <div className="px-5 py-4" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
                  <p className="text-[11px] mb-2" style={{ color: C.textDim }}>
                    환불 사유
                  </p>
                  <select
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      disabled={submitting}
                      className="w-full px-3 py-2.5 text-xs outline-none mb-3"
                      style={{
                        background: "rgba(0,0,0,0.45)",
                        border: `1px solid ${C.panelBorder}`,
                        color: reason ? C.text : C.textMuted,
                        fontFamily: "Noto Sans KR, sans-serif",
                      }}
                  >
                    <option value="">사유를 선택해주세요</option>
                    {REASONS.map((r) => (
                        <option key={r.value} value={r.value}>
                          {r.label}
                        </option>
                    ))}
                  </select>

                  <textarea
                      value={detail}
                      onChange={(e) => setDetail(e.target.value.slice(0, 500))}
                      disabled={submitting}
                      placeholder="상세 사유를 입력해주세요 (선택)"
                      rows={3}
                      className="w-full px-3 py-2.5 text-xs outline-none resize-none"
                      style={{
                        background: "rgba(0,0,0,0.45)",
                        border: `1px solid ${C.panelBorder}`,
                        color: C.text,
                        fontFamily: "Noto Sans KR, sans-serif",
                      }}
                  />
                  <p
                      className="text-right text-[10px] mt-1"
                      style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}
                  >
                    {detail.length} / 500
                  </p>
                </div>

                {submitError && (
                    <p
                        className="px-5 pt-3 text-xs"
                        style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}
                    >
                      {submitError}
                    </p>
                )}

                {/* 버튼 */}
                <div className="flex gap-2 px-5 py-4">
                  <button
                      onClick={onClose}
                      disabled={submitting}
                      className="flex-1 py-2.5 text-xs font-bold uppercase tracking-widest"
                      style={{ color: C.textDim, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
                  >
                    취소
                  </button>
                  <button
                      onClick={submit}
                      disabled={!canSubmit}
                      className="flex-1 py-2.5 text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2"
                      style={{
                        background: canSubmit ? C.red : C.redDim,
                        color: "#fff",
                        border: `1px solid ${canSubmit ? C.redBright : C.redDim}`,
                        fontFamily: "Share Tech Mono",
                        cursor: canSubmit ? "pointer" : "not-allowed",
                      }}
                  >
                    {submitting ? <><Spinner /> 처리중...</> : "환불 신청 →"}
                  </button>
                </div>
              </>
          )}
        </div>
      </div>
  );
}

function StepButton({
                      children,
                      disabled,
                      onClick,
                    }: {
  children: string;
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
      <button
          type="button"
          onClick={onClick}
          disabled={disabled}
          className="flex items-center justify-center text-xs"
          style={{
            width: 22,
            height: 22,
            border: `1px solid ${disabled ? C.panelBorder : C.textDim}`,
            color: disabled ? C.textMuted : C.text,
            background: "rgba(0,0,0,0.4)",
            cursor: disabled ? "not-allowed" : "pointer",
            opacity: disabled ? 0.4 : 1,
          }}
      >
        {children}
      </button>
  );
}