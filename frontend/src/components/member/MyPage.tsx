import { C } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";

/* ─── 마이페이지 ─────────────────────────────────────────── */
/* 지금은 메뉴 두 개만 있다. 각 메뉴의 실제 화면은 아직 없다. */
const MYPAGE_MENUS = [
  { key: "orders", label: "내 주문내역 관리", desc: "주문 조회 · 배송 상태 · 취소" },
  { key: "reviews", label: "내 리뷰 관리", desc: "작성한 리뷰 확인 · 수정 · 삭제" },
];

export function MyPage({ onBack }: { onBack: () => void }) {
  return (
    <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
      <button
        onClick={onBack}
        className="text-xs uppercase tracking-widest mb-4"
        style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
      >
        ← 목록으로
      </button>

      <PageTitle note="// 회원 정보와 활동 내역">My Page</PageTitle>

      <div className="max-w-2xl" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
        {MYPAGE_MENUS.map((menu) => (
          <div
            key={menu.key}
            className="flex items-center gap-4 px-6 py-5"
            style={{ borderBottom: `1px solid ${C.panelBorder}` }}
          >
            <div className="flex-1">
              <div className="text-base mb-1" style={{ color: C.text, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
                {menu.label}
              </div>
              <div className="text-xs" style={{ color: C.textMuted, fontFamily: "Noto Sans KR, sans-serif" }}>
                {menu.desc}
              </div>
            </div>
            <span
              className="text-[10px] uppercase tracking-widest px-2 py-1 shrink-0"
              style={{ border: `1px solid ${C.panelBorder}`, color: C.textMuted, fontFamily: "Share Tech Mono" }}
            >
              준비 중
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
