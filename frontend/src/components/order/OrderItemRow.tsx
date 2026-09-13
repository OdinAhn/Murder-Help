import type { ReactNode } from "react";
import type { OrderItemData } from "../../api/orders";
import { C, krw } from "../../lib/theme";
import { TierBadge } from "../member/TierBadge";

/* ─── 주문 상품 한 줄 ─────────────────────────────────────── */
/* 주문내역 카드와 주문 상세에서 같이 쓴다. 가격은 결제 당시 스냅샷(unitPrice)이다.
   action 에는 오른쪽 칸(리뷰 버튼)을 넣는다. null 을 넘기면 칸만 비워서 줄 정렬을 맞춘다. */
export function OrderItemRow({ item, action }: { item: OrderItemData; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap md:flex-nowrap items-center gap-4 py-4">
      <Thumbnail src={item.imageUrl} alt={item.productName} />

      <div className="flex-1 min-w-0">
        {(item.productCode || item.tier) && (
          <div className="flex items-center gap-2 mb-1.5">
            {item.productCode && (
              <span className="text-[10px]" style={{ color: C.textMuted, fontFamily: "Share Tech Mono", letterSpacing: "0.1em" }}>
                ITEM NO. {item.productCode}
              </span>
            )}
            {item.tier && <TierBadge tier={item.tier} small />}
          </div>
        )}
        <div className="truncate text-sm" style={{ color: C.text, fontWeight: 300 }}>
          {item.productName}
        </div>
      </div>

      <div className="w-24 shrink-0 text-center text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
        {krw(item.unitPrice)} × {item.quantity}
      </div>

      <div className="w-24 shrink-0 text-right text-sm font-bold" style={{ color: C.text, fontFamily: "Share Tech Mono" }}>
        {krw(item.unitPrice * item.quantity)}
      </div>

      {action !== undefined && <div className="w-28 shrink-0 text-right">{action}</div>}
    </div>
  );
}

/* 이미지가 없으면 빈 칸에 X 를 그려 자리만 잡는다 */
function Thumbnail({ src, alt }: { src?: string; alt: string }) {
  return (
    <div className="w-14 h-14 shrink-0 overflow-hidden" style={{ background: "#060606", border: `1px solid ${C.panelBorder}` }}>
      {src ? (
        <img src={src} alt={alt} className="w-full h-full object-cover" style={{ filter: "brightness(0.8) saturate(0.65)" }} />
      ) : (
        <svg width="100%" height="100%" viewBox="0 0 56 56" preserveAspectRatio="none">
          <line x1="0" y1="0" x2="56" y2="56" stroke={C.panelBorder} />
          <line x1="56" y1="0" x2="0" y2="56" stroke={C.panelBorder} />
        </svg>
      )}
    </div>
  );
}
