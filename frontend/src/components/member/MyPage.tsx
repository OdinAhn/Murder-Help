import { useEffect, useState } from "react";
import { authFetch } from "../../api/client";
import { C } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";
import { MyOrders } from "./MyOrders";

type Section = "home" | "orders" | "reviews";
type Tab = "pending" | "written";

type Api<T> = { code: string; message?: string; data?: T };

type PendingReview = {
    orderItemId: number;
    productCode: string;
    productName: string;
    purchasedAt: string;
    imageUrl?: string;
};

type WrittenReview = {
    reviewId: number;
    productId: number;
    productCode?: string;
    productName?: string;
    rating: number;
    content: string;
    createdAt: string;
};

async function api<T>(url: string, options: RequestInit = {}): Promise<T> {
    const response = await authFetch(url, options);

    const body = (await response.json().catch(() => null)) as Api<T> | null;

    if (!response.ok || !body || body.code !== "SUCCESS") {
        throw new Error(body?.message ?? "요청에 실패했습니다.");
    }

    return body.data as T;
}

function date(value?: string) {
    return value ? value.slice(0, 10).replace(/-/g, ".") : "";
}

export function MyPage({ onBack }: { onBack: () => void }) {
    const [section, setSection] = useState<Section>("home");
    const [tab, setTab] = useState<Tab>("pending");
    const [reviewView, setReviewView] = useState<"list" | "write">("list");

    const [pending, setPending] = useState<PendingReview[]>([]);
    const [written, setWritten] = useState<WrittenReview[]>([]);
    const [selected, setSelected] = useState<PendingReview | null>(null);

    const [rating, setRating] = useState(5);
    const [content, setContent] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function loadReviews() {
        setLoading(true);
        setError("");

        try {
            const [pendingData, writtenData] = await Promise.all([
                api<PendingReview[]>("/api/reviews/pending"),
                api<WrittenReview[]>("/api/reviews/me"),
            ]);

            setPending(pendingData ?? []);
            setWritten(writtenData ?? []);
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : "리뷰 목록을 불러오지 못했습니다.",
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        if (section === "reviews") {
            void loadReviews();
        }
    }, [section]);

    function openWrite(review: PendingReview) {
        setSelected(review);
        setRating(5);
        setContent("");
        setError("");
        setReviewView("write");
    }

    async function submitReview() {
        if (!selected) return;

        if (!content.trim()) {
            setError("리뷰 내용을 입력해 주세요.");
            return;
        }

        try {
            await api("/api/reviews", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    orderItemId: selected.orderItemId,
                    rating,
                    content: content.trim(),
                }),
            });

            setSelected(null);
            setContent("");
            setReviewView("list");
            setTab("written");

            await loadReviews();
        } catch (error) {
            setError(
                error instanceof Error ? error.message : "리뷰 작성에 실패했습니다.",
            );
        }
    }

    async function deleteReview(reviewId: number) {
        if (!window.confirm("이 리뷰를 삭제하시겠습니까?")) return;

        try {
            await api<void>(`/api/reviews/${reviewId}`, {
                method: "DELETE",
            });

            await loadReviews();
        } catch (error) {
            setError(
                error instanceof Error ? error.message : "리뷰 삭제에 실패했습니다.",
            );
        }
    }

    const menu = [
        ["home", "마이페이지"],
        ["orders", "내 주문내역 관리"],
        ["reviews", "내 리뷰 관리"],
    ] as const;

    return (
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
            <button
                onClick={onBack}
                className="text-xs uppercase tracking-widest mb-4"
                style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}
            >
                ← 목록으로
            </button>

            <div
                className="flex flex-col md:flex-row min-h-[560px]"
                style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}
            >
                <aside
                    className="w-full md:w-64 shrink-0 p-5"
                    style={{ borderRight: `1px solid ${C.panelBorder}` }}
                >
                    <div
                        className="text-lg mb-8"
                        style={{ color: C.text, fontFamily: "Cinzel, serif" }}
                    >
                        MY PAGE
                    </div>

                    {menu.map(([key, label]) => (
                        <button
                            key={key}
                            onClick={() => {
                                setSection(key);
                                if (key === "reviews") setReviewView("list");
                            }}
                            className="block w-full text-left px-4 py-3 text-sm"
                            style={{
                                color: section === key ? C.text : C.textMuted,
                                background:
                                    section === key ? "rgba(200,30,0,0.16)" : "transparent",
                                borderLeft: `2px solid ${
                                    section === key ? C.red : "transparent"
                                }`,
                            }}
                        >
                            {label}
                        </button>
                    ))}
                </aside>

                <main className="flex-1 min-w-0 p-6 md:p-9">
                    {section === "home" && (
                        <>
                            <PageTitle note="// 회원 정보와 활동 내역">My Page</PageTitle>
                            <p style={{ color: C.textMuted }}>
                                왼쪽 메뉴에서 주문 내역 또는 리뷰 관리를 선택하세요.
                            </p>
                        </>
                    )}

                    {section === "orders" && <MyOrders />}

                    {section === "reviews" && reviewView === "list" && (
                        <>
                            <PageTitle note="// 구매 확정 상품의 리뷰를 작성하고 관리합니다">
                                My Review Management
                            </PageTitle>

                            <div
                                className="grid grid-cols-2 mb-5"
                                style={{ border: `1px solid ${C.panelBorder}` }}
                            >
                                <button
                                    onClick={() => setTab("pending")}
                                    className="py-3"
                                    style={{
                                        color: tab === "pending" ? C.text : C.textMuted,
                                        borderBottom: `2px solid ${
                                            tab === "pending" ? C.red : "transparent"
                                        }`,
                                    }}
                                >
                                    리뷰 작성
                                </button>

                                <button
                                    onClick={() => setTab("written")}
                                    className="py-3"
                                    style={{
                                        color: tab === "written" ? C.text : C.textMuted,
                                        borderBottom: `2px solid ${
                                            tab === "written" ? C.red : "transparent"
                                        }`,
                                    }}
                                >
                                    작성한 리뷰
                                </button>
                            </div>

                            {error && (
                                <p className="mb-4 text-xs" style={{ color: C.redBright }}>
                                    {error}
                                </p>
                            )}

                            {loading && (
                                <p className="py-12 text-center" style={{ color: C.textMuted }}>
                                    불러오는 중입니다.
                                </p>
                            )}

                            {!loading && tab === "pending" && (
                                <>
                                    {pending.length === 0 && (
                                        <p className="py-12 text-center" style={{ color: C.textMuted }}>
                                            작성할 수 있는 리뷰가 없습니다.
                                        </p>
                                    )}

                                    {pending.map((review) => (
                                        <div
                                            key={review.orderItemId}
                                            className="flex items-center gap-4 py-5"
                                            style={{ borderBottom: `1px solid ${C.panelBorder}` }}
                                        >
                                            {review.imageUrl && (
                                                <img
                                                    src={review.imageUrl}
                                                    alt={review.productName}
                                                    className="w-16 h-16 object-cover"
                                                />
                                            )}

                                            <div className="flex-1">
                                                <div style={{ color: C.text }}>
                                                    {review.productCode} · {review.productName}
                                                </div>
                                                <div className="mt-2 text-xs" style={{ color: C.textMuted }}>
                                                    {date(review.purchasedAt)} 구매 확정
                                                </div>
                                            </div>

                                            <button
                                                onClick={() => openWrite(review)}
                                                className="px-5 py-2 text-xs"
                                                style={{
                                                    background: C.red,
                                                    color: "#fff",
                                                    fontFamily: "Noto Sans KR, sans-serif",
                                                }}
                                            >
                                                리뷰 작성하기
                                            </button>
                                        </div>
                                    ))}
                                </>
                            )}

                            {!loading && tab === "written" && (
                                <>
                                    {written.length === 0 && (
                                        <p className="py-12 text-center" style={{ color: C.textMuted }}>
                                            작성한 리뷰가 없습니다.
                                        </p>
                                    )}

                                    {written.map((review) => (
                                        <div
                                            key={review.reviewId}
                                            className="flex gap-4 py-5"
                                            style={{ borderBottom: `1px solid ${C.panelBorder}` }}
                                        >
                                            <div className="flex-1">
                                                <div style={{ color: C.text }}>
                                                    {"★".repeat(review.rating)}{" "}
                                                    <span style={{ color: C.textMuted }}>
                            {review.productCode ?? `상품 #${review.productId}`}
                          </span>
                                                </div>

                                                <p className="mt-2 text-xs" style={{ color: C.textDim }}>
                                                    {review.content}
                                                </p>

                                                <div className="mt-2 text-[11px]" style={{ color: C.textMuted }}>
                                                    {date(review.createdAt)}
                                                </div>
                                            </div>

                                            <button
                                                onClick={() => void deleteReview(review.reviewId)}
                                                className="h-fit px-3 py-1.5 text-xs"
                                                style={{
                                                    border: `1px solid ${C.panelBorder}`,
                                                    color: C.textMuted,
                                                }}
                                            >
                                                삭제
                                            </button>
                                        </div>
                                    ))}
                                </>
                            )}
                        </>
                    )}

                    {section === "reviews" &&
                        reviewView === "write" &&
                        selected && (
                            <>
                                <button
                                    onClick={() => setReviewView("list")}
                                    className="text-xs mb-5"
                                    style={{ color: C.textMuted }}
                                >
                                    ← 리뷰 목록으로
                                </button>

                                <PageTitle note={`// REVIEWS / WRITE · ${selected.productCode}`}>
                                    Review Write
                                </PageTitle>

                                <div
                                    className="p-6"
                                    style={{
                                        border: `1px solid ${C.panelBorder}`,
                                        background: "rgba(0,0,0,0.18)",
                                    }}
                                >
                                    <div className="mb-6 text-lg" style={{ color: C.text }}>
                                        {selected.productName}
                                    </div>

                                    <div className="flex gap-2 mb-3">
                                        {[1, 2, 3, 4, 5].map((value) => (
                                            <button
                                                key={value}
                                                onClick={() => setRating(value)}
                                                className="text-3xl"
                                                style={{
                                                    color: value <= rating ? C.redBright : C.textMuted,
                                                }}
                                            >
                                                ★
                                            </button>
                                        ))}
                                    </div>

                                    <p className="mb-5 text-xs" style={{ color: C.textMuted }}>
                                        선택한 별점: {rating} / 5
                                    </p>

                                    <textarea
                                        value={content}
                                        maxLength={500}
                                        onChange={(event) => setContent(event.target.value)}
                                        placeholder="리뷰 내용을 입력해 주세요."
                                        className="w-full min-h-40 px-4 py-4 text-sm outline-none"
                                        style={{
                                            background: "#120000",
                                            color: C.text,
                                            border: `1px solid ${C.panelBorder}`,
                                        }}
                                    />

                                    <div className="mt-2 text-right text-xs" style={{ color: C.textMuted }}>
                                        {content.length} / 500
                                    </div>

                                    {error && (
                                        <p className="mt-3 text-xs" style={{ color: C.redBright }}>
                                            {error}
                                        </p>
                                    )}

                                    <button
                                        onClick={() => void submitReview()}
                                        disabled={!content.trim()}
                                        className="w-full mt-6 py-4 text-sm"
                                        style={{
                                            background: C.red,
                                            color: "#fff",
                                            opacity: content.trim() ? 1 : 0.5,
                                        }}
                                    >
                                        리뷰 등록하기 →
                                    </button>
                                </div>
                            </>
                        )}
                </main>
            </div>
        </div>
    );
}