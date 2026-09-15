import {
    type ChangeEvent,
    useEffect,
    useRef,
    useState,
} from "react";
import { authFetch } from "../../api/client";
import type { OrderData, OrderItemData } from "../../api/orders";
import { C } from "../../lib/theme";
import { PageTitle } from "../common/PageTitle";
import { MyOrders } from "./MyOrders";

type Section = "profile" | "orders" | "reviews";
type Tab = "pending" | "written";

type Api<T> = {
    code: string;
    message?: string;
    data?: T;
};

type MemberInfo = {
    id: number;
    email: string;
    name: string;
    phone: string;
    grade?: string;
    membershipGrade?: string;
    profileImageUrl?: string | null;
};

type PendingReview = {
    orderItemId: number;
    productCode: string;
    productName: string;
    purchasedAt: string;
    imageUrl?: string;
};

type ReviewTarget = Omit<PendingReview, "orderItemId"> & {
    orderItemId?: number;
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

function DefaultProfileImage() {
    return (
        <div
            className="w-24 h-24 flex items-center justify-center"
            style={{
                background: "#240606",
                border: `1px solid ${C.panelBorder}`,
            }}
        >
            <svg
                viewBox="0 0 100 100"
                width="72"
                height="72"
                aria-label="기본 프로필 이미지"
            >
                <circle cx="50" cy="34" r="17" fill="#8b544d" />
                <path
                    d="M20 88c4-21 17-31 30-31s26 10 30 31"
                    fill="#8b544d"
                />
            </svg>
        </div>
    );
}

export function MyPage({ onBack }: { onBack: () => void }) {
    /* 마이페이지 첫 진입 시 무조건 내 정보 수정 */
    const [section, setSection] = useState<Section>("profile");
    const [tab, setTab] = useState<Tab>("pending");
    const [reviewView, setReviewView] = useState<"list" | "write">("list");

    const [member, setMember] = useState<MemberInfo | null>(null);
    const [name, setName] = useState("");
    const [phone, setPhone] = useState("");
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [profileLoading, setProfileLoading] = useState(true);
    const [profileSaving, setProfileSaving] = useState(false);
    const [profileMessage, setProfileMessage] = useState("");
    const [profileError, setProfileError] = useState("");
    const [imageModalOpen, setImageModalOpen] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [pending, setPending] = useState<PendingReview[]>([]);
    const [written, setWritten] = useState<WrittenReview[]>([]);
    const [selected, setSelected] = useState<ReviewTarget | null>(null);

    const [rating, setRating] = useState(5);
    const [content, setContent] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [ordersRefreshKey, setOrdersRefreshKey] = useState(0);

    async function loadMyInfo() {
        setProfileLoading(true);
        setProfileError("");

        try {
            const data = await api<MemberInfo>("/api/members/me");

            setMember(data);
            setName(data.name ?? "");
            setPhone(data.phone ?? "");
            setPreviewUrl(data.profileImageUrl ?? null);
            setSelectedFile(null);
        } catch (caughtError) {
            setProfileError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "회원 정보를 불러오지 못했습니다.",
            );
        } finally {
            setProfileLoading(false);
        }
    }

    useEffect(() => {
        void loadMyInfo();
    }, []);

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
        } catch (caughtError) {
            setError(
                caughtError instanceof Error
                    ? caughtError.message
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

    function changeSection(nextSection: Section) {
        setSection(nextSection);
        setReviewView("list");
        setError("");
        setProfileError("");
        setProfileMessage("");
    }

    function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
        const file = event.target.files?.[0];

        if (!file) return;

        if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
            setProfileError("JPG, PNG, WEBP 형식의 이미지만 등록할 수 있습니다.");
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            setProfileError("프로필 이미지는 5MB 이하만 등록할 수 있습니다.");
            return;
        }

        setProfileError("");
        setProfileMessage("");
        setSelectedFile(file);
        setPreviewUrl(URL.createObjectURL(file));
    }

    async function uploadProfileImage() {
        if (!selectedFile) {
            setProfileError("업로드할 이미지를 먼저 선택해 주세요.");
            return;
        }

        setProfileSaving(true);
        setProfileError("");
        setProfileMessage("");

        try {
            const formData = new FormData();
            formData.append("image", selectedFile);

            const data = await api<MemberInfo>("/api/members/me/profile-image", {
                method: "PATCH",
                body: formData,
            });

            setMember(data);
            setPreviewUrl(data.profileImageUrl ?? null);
            setSelectedFile(null);
            setProfileMessage("프로필 이미지가 변경되었습니다.");

            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        } catch (caughtError) {
            setProfileError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "프로필 이미지 변경에 실패했습니다.",
            );
        } finally {
            setProfileSaving(false);
        }
    }

    async function deleteProfileImage() {
        if (!window.confirm("프로필 이미지를 기본 이미지로 변경하시겠습니까?")) {
            return;
        }

        setProfileSaving(true);
        setProfileError("");
        setProfileMessage("");

        try {
            const data = await api<MemberInfo>("/api/members/me/profile-image", {
                method: "DELETE",
            });

            setMember(data);
            setPreviewUrl(null);
            setSelectedFile(null);
            setProfileMessage("기본 프로필 이미지로 변경되었습니다.");

            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        } catch (caughtError) {
            setProfileError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "프로필 이미지 삭제에 실패했습니다.",
            );
        } finally {
            setProfileSaving(false);
        }
    }

    async function saveMyInfo() {
        setProfileSaving(true);
        setProfileError("");
        setProfileMessage("");

        try {
            const data = await api<MemberInfo>("/api/members/me", {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    name: name.trim(),
                    phone: phone.trim(),
                }),
            });

            setMember(data);
            setName(data.name ?? "");
            setPhone(data.phone ?? "");
            setProfileMessage("회원 정보가 저장되었습니다.");
        } catch (caughtError) {
            setProfileError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "회원 정보 저장에 실패했습니다.",
            );
        } finally {
            setProfileSaving(false);
        }
    }

    function openWrite(review: ReviewTarget) {
        setSelected(review);
        setRating(5);
        setContent("");
        setError("");
        setReviewView("write");
    }

    function openWriteFromOrder(order: OrderData, item: OrderItemData) {
        openWrite({
            orderItemId: item.orderItemId,
            productCode: item.productCode ?? "",
            productName: item.productName,
            purchasedAt: order.orderedAt ?? "",
            imageUrl: item.imageUrl,
        });
    }

    async function submitReview() {
        if (!selected) return;

        if (selected.orderItemId === undefined) {
            setError("주문 상품 번호가 없어 리뷰를 등록할 수 없습니다.");
            return;
        }

        if (!content.trim()) {
            setError("리뷰 내용을 입력해 주세요.");
            return;
        }

        try {
            await api("/api/reviews", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    orderItemId: selected.orderItemId,
                    rating,
                    content: content.trim(),
                }),
            });

            setSelected(null);
            setContent("");
            setReviewView("list");

            if (section === "orders") {
                setOrdersRefreshKey((key) => key + 1);
                return;
            }

            setTab("written");
            await loadReviews();
        } catch (caughtError) {
            setError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "리뷰 작성에 실패했습니다.",
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
        } catch (caughtError) {
            setError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "리뷰 삭제에 실패했습니다.",
            );
        }
    }

    const menu = [
        ["profile", "내 정보 수정", "프로필 이미지 · 이름 · 전화번호 관리"],
        ["orders", "내 주문내역 관리", "주문 조회 · 배송 상태 · 취소"],
        ["reviews", "내 리뷰 관리", "구매확정 상품 리뷰 작성 · 수정 · 삭제"],
    ] as const;

    const grade = member?.membershipGrade ?? member?.grade ?? "CODE YELLOW";

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
                style={{
                    background: C.panel,
                    border: `1px solid ${C.panelBorder}`,
                }}
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

                    {menu.map(([key, label, description]) => (
                        <button
                            key={key}
                            onClick={() => changeSection(key)}
                            className="block w-full text-left px-4 py-3 mb-1"
                            style={{
                                color: section === key ? C.text : C.textMuted,
                                background:
                                    section === key
                                        ? "rgba(200,30,0,0.16)"
                                        : "transparent",
                                borderLeft: `2px solid ${
                                    section === key ? C.red : "transparent"
                                }`,
                            }}
                        >
                            <div className="text-sm">{label}</div>
                            <div
                                className="mt-1 text-[10px]"
                                style={{ color: C.textDim }}
                            >
                                {description}
                            </div>
                        </button>
                    ))}
                </aside>

                <main className="flex-1 min-w-0 p-6 md:p-9">
                    {section === "profile" && (
                        <>
                            <PageTitle>
                                My Information
                            </PageTitle>

                            {profileLoading && (
                                <p
                                    className="py-12 text-center"
                                    style={{ color: C.textMuted }}
                                >
                                    회원 정보를 불러오는 중입니다.
                                </p>
                            )}

                            {!profileLoading && (
                                <div
                                    className="p-6"
                                    style={{
                                        border: `1px solid ${C.panelBorder}`,
                                        background: "rgba(0,0,0,0.18)",
                                    }}
                                >
                                    <div
                                        className="flex flex-col sm:flex-row gap-5 pb-6 mb-6"
                                        style={{
                                            borderBottom: `1px solid ${C.panelBorder}`,
                                        }}
                                    >
                                        <button
                                            type="button"
                                            onClick={() => {
                                                if (previewUrl) {
                                                    setImageModalOpen(true);
                                                }
                                            }}
                                            className="w-24 h-24 shrink-0 overflow-hidden"
                                            title={
                                                previewUrl
                                                    ? "이미지 크게 보기"
                                                    : "기본 프로필 이미지"
                                            }
                                            style={{
                                                cursor: previewUrl
                                                    ? "zoom-in"
                                                    : "default",
                                            }}
                                        >
                                            {previewUrl ? (
                                                <img
                                                    src={previewUrl}
                                                    alt="프로필"
                                                    className="w-full h-full object-cover"
                                                />
                                            ) : (
                                                <DefaultProfileImage />
                                            )}
                                        </button>

                                        <div className="flex-1">
                                            <div className="flex flex-wrap gap-2">
                                                <input
                                                    ref={fileInputRef}
                                                    type="file"
                                                    accept="image/jpeg,image/png,image/webp"
                                                    onChange={handleFileChange}
                                                    className="hidden"
                                                />

                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        fileInputRef.current?.click()
                                                    }
                                                    className="px-4 py-2 text-xs"
                                                    style={{
                                                        border: `1px solid ${C.panelBorder}`,
                                                        color: C.text,
                                                    }}
                                                >
                                                    이미지 선택
                                                </button>

                                                <button
                                                    type="button"
                                                    disabled={
                                                        !selectedFile || profileSaving
                                                    }
                                                    onClick={() =>
                                                        void uploadProfileImage()
                                                    }
                                                    className="px-4 py-2 text-xs"
                                                    style={{
                                                        background: C.red,
                                                        color: "#fff",
                                                        opacity:
                                                            !selectedFile || profileSaving
                                                                ? 0.5
                                                                : 1,
                                                    }}
                                                >
                                                    프로필 이미지 변경
                                                </button>

                                                {previewUrl && (
                                                    <button
                                                        type="button"
                                                        disabled={profileSaving}
                                                        onClick={() => void deleteProfileImage()}
                                                        className="px-4 py-2 text-xs"
                                                        style={{
                                                            border: `1px solid ${C.panelBorder}`,
                                                            background: "#120000",
                                                            color: C.text,
                                                            opacity: profileSaving ? 0.5 : 1,
                                                        }}
                                                    >
                                                        기본 이미지로 변경
                                                    </button>
                                                )}
                                            </div>

                                            <p
                                                className="mt-3 text-[11px]"
                                                style={{ color: C.textMuted }}
                                            >
                                                JPG, PNG, WEBP 형식 · 최대 5MB
                                            </p>

                                            {selectedFile && (
                                                <p
                                                    className="mt-2 text-[11px]"
                                                    style={{ color: C.textDim }}
                                                >
                                                    선택한 파일: {selectedFile.name}
                                                </p>
                                            )}
                                        </div>
                                    </div>

                                    <div className="grid grid-cols-1 gap-4">
                                        <label className="grid grid-cols-1 sm:grid-cols-[100px_1fr] gap-2 sm:items-center">
                                            <span
                                                className="text-sm"
                                                style={{ color: C.textMuted }}
                                            >
                                                이메일
                                            </span>
                                            <input
                                                value={member?.email ?? ""}
                                                disabled
                                                className="px-4 py-3 text-sm outline-none"
                                                style={{
                                                    background: "#120000",
                                                    color: C.textMuted,
                                                    border: `1px solid ${C.panelBorder}`,
                                                }}
                                            />
                                        </label>

                                        <label className="grid grid-cols-1 sm:grid-cols-[100px_1fr] gap-2 sm:items-center">
                                            <span
                                                className="text-sm"
                                                style={{ color: C.textMuted }}
                                            >
                                                이름
                                            </span>
                                            <input
                                                value={name}
                                                onChange={(event) =>
                                                    setName(event.target.value)
                                                }
                                                maxLength={50}
                                                className="px-4 py-3 text-sm outline-none"
                                                style={{
                                                    background: "#120000",
                                                    color: C.text,
                                                    border: `1px solid ${C.panelBorder}`,
                                                }}
                                            />
                                        </label>

                                        <label className="grid grid-cols-1 sm:grid-cols-[100px_1fr] gap-2 sm:items-center">
                                            <span
                                                className="text-sm"
                                                style={{ color: C.textMuted }}
                                            >
                                                전화번호
                                            </span>
                                            <input
                                                value={phone}
                                                onChange={(event) =>
                                                    setPhone(event.target.value)
                                                }
                                                maxLength={20}
                                                placeholder="010-0000-0000"
                                                className="px-4 py-3 text-sm outline-none"
                                                style={{
                                                    background: "#120000",
                                                    color: C.text,
                                                    border: `1px solid ${C.panelBorder}`,
                                                }}
                                            />
                                        </label>

                                        <label className="grid grid-cols-1 sm:grid-cols-[100px_1fr] gap-2 sm:items-center">
                                            <span
                                                className="text-sm"
                                                style={{ color: C.textMuted }}
                                            >
                                                회원 등급
                                            </span>
                                            <input
                                                value={grade}
                                                disabled
                                                className="px-4 py-3 text-sm outline-none"
                                                style={{
                                                    background: "#120000",
                                                    color: C.textMuted,
                                                    border: `1px solid ${C.panelBorder}`,
                                                }}
                                            />
                                        </label>
                                    </div>

                                    {profileError && (
                                        <p
                                            className="mt-4 text-xs"
                                            style={{ color: C.redBright }}
                                        >
                                            {profileError}
                                        </p>
                                    )}

                                    {profileMessage && (
                                        <p
                                            className="mt-4 text-xs"
                                            style={{ color: "#8fcf78" }}
                                        >
                                            {profileMessage}
                                        </p>
                                    )}

                                    <button
                                        type="button"
                                        disabled={profileSaving}
                                        onClick={() => void saveMyInfo()}
                                        className="w-full mt-6 py-4 text-sm"
                                        style={{
                                            background: C.red,
                                            color: "#fff",
                                            opacity: profileSaving ? 0.5 : 1,
                                        }}
                                    >
                                        회원 정보 저장
                                    </button>
                                </div>
                            )}
                        </>
                    )}

                    {section === "orders" && (
                        <div hidden={reviewView === "write"}>
                            <MyOrders
                                onShop={onBack}
                                onWriteReview={openWriteFromOrder}
                                refreshKey={ordersRefreshKey}
                            />
                        </div>
                    )}

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
                                <p
                                    className="mb-4 text-xs"
                                    style={{ color: C.redBright }}
                                >
                                    {error}
                                </p>
                            )}

                            {loading && (
                                <p
                                    className="py-12 text-center"
                                    style={{ color: C.textMuted }}
                                >
                                    불러오는 중입니다.
                                </p>
                            )}

                            {!loading && tab === "pending" && (
                                <>
                                    {pending.length === 0 && (
                                        <p
                                            className="py-12 text-center"
                                            style={{ color: C.textMuted }}
                                        >
                                            작성할 수 있는 리뷰가 없습니다.
                                        </p>
                                    )}

                                    {pending.map((review) => (
                                        <div
                                            key={review.orderItemId}
                                            className="flex items-center gap-4 py-5"
                                            style={{
                                                borderBottom: `1px solid ${C.panelBorder}`,
                                            }}
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
                                                <div
                                                    className="mt-2 text-xs"
                                                    style={{ color: C.textMuted }}
                                                >
                                                    {date(review.purchasedAt)} 구매 확정
                                                </div>
                                            </div>

                                            <button
                                                onClick={() => openWrite(review)}
                                                className="px-5 py-2 text-xs"
                                                style={{
                                                    background: C.red,
                                                    color: "#fff",
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
                                        <p
                                            className="py-12 text-center"
                                            style={{ color: C.textMuted }}
                                        >
                                            작성한 리뷰가 없습니다.
                                        </p>
                                    )}

                                    {written.map((review) => (
                                        <div
                                            key={review.reviewId}
                                            className="flex gap-4 py-5"
                                            style={{
                                                borderBottom: `1px solid ${C.panelBorder}`,
                                            }}
                                        >
                                            <div className="flex-1">
                                                <div style={{ color: C.text }}>
                                                    {"★".repeat(review.rating)}{" "}
                                                    <span style={{ color: C.textMuted }}>
                                                        {review.productCode ??
                                                            `상품 #${review.productId}`}
                                                    </span>
                                                </div>

                                                <p
                                                    className="mt-2 text-xs"
                                                    style={{ color: C.textDim }}
                                                >
                                                    {review.content}
                                                </p>

                                                <div
                                                    className="mt-2 text-[11px]"
                                                    style={{ color: C.textMuted }}
                                                >
                                                    {date(review.createdAt)}
                                                </div>
                                            </div>

                                            <button
                                                onClick={() =>
                                                    void deleteReview(review.reviewId)
                                                }
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

                    {reviewView === "write" && selected && (
                        <>
                            <button
                                onClick={() => setReviewView("list")}
                                className="text-xs mb-5"
                                style={{ color: C.textMuted }}
                            >
                                {section === "orders"
                                    ? "← 주문내역으로"
                                    : "← 리뷰 목록으로"}
                            </button>

                            <PageTitle
                                note={`// REVIEWS / WRITE · ${selected.productCode}`}
                            >
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
                                                color:
                                                    value <= rating
                                                        ? C.redBright
                                                        : C.textMuted,
                                            }}
                                        >
                                            ★
                                        </button>
                                    ))}
                                </div>

                                <p
                                    className="mb-5 text-xs"
                                    style={{ color: C.textMuted }}
                                >
                                    선택한 별점: {rating} / 5
                                </p>

                                <textarea
                                    value={content}
                                    maxLength={500}
                                    onChange={(event) =>
                                        setContent(event.target.value)
                                    }
                                    placeholder="리뷰 내용을 입력해 주세요."
                                    className="w-full min-h-40 px-4 py-4 text-sm outline-none"
                                    style={{
                                        background: "#120000",
                                        color: C.text,
                                        border: `1px solid ${C.panelBorder}`,
                                    }}
                                />

                                <div
                                    className="mt-2 text-right text-xs"
                                    style={{ color: C.textMuted }}
                                >
                                    {content.length} / 500
                                </div>

                                {error && (
                                    <p
                                        className="mt-3 text-xs"
                                        style={{ color: C.redBright }}
                                    >
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

            {imageModalOpen && previewUrl && (
                <div
                    role="presentation"
                    onClick={() => setImageModalOpen(false)}
                    className="fixed inset-0 z-50 flex items-center justify-center p-6"
                    style={{ background: "rgba(0, 0, 0, 0.86)" }}
                >
                    <div
                        role="dialog"
                        aria-modal="true"
                        aria-label="프로필 이미지 크게 보기"
                        onClick={(event) => event.stopPropagation()}
                        className="relative max-w-[90vw] max-h-[90vh]"
                    >
                        <button
                            type="button"
                            onClick={() => setImageModalOpen(false)}
                            className="absolute -top-10 right-0 text-sm"
                            style={{ color: "#fff" }}
                        >
                            닫기 ✕
                        </button>

                        <img
                            src={previewUrl}
                            alt="프로필 크게 보기"
                            className="max-w-[90vw] max-h-[80vh] object-contain"
                        />
                    </div>
                </div>
            )}
        </div>
    );
}