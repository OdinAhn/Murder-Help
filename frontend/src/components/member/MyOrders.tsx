import { useEffect, useState } from "react";
import { fetchMyOrders, type OrderData, type OrderPeriod, type OrderStatus } from "../../api/orders";
import { C, krw } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";
import { Spinner } from "../common/Spinner";

const PAGE_SIZE = 10;

const PERIODS: [OrderPeriod, string][] = [
  ["MONTH_1", "1개월"],
  ["MONTH_3", "3개월"],
  ["MONTH_6", "6개월"],
  ["MONTH_12", "1년"],
  ["ALL", "전체"],
];

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PREPARING_DELIVERY: "배송 준비중",
  SHIPPING: "배송중",
  DELIVERED: "배송 완료",
  CANCELED: "주문 취소",
};

/* 끝난 주문(배송 완료·취소)은 흐리게 보여 준다 */
function statusColor(status: OrderStatus) {
  if (status === "CANCELED") return C.textMuted;
  if (status === "DELIVERED") return C.textDim;
  if (status === "PENDING_PAYMENT") return C.yellowBright;
  return C.redBright;
}

/* ─── 내 주문내역 ────────────────────────────────────────── */
export function MyOrders() {
  const [period, setPeriod] = useState<OrderPeriod>("MONTH_3");
  const [status, setStatus] = useState<OrderStatus | "">("");
  const [orders, setOrders] = useState<OrderData[]>([]);
  /* 백엔드는 page=1 을 첫 페이지로 받는다(WebConfig 의 oneIndexedParameters) */
  const [page, setPage] = useState(1);
  const [hasNext, setHasNext] = useState(false);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  /* 기간·상태가 바뀌면 첫 페이지부터 다시 불러온다 */
  useEffect(() => {
    const controller = new AbortController();
    setOrders([]);
    setLoading(true);
    setError("");

    fetchMyOrders({ period, status: status || undefined, page: 1, size: PAGE_SIZE }, controller.signal)
      .then((res) => {
        setOrders(res.items);
        setPage(1);
        setHasNext(res.hasNext);
        setTotal(res.totalElements);
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setHasNext(false);
        setTotal(0);
        setError(error instanceof Error ? error.message : "주문 내역을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [period, status, reloadKey]);

  async function loadMore() {
    if (!hasNext || loading) return;
    const nextPage = page + 1;
    setLoading(true);
    setError("");

    try {
      const res = await fetchMyOrders({ period, status: status || undefined, page: nextPage, size: PAGE_SIZE });
      setOrders((prev) => [...prev, ...res.items]);
      setPage(nextPage);
      setHasNext(res.hasNext);
    } catch (error) {
      setError(error instanceof Error ? error.message : "주문 내역을 더 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <PageTitle note={`// 주문 조회 · 배송 상태 · ${total}건`}>My Orders</PageTitle>

      {/* 조회 기간 · 주문 상태 */}
      <div className="flex flex-wrap items-center gap-2 mb-5">
        {PERIODS.map(([value, label]) => (
          <button
            key={value}
            onClick={() => setPeriod(value)}
            className="text-[10px] uppercase tracking-widest px-3 py-1.5 transition-all"
            style={{
              fontFamily: "Share Tech Mono",
              background: period === value ? C.red : "rgba(0,0,0,0.5)",
              color: period === value ? "#fff" : C.textMuted,
              border: `1px solid ${period === value ? C.red : C.panelBorder}`,
            }}
          >
            {label}
          </button>
        ))}

        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as OrderStatus | "")}
          className="ml-auto text-[10px] uppercase tracking-wider px-2 py-1.5 outline-none"
          style={{ background: "rgba(0,0,0,0.5)", color: C.textMuted, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
        >
          <option value="">전체 상태</option>
          {(Object.keys(STATUS_LABEL) as OrderStatus[]).map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <div
          className="mb-4 flex items-center justify-between gap-4 px-4 py-3"
          style={{ border: `1px solid ${C.redDim}` }}
        >
          <p className="text-xs" style={{ color: C.redBright, fontFamily: "Noto Sans KR, sans-serif" }}>
            {error}
          </p>
          <button
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
            className="shrink-0 px-3 py-1.5 text-[10px] font-bold uppercase tracking-widest"
            style={{ color: C.text, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
          >
            다시 시도
          </button>
        </div>
      )}

      {loading && orders.length === 0 ? (
        <div
          className="flex items-center justify-center gap-3 py-16"
          style={{ border: `1px dashed ${C.panelBorder}`, color: C.textDim }}
        >
          <Spinner color={C.redBright} />
          <span className="text-xs" style={{ fontFamily: "Share Tech Mono" }}>
            주문 내역을 불러오는 중...
          </span>
        </div>
      ) : !error && orders.length === 0 ? (
        <p className="py-12 text-center" style={{ color: C.textMuted }}>
          해당 기간의 주문 내역이 없습니다.
        </p>
      ) : (
        <div className="space-y-3">
          {orders.map((order) => (
            <OrderCard key={order.orderNumber} order={order} />
          ))}
        </div>
      )}

      {hasNext && (
        <div className="text-center mt-6">
          <button
            onClick={() => void loadMore()}
            disabled={loading}
            className="px-10 py-2.5 text-xs font-bold uppercase tracking-widest"
            style={{ border: `1px solid ${C.panelBorder}`, color: C.textDim, fontFamily: "Share Tech Mono", cursor: loading ? "wait" : "pointer" }}
          >
            {loading ? "불러오는 중…" : "더 보기 →"}
          </button>
        </div>
      )}
    </>
  );
}

function OrderCard({ order }: { order: OrderData }) {
  const color = statusColor(order.status);

  return (
    <div className="p-5" style={{ border: `1px solid ${C.panelBorder}`, background: "rgba(0,0,0,0.18)" }}>
      <div className="flex items-center justify-between gap-3 mb-3">
        <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
          주문번호 {order.orderNumber}
        </span>
        <span
          className="text-[10px] px-2 py-0.5 tracking-widest"
          style={{ color, border: `1px solid ${color}`, fontFamily: "Share Tech Mono" }}
        >
          {STATUS_LABEL[order.status]}
        </span>
      </div>

      {/* 주문 상품 — 응답에 상품 ID가 없어 순번을 key 로 쓴다 */}
      {order.items.map((item, i) => (
        <div
          key={i}
          className="flex items-center justify-between gap-4 py-2 text-sm"
          style={{ borderTop: `1px solid ${C.panelBorder}` }}
        >
          <span className="truncate" style={{ color: C.text }}>
            {item.productName}
          </span>
          <span className="shrink-0 text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
            {krw(item.unitPrice)} × {item.quantity}
          </span>
        </div>
      ))}

      <div
        className="flex flex-wrap items-end justify-between gap-3 pt-3"
        style={{ borderTop: `1px solid ${C.panelBorder}` }}
      >
        <div className="text-[11px] leading-relaxed" style={{ color: C.textMuted }}>
          <div>{order.receiverName} · {order.receiverPhone}</div>
          <div>{order.deliveryAddress}</div>
          {order.deliveryRequest && <div>요청사항: {order.deliveryRequest}</div>}
        </div>
        <div className="text-right">
          <div className="text-[10px] uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
            Total
          </div>
          <div className="text-lg font-bold" style={{ color: C.price, fontFamily: "Share Tech Mono" }}>
            {krw(order.totalAmount)}
          </div>
        </div>
      </div>
    </div>
  );
}
