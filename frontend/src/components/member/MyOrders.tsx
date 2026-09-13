import { useEffect, useState } from "react";
import {
  fetchMyOrders,
  type OrderData,
  type OrderItemData,
  type OrderPeriod,
  type OrderStatus,
} from "../../api/orders";
import { formatDate, ORDER_STATUS_LABEL, orderStatusStyle } from "../../lib/order";
import { C, krw } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";
import { Spinner } from "../common/Spinner";
import { OrderDetail } from "../order/OrderDetail";
import { OrderItemRow } from "../order/OrderItemRow";

const PAGE_SIZE = 10;
/* 페이지 번호는 현재 페이지 주변으로 최대 5개까지만 보여 준다 */
const PAGE_WINDOW = 5;

const PERIODS: [OrderPeriod, string][] = [
  ["MONTH_1", "1개월"],
  ["MONTH_3", "3개월"],
  ["MONTH_6", "6개월"],
  ["MONTH_12", "12개월"],
  ["ALL", "전체"],
];

/* 필터로 고를 수 있는 주문 상태 — 결제 대기는 필터에서 뺀다 */
const STATUS_FILTERS: OrderStatus[] = ["PAID", "PREPARING_DELIVERY", "SHIPPING", "DELIVERED", "CANCELED"];

/* 리뷰는 배송완료된 주문만 쓸 수 있다(ReviewService 가 DELIVERED 만 허용).
   그 외 상태는 눌러도 서버가 거절하므로 칸을 비워 둔다 */
function canWriteReview(status: OrderStatus) {
  return status === "DELIVERED";
}

type Props = {
  /* 빈 상태의 '쇼핑하러 가기' */
  onShop: () => void;
  onWriteReview: (order: OrderData, item: OrderItemData) => void;
  /* 리뷰를 쓰고 돌아왔을 때 '리뷰 작성 완료' 로 바뀌도록 바깥에서 다시 불러오게 한다 */
  refreshKey?: number;
};

/* ─── 내 주문내역 ────────────────────────────────────────── */
export function MyOrders({ onShop, onWriteReview, refreshKey = 0 }: Props) {
  const [period, setPeriod] = useState<OrderPeriod>("MONTH_3");
  const [status, setStatus] = useState<OrderStatus | "">("");
  /* 백엔드는 page=1 을 첫 페이지로 받는다(WebConfig 의 oneIndexedParameters) */
  const [page, setPage] = useState(1);
  const [orders, setOrders] = useState<OrderData[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  /* 0건일 때 필터 때문인지, 주문이 아예 없는지 구분해서 문구를 바꾼다 */
  const [noOrdersAtAll, setNoOrdersAtAll] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [detail, setDetail] = useState<OrderData | null>(null);

  /* 기간·상태·페이지가 바뀌는 순간 바로 서버에 다시 조회한다 */
  useEffect(() => {
    const controller = new AbortController();
    setOrders([]);
    setLoading(true);
    setError("");

    async function load() {
      const res = await fetchMyOrders({ period, status: status || undefined, page, size: PAGE_SIZE }, controller.signal);

      /* 0건이면 전체 기간 · 전체 상태로 한 번 더 확인한다. 이미 그 조건이면 다시 부를 필요 없다 */
      let empty = false;
      if (res.totalElements === 0) {
        empty = period === "ALL" && !status
          ? true
          : (await fetchMyOrders({ period: "ALL", page: 1, size: 1 }, controller.signal)).totalElements === 0;
      }

      if (controller.signal.aborted) return;
      setOrders(res.items);
      setTotal(res.totalElements);
      setTotalPages(res.totalPages);
      setNoOrdersAtAll(empty);
    }

    load()
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setTotal(0);
        setTotalPages(0);
        setError(error instanceof Error ? error.message : "주문 내역을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [period, status, page, reloadKey, refreshKey]);

  /* 기간·상태가 바뀌면 첫 페이지부터 조회한다 */
  function changePeriod(value: OrderPeriod) {
    setPeriod(value);
    setPage(1);
  }

  function changeStatus(value: OrderStatus | "") {
    setStatus(value);
    setPage(1);
  }

  function changePage(value: number) {
    if (value < 1 || value > totalPages || value === page) return;
    setPage(value);
    window.scrollTo({ top: 0 });
  }

  function openDetail(order: OrderData) {
    setDetail(order);
    window.scrollTo({ top: 0 });
  }

  if (detail) {
    return <OrderDetail order={detail} onBack={() => setDetail(null)} />;
  }

  const periodLabel = PERIODS.find(([value]) => value === period)?.[1];
  const note = period === "ALL" ? `// 전체 기간 · 총 ${total}건` : `// 최근 ${periodLabel} · 총 ${total}건`;

  return (
    <>
      <PageTitle note={note}>My Orders</PageTitle>

      {/* 조회 기간 · 주문 상태 — 둘 다 조건으로 같이 걸린다 */}
      <div
        className="flex flex-wrap items-center gap-2 py-3 mb-5"
        style={{ borderTop: `1px solid ${C.panelBorder}`, borderBottom: `1px solid ${C.panelBorder}` }}
      >
        {PERIODS.map(([value, label]) => (
          <button
            key={value}
            onClick={() => changePeriod(value)}
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
          onChange={(e) => changeStatus(e.target.value as OrderStatus | "")}
          className="ml-auto text-[10px] tracking-wider px-2 py-1.5 outline-none"
          style={{ background: "rgba(0,0,0,0.5)", color: C.textMuted, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
        >
          <option value="">주문상태 · 전체</option>
          {STATUS_FILTERS.map((s) => (
            <option key={s} value={s}>
              주문상태 · {ORDER_STATUS_LABEL[s]}
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

      {loading ? (
        <div
          className="flex items-center justify-center gap-3 py-16"
          style={{ border: `1px dashed ${C.panelBorder}`, color: C.textDim }}
        >
          <Spinner color={C.redBright} />
          <span className="text-xs" style={{ fontFamily: "Share Tech Mono" }}>
            주문 내역을 불러오는 중...
          </span>
        </div>
      ) : error ? null : orders.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16"
          style={{ border: `1px dashed ${C.panelBorder}` }}
        >
          <div className="text-3xl font-bold uppercase mb-2" style={{ fontFamily: "Cinzel, serif", color: C.redDim }}>
            NO ORDERS
          </div>
          <div className="text-xs mb-6" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
            // {noOrdersAtAll ? "아직 주문한 상품이 없습니다" : "조건에 맞는 주문이 없습니다"}
          </div>
          <button
            type="button"
            onClick={onShop}
            className="px-8 py-2.5 text-xs font-bold uppercase tracking-widest"
            style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
          >
            쇼핑하러 가기 →
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <OrderCard
              key={order.orderNumber}
              order={order}
              onOpenDetail={() => openDetail(order)}
              onWriteReview={(item) => onWriteReview(order, item)}
            />
          ))}
        </div>
      )}

      {totalPages > 1 && <Pagination page={page} totalPages={totalPages} onChange={changePage} />}
    </>
  );
}

/* ─── 주문 카드 ──────────────────────────────────────────── */
/* 주문 1건이 카드 1개. 한 번에 여러 상품을 주문했으면 카드 안에 품목 줄로 쌓는다 */
function OrderCard({
  order,
  onOpenDetail,
  onWriteReview,
}: {
  order: OrderData;
  onOpenDetail: () => void;
  onWriteReview: (item: OrderItemData) => void;
}) {
  return (
    <div style={{ border: `1px solid ${C.panelBorder}`, background: "rgba(0,0,0,0.18)" }}>
      {/* 주문일 · 주문번호 · 상태 · 주문 상세 */}
      <div
        className="flex flex-wrap items-center gap-3 px-5 py-3"
        style={{ background: "rgba(200,30,0,0.06)", borderBottom: `1px solid ${C.panelBorder}` }}
      >
        {order.orderedAt && (
          <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
            {formatDate(order.orderedAt)}
          </span>
        )}
        <span className="text-xs" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
          주문번호 {order.orderNumber}
        </span>
        <span
          className="text-[10px] px-2 py-0.5 tracking-widest"
          style={{ ...orderStatusStyle(order.status), fontFamily: "Share Tech Mono" }}
        >
          {ORDER_STATUS_LABEL[order.status]}
        </span>
        <button
          type="button"
          onClick={onOpenDetail}
          className="ml-auto text-[11px]"
          style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
        >
          주문 상세 →
        </button>
      </div>

      {/* 주문 상품 — orderItemId 가 아직 응답에 없으면 순번을 key 로 쓴다 */}
      {order.items.map((item, i) => (
        <div key={item.orderItemId ?? i} className="px-5" style={{ borderBottom: `1px solid ${C.panelBorder}` }}>
          <OrderItemRow item={item} action={reviewAction(order, item, onWriteReview)} />
        </div>
      ))}

      {/* 품목 수 · 결제 금액 — 취소된 주문은 금액을 흐리게 보여 준다 */}
      <div className="flex items-center justify-between px-5 py-3">
        <span className="text-xs" style={{ color: C.textDim }}>
          {order.items.length}개 품목
        </span>
        <span
          className="text-lg font-bold"
          style={{ color: order.status === "CANCELED" ? C.textMuted : C.price, fontFamily: "Share Tech Mono" }}
        >
          {krw(order.totalAmount)}
        </span>
      </div>
    </div>
  );
}

/* 리뷰가 있으면 '리뷰 작성 완료'(비활성), 없으면(null · 필드 없음) '리뷰 작성',
   리뷰를 쓸 수 없는 주문이면 칸을 비워 둔다 */
function reviewAction(order: OrderData, item: OrderItemData, onWriteReview: (item: OrderItemData) => void) {
  if (item.review) {
    return (
      <button
        type="button"
        disabled
        className="px-3 py-1.5 text-[11px]"
        style={{ color: C.textMuted, border: `1px solid ${C.panelBorder}`, cursor: "default" }}
      >
        리뷰 작성 완료
      </button>
    );
  }

  if (!canWriteReview(order.status)) return null;

  return (
    <button
      type="button"
      onClick={() => onWriteReview(item)}
      className="px-3 py-1.5 text-[11px]"
      style={{ color: C.text, border: `1px solid ${C.textDim}` }}
    >
      리뷰 작성
    </button>
  );
}

/* ─── 페이지 번호 ← 1 2 3 → ──────────────────────────────── */
function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  const start = Math.max(1, Math.min(page - Math.floor(PAGE_WINDOW / 2), totalPages - PAGE_WINDOW + 1));
  const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
  const pages = Array.from({ length: end - start + 1 }, (_, i) => start + i);

  return (
    <div className="flex items-center justify-center gap-4 mt-8 text-xs" style={{ fontFamily: "Share Tech Mono" }}>
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page === 1}
        aria-label="이전 페이지"
        style={{ color: page === 1 ? C.textMuted : C.textDim }}
      >
        ←
      </button>

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          onClick={() => onChange(p)}
          className="pb-0.5"
          style={{
            color: p === page ? C.text : C.textDim,
            borderBottom: `1px solid ${p === page ? C.redBright : "transparent"}`,
          }}
        >
          {p}
        </button>
      ))}

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page === totalPages}
        aria-label="다음 페이지"
        style={{ color: page === totalPages ? C.textMuted : C.textDim }}
      >
        →
      </button>
    </div>
  );
}
