import { useEffect, useState } from "react";
import {
  addCartItem,
  deleteCartItem,
  fetchCartItems,
  updateCartItemQuantity,
  type CartItemDetailData,
} from "./api/cart";
import {
  fetchPopularSearches,
  fetchProductDetail,
  fetchProductList,
  searchProducts,
  PAGE_SIZE,
  type ApiProduct,
  type ApiProductSort,
  type PopularSearch,
  type ProductDetailData,
} from "./api/products";
import { NAV_ITEMS, SUBCATS, type Tier } from "./catalog";
import { Gate } from "./components/auth/Gate";
import { LoginModal } from "./components/auth/LoginModal";
import { CartView } from "./components/cart/CartView";
import { FloatingChatWidget } from "./components/chat/FloatingChatWidget";
import { Spinner } from "./components/common/Spinner";
import { CodeTab } from "./components/member/CodeTab";
import { MyPage } from "./components/member/MyPage";
import { TierBadge } from "./components/member/TierBadge";
import { TierProgress } from "./components/member/TierProgress";
import { CheckoutView } from "./components/order/CheckoutView";
import { OrderDone } from "./components/order/OrderDone";
import { ProductCard } from "./components/product/ProductCard";
import { ProductDetail } from "./components/product/ProductDetail";
import { Sidebar } from "./components/product/Sidebar";
import { ACCOUNTS } from "./lib/accounts";
import { drop, read, SESSION_KEY, write } from "./lib/storage";
import { C } from "./lib/theme";
import { canAccess, TIERS, tierFor } from "./lib/tier";

/* ─── 세션 ───────────────────────────────────────────────── */
/* 등급은 저장하지 않는다. spent 에서 계산하므로 저장하면 두 값이 어긋난다. */
type Session = {
  id: string;
  spent: number;
};

/* ─── 주문 ───────────────────────────────────────────────── */
type CartLine = { id: string; qty: number };

function toCartProduct(item: CartItemDetailData): ApiProduct {
  return {
    productId: item.productId,
    id: item.productCode,
    name: item.name,
    category: item.category,
    sub: item.subCategory,
    price: item.price,
    tier: item.tier,
    img: item.imageUrl,
    desc: "",
    specs: [],
  };
}

/* 장바구니 담기와 결제는 로그인이 필요하다. 비로그인 상태에서 누른 동작을
   여기에 담아 두었다가 로그인에 성공하면 이어서 실행한다. */
type Pending =
  | { kind: "add"; id: string; qty: number }
  | { kind: "buy"; id: string; qty: number }
  | { kind: "checkout" };

/* ─── 화면 ───────────────────────────────────────────────── */
type View =
  | { name: "list" }
  | { name: "detail"; id: string }
  | { name: "cart" }
  | { name: "checkout" }
  | { name: "done"; orderNo: string; total: number }
  | { name: "mypage" };

/* ─── main app ───────────────────────────────────────────── */
export default function App() {
  const [session, setSession] = useState<Session | null>(() => read<Session | null>(SESSION_KEY, null));
  const [cart, setCart] = useState<CartLine[]>([]);
  const [cartItemIds, setCartItemIds] = useState<Record<string, number>>({});
  const [cartLoading, setCartLoading] = useState(false);
  const [cartError, setCartError] = useState<string | null>(null);
  const [cartReloadKey, setCartReloadKey] = useState(0);
  const [cartPendingIds, setCartPendingIds] = useState<Set<string>>(() => new Set());
  const [view, setView] = useState<View>({ name: "list" });
  const [showLogin, setShowLogin] = useState(false);
  /* 로그인이 필요해 막힌 동작. 로그인에 성공하면 이어서 실행한다 */
  const [afterLogin, setAfterLogin] = useState<Pending | null>(null);
  const [activeCodeTab, setActiveCodeTab] = useState<Tier>(() => {
    const saved = read<Session | null>(SESSION_KEY, null);
    if (!saved) return "red";
    const tier = tierFor(saved.spent, saved.id);
    return tier === "green" ? "red" : tier;
  });

  /* 등급은 저장된 값이 아니라 누적 구매금액에서 계산한다 */
  const userTier: Tier | null = session ? tierFor(session.spent, session.id) : null;
  const [activeNav, setActiveNav] = useState("Guns");
  const [activeSub, setActiveSub] = useState("전체");
  const [menuOpen, setMenuOpen] = useState(false);

  /* ── 상품 목록/검색/상세 (백엔드 연동) ──
     상품 코드는 기존 화면과 장바구니 식별자로 유지하고,
     상세조회에는 목록 API가 내려준 DB 상품 ID(productId)를 사용한다. */
  const [productCache, setProductCache] = useState<Record<string, ApiProduct>>({});
  const [listItems, setListItems] = useState<ApiProduct[]>([]);
  const [listPage, setListPage] = useState(0);
  const [listHasNext, setListHasNext] = useState(false);
  const [listTotal, setListTotal] = useState(0);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState(false);
  const [sortKey, setSortKey] = useState<ApiProductSort>("POPULAR");
  const [searchInput, setSearchInput] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [searchFocused, setSearchFocused] = useState(false);
  const [popularSearches, setPopularSearches] = useState<PopularSearch[]>([]);
  const [detailProduct, setDetailProduct] = useState<ProductDetailData | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detailReloadKey, setDetailReloadKey] = useState(0);

  function cacheProducts(items: ApiProduct[]) {
    setProductCache((prev) => {
      const next = { ...prev };
      for (const p of items) next[p.id] = p;
      return next;
    });
  }

  /* 검색창 입력을 살짝 늦춰서 반영한다(타이핑마다 API를 호출하지 않도록) */
  useEffect(() => {
    const timer = window.setTimeout(() => setSearchKeyword(searchInput.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    if (!searchFocused || searchInput.trim()) return;

    let cancelled = false;
    fetchPopularSearches()
      .then((searches) => {
        if (!cancelled) setPopularSearches(searches);
      })
      .catch(() => {
        if (!cancelled) setPopularSearches([]);
      });

    return () => {
      cancelled = true;
    };
  }, [searchFocused, searchInput]);

  const isSearching = searchKeyword.length > 0;

  function requestPage(page: number) {
    return isSearching
      ? searchProducts({
          keyword: searchKeyword,
          tier: activeCodeTab,
          sort: sortKey,
          page,
          size: PAGE_SIZE,
        })
      : fetchProductList({
          category: activeNav,
          subCategory: activeSub === "전체" ? undefined : activeSub,
          tier: activeCodeTab,
          sort: sortKey,
          page,
          size: PAGE_SIZE,
        });
  }

  /* 카테고리·등급 탭·정렬·검색어가 바뀌면 1페이지부터 새로 불러온다 */
  useEffect(() => {
    if (!session || !userTier || view.name !== "list") return;

    let cancelled = false;
    setListLoading(true);
    setListError(false);

    requestPage(0)
      .then((res) => {
        if (cancelled) return;
        setListItems(res.items);
        setListPage(0);
        setListHasNext(res.hasNext);
        setListTotal(res.totalElements);
        cacheProducts(res.items);
      })
      .catch(() => {
        if (cancelled) return;
        setListItems([]);
        setListHasNext(false);
        setListTotal(0);
        setListError(true);
      })
      .finally(() => {
        if (!cancelled) setListLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session, userTier, view.name, activeNav, activeSub, activeCodeTab, sortKey, isSearching, searchKeyword]);

  function loadMoreProducts() {
    if (!userTier || !listHasNext || listLoading) return;
    const nextPage = listPage + 1;
    setListLoading(true);

    requestPage(nextPage)
      .then((res) => {
        setListItems((prev) => [...prev, ...res.items]);
        setListPage(nextPage);
        setListHasNext(res.hasNext);
        cacheProducts(res.items);
      })
      .finally(() => setListLoading(false));
  }

  const detailProductId = view.name === "detail" ? Number(view.id) : null;

  useEffect(() => {
    if (!session || !userTier || detailProductId === null) {
      setDetailProduct(null);
      setDetailLoading(false);
      setDetailError(null);
      return;
    }

    if (!Number.isSafeInteger(detailProductId) || detailProductId <= 0) {
      setDetailProduct(null);
      setDetailLoading(false);
      setDetailError("올바르지 않은 상품 ID입니다.");
      return;
    }

    const controller = new AbortController();
    setDetailProduct(null);
    setDetailLoading(true);
    setDetailError(null);

    fetchProductDetail(detailProductId, controller.signal)
      .then((product) => {
        setDetailProduct(product);
        cacheProducts([product]);
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setDetailError(error instanceof Error ? error.message : "상품 상세정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setDetailLoading(false);
      });

    return () => controller.abort();
    // cacheProducts는 상태 갱신 헬퍼이므로 상세조회 재실행 조건에서 제외한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session, userTier, detailProductId, detailReloadKey]);

  /* 장바구니는 로그인한 회원의 서버 데이터로 구성한다. */
  useEffect(() => {
    if (!session) {
      setCart([]);
      setCartItemIds({});
      setCartLoading(false);
      setCartError(null);
      return;
    }

    let cancelled = false;
    setCartLoading(true);
    setCartError(null);

    fetchCartItems()
      .then((items) => {
        if (cancelled) return;

        const products = items.map(toCartProduct);
        setProductCache((prev) => {
          const next = { ...prev };
          for (const product of products) next[product.id] = product;
          return next;
        });
        setCart(items.map((item) => ({ id: item.productCode, qty: item.quantity })));
        setCartItemIds(
          Object.fromEntries(items.map((item) => [item.productCode, item.id]))
        );
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setCart([]);
        setCartItemIds({});
        setCartError(error instanceof Error ? error.message : "장바구니를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setCartLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [session?.id, cartReloadKey]);

  /* 누적 구매금액이 올라가도 같은 자리에서 저장된다 */
  useEffect(() => {
    if (session) write(SESSION_KEY, session);
    else drop(SESSION_KEY);
  }, [session]);

  /* ── 브라우저 히스토리 ──
     화면 전환을 히스토리에 남겨 뒤로/앞으로 가기(마우스 옆 버튼 포함)가 동작하게 한다. */
  useEffect(() => {
    window.history.replaceState({ view: { name: "list" } }, "");
    function onPop(e: PopStateEvent) {
      const saved = (e.state as { view?: View } | null)?.view;
      setView(saved ?? { name: "list" });
      window.scrollTo({ top: 0 });
    }
    window.addEventListener("popstate", onPop);
    return () => window.removeEventListener("popstate", onPop);
  }, []);

  /* 같은 화면으로의 이동은 히스토리에 쌓지 않는다 — 뒤로 가기를 여러 번 눌러야 하는 것을 막는다 */
  function sameView(a: View, b: View) {
    if (a.name !== b.name) return false;
    if (a.name === "detail" && b.name === "detail") return a.id === b.id;
    return true;
  }

  function navigate(next: View, replace = false) {
    if (!sameView(view, next)) {
      if (replace) window.history.replaceState({ view: next }, "");
      else window.history.pushState({ view: next }, "");
    }
    setView(next);
    window.scrollTo({ top: 0 });
  }

  /* 로고 클릭 — 목록으로 돌아가면서 카테고리·서브카테고리도 처음 상태로 되돌린다 */
  function goHome() {
    setActiveNav("Guns");
    setActiveSub("전체");
    setMenuOpen(false);
    setSearchInput("");
    navigate({ name: "list" });
  }

  function openCart() {
    setCartReloadKey((key) => key + 1);
    navigate({ name: "cart" });
  }

  function handleLogin(id: string, spent: number) {
    setSession({ id, spent });
    const tier = tierFor(spent, id);
    setActiveCodeTab(tier === "green" ? "red" : tier);
    setShowLogin(false);

    /* 서버 장바구니는 session 변경을 감지한 조회 effect에서 불러온다. */
    setCart([]);
    setCartItemIds({});

    /* 로그인 직전에 막혔던 동작을 이어서 실행한다.
       이 시점에는 session 이 아직 갱신 전이라 로그인 검사를 다시 하지 않는다. */
    const pending = afterLogin;
    setAfterLogin(null);
    if (!pending) return;

    if (pending.kind === "checkout") {
      navigate({ name: "checkout" });
      return;
    }
    putInCart(pending.id, pending.qty);
    if (pending.kind === "buy") navigate({ name: "checkout" });
  }

  function handleLogout() {
    setSession(null);
    setActiveCodeTab("red");
    setCart([]);
    setCartItemIds({});
    setCartError(null);
    navigate({ name: "list" });
  }

  function changeNav(cat: string) {
    setActiveNav(cat);
    setActiveSub("전체");
    setMenuOpen(false);
    setSearchInput("");
    navigate({ name: "list" });
  }

  /* ── 장바구니 ── */
  const cartLines = cart
    .map((line) => {
      const p = productCache[line.id];
      return p ? { p, qty: line.qty } : null;
    })
    .filter((l): l is { p: ApiProduct; qty: number } => l !== null);

  const cartCount = cart.reduce((sum, l) => sum + l.qty, 0);

  function putInCart(id: string, qty: number) {
    setCart((prev) => {
      const found = prev.find((l) => l.id === id);
      if (found) return prev.map((l) => (l.id === id ? { ...l, qty: Math.min(99, l.qty + qty) } : l));
      return [...prev, { id, qty }];
    });
  }

  /* 서버 장바구니에 담긴 경우에만 화면 장바구니에도 반영한다. */
  async function addToCart(id: string, qty: number): Promise<boolean> {
    if (!userTier) {
      setAfterLogin({ kind: "add", id, qty });
      setShowLogin(true);
      return false;
    }
    /* 등급이 모자라면 담을 수 없다 */
    const p = productCache[id];
    if (!p || !canAccess(userTier, p.tier)) return false;

    const savedItem = await addCartItem(p.productId, qty);
    setCartItemIds((prev) => ({ ...prev, [id]: savedItem.id }));
    setCart((prev) => {
      const found = prev.some((line) => line.id === id);
      if (found) {
        return prev.map((line) =>
          line.id === id ? { ...line, qty: savedItem.quantity } : line
        );
      }
      return [...prev, { id, qty: savedItem.quantity }];
    });

    return true;
  }

  async function setQty(id: string, qty: number) {
    const cartItemId = cartItemIds[id];
    if (cartItemId === undefined || cartPendingIds.has(id)) return;

    setCartPendingIds((prev) => new Set(prev).add(id));
    setCartError(null);

    try {
      const updatedItem = await updateCartItemQuantity(cartItemId, qty);
      setCart((prev) =>
        prev.map((line) => line.id === id ? { ...line, qty: updatedItem.quantity } : line)
      );
    } catch (error) {
      setCartError(error instanceof Error ? error.message : "장바구니 수량을 변경하지 못했습니다.");
    } finally {
      setCartPendingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }

  async function removeLine(id: string) {
    const cartItemId = cartItemIds[id];
    if (cartItemId === undefined || cartPendingIds.has(id)) return;

    setCartPendingIds((prev) => new Set(prev).add(id));
    setCartError(null);

    try {
      await deleteCartItem(cartItemId);
      setCart((prev) => prev.filter((line) => line.id !== id));
      setCartItemIds((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    } catch (error) {
      setCartError(error instanceof Error ? error.message : "장바구니 상품을 삭제하지 못했습니다.");
    } finally {
      setCartPendingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }

  function goCheckout() {
    if (cartLines.length === 0) return;
    if (!userTier) {
      setAfterLogin({ kind: "checkout" });
      setShowLogin(true);
      return;
    }
    navigate({ name: "checkout" });
  }

  function buyNow(id: string, qty: number) {
    if (!userTier) {
      setAfterLogin({ kind: "buy", id, qty });
      setShowLogin(true);
      return;
    }
    const p = productCache[id];
    if (!p || !canAccess(userTier, p.tier)) return;
    putInCart(id, qty);
    navigate({ name: "checkout" });
  }

  function finishOrder(orderNo: string, total: number) {
    setCart([]);

    /* 결제금액을 누적해 등급을 다시 계산한다. 서버가 붙으면 이 누적은
       서버가 하고 응답으로 내려주는 값을 쓰게 된다. */
    if (session) {
      const spent = session.spent + total;
      ACCOUNTS[session.id].spent = spent;
      setSession({ ...session, spent });
      if (tierFor(spent, session.id) !== tierFor(session.spent, session.id)) {
        const newTier = tierFor(spent, session.id);
        setActiveCodeTab(newTier === "green" ? "red" : newTier);
      }
    }

    /* 주문서를 완료 화면으로 대체한다 — 뒤로 가기로 비워진 주문서에 돌아가지 않도록 */
    navigate({ name: "done", orderNo, total }, true);
  }

  /* 카테고리·등급 탭·검색 필터링은 백엔드(/api/products, /api/v1/products/search)가
     이미 적용해서 내려준다. listItems는 그 결과를 그대로 담는다. */
  /* 뒤로 가기로 예전 세션의 상위 등급 상품에 돌아올 수 있으므로 여기서도 막는다 */
  const detailAllowed = detailProduct !== null && canAccess(userTier, detailProduct.tier);
  const onListPage = view.name === "list";

  return (
    <div className="min-h-full" style={{ background: C.bg, color: C.text, fontFamily: "Noto Sans KR, sans-serif", minHeight: "100vh" }}>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>

      {showLogin && (
        <LoginModal onLogin={handleLogin} onClose={() => { setShowLogin(false); setAfterLogin(null); }} />
      )}

      {/* ── HEADER ─────────────────────────────────── */}
      <header
        className="sticky top-0 z-50"
        style={{ background: "rgba(12,0,0,0.93)", backdropFilter: "blur(8px)", borderBottom: `1px solid ${C.panelBorder}` }}
      >
        <div className="max-w-[1280px] mx-auto px-4 md:px-8">
          <div className="flex items-center justify-between h-14">
            {/* Logo */}
            <button className="flex items-center gap-3" onClick={goHome}>
              <div className="w-7 h-7 flex items-center justify-center text-[10px] font-black"
                style={{ background: C.red, color: "#fff", fontFamily: "Cinzel, serif" }}>
                MH
              </div>
              <span className="font-bold uppercase tracking-wider text-base" style={{ fontFamily: "Cinzel, serif" }}>
                Murder<span style={{ color: C.redBright }}>Help</span>
              </span>
            </button>

            {/* Desktop nav */}
            <nav className="hidden md:flex items-center h-full">
              {NAV_ITEMS.map((item, i) => {
                const active = onListPage && activeNav === item;
                return (
                  <button
                    key={item}
                    onClick={() => changeNav(item)}
                    className="px-4 h-14 text-sm font-semibold uppercase tracking-widest transition-colors relative"
                    style={{
                      fontFamily: "Cinzel, serif",
                      fontSize: 12,
                      color: active ? C.text : C.textDim,
                      borderBottom: active ? `2px solid ${C.redBright}` : "2px solid transparent",
                    }}
                    onMouseEnter={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.color = C.text; }}
                    onMouseLeave={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.color = C.textDim; }}
                  >
                    {item}
                    {i < NAV_ITEMS.length - 1 && (
                      <span className="absolute right-0 top-1/2 -translate-y-1/2 text-xs" style={{ color: C.redDim }}>|</span>
                    )}
                  </button>
                );
              })}
            </nav>

            {/* Right actions */}
            <div className="flex items-center gap-3">
              {/* search */}
              <div className="relative hidden md:flex items-center gap-2 px-3 py-1.5 text-xs"
                style={{ background: "rgba(0,0,0,0.5)", border: `1px solid ${C.panelBorder}` }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke={C.textMuted} strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  onFocus={() => setSearchFocused(true)}
                  onBlur={() => setSearchFocused(false)}
                  placeholder="검색..."
                  className="bg-transparent outline-none w-20 text-xs"
                  style={{ color: C.textDim, fontFamily: "Noto Sans KR" }}
                />
                {searchFocused && !searchInput.trim() && popularSearches.length > 0 && (
                  <div
                    className="absolute top-full left-0 z-50 mt-2 w-52 p-2"
                    style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}
                  >
                    <p className="px-2 py-1 text-[10px] tracking-widest" style={{ color: C.red, fontFamily: "Share Tech Mono" }}>
                      POPULAR SEARCHES
                    </p>
                    {popularSearches.map((search) => (
                      <button
                        key={search.keyword}
                        type="button"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => {
                          setSearchInput(search.keyword);
                          setSearchFocused(false);
                        }}
                        className="flex w-full items-center gap-2 px-2 py-1.5 text-left text-xs"
                        style={{ color: C.textDim }}
                      >
                        <span style={{ color: C.red, fontFamily: "Share Tech Mono" }}>{search.rank}</span>
                        <span className="truncate">{search.keyword}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* cart */}
              <button
                onClick={openCart}
                className="relative flex items-center justify-center transition-all"
                style={{ width: 34, height: 30, border: `1px solid ${C.panelBorder}`, color: C.textDim }}
                aria-label="장바구니"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                  <circle cx="9" cy="20" r="1.4" /><circle cx="18" cy="20" r="1.4" />
                  <path d="M2 3h3l2.4 12.2a1.5 1.5 0 0 0 1.5 1.2h8.6a1.5 1.5 0 0 0 1.5-1.2L21 7H6" />
                </svg>
                {cartCount > 0 && (
                  <span
                    className="absolute -top-1.5 -right-1.5 text-[9px] font-bold flex items-center justify-center"
                    style={{
                      minWidth: 16, height: 16, padding: "0 3px",
                      background: C.red, color: "#fff", fontFamily: "Share Tech Mono",
                    }}
                  >
                    {cartCount}
                  </span>
                )}
              </button>

              {/* auth */}
              {userTier ? (
                <div className="flex items-center gap-2">
                  <TierBadge tier={userTier} />
                  <button
                    onClick={handleLogout}
                    className="text-xs px-3 py-1.5 uppercase tracking-wider transition-all"
                    style={{ border: `1px solid ${C.panelBorder}`, color: C.textMuted, fontFamily: "Share Tech Mono" }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.color = C.text; (e.currentTarget as HTMLButtonElement).style.borderColor = C.red; }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.color = C.textMuted; (e.currentTarget as HTMLButtonElement).style.borderColor = C.panelBorder; }}
                  >
                    Logout
                  </button>
                  <button
                    onClick={() => navigate({ name: "mypage" })}
                    className="text-xs px-3 py-1.5 uppercase tracking-wider transition-all"
                    style={{
                      border: `1px solid ${view.name === "mypage" ? C.red : C.panelBorder}`,
                      color: view.name === "mypage" ? C.text : C.textDim,
                      fontFamily: "Share Tech Mono",
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.color = C.text; (e.currentTarget as HTMLButtonElement).style.borderColor = C.red; }}
                    onMouseLeave={(e) => {
                      const on = view.name === "mypage";
                      (e.currentTarget as HTMLButtonElement).style.color = on ? C.text : C.textDim;
                      (e.currentTarget as HTMLButtonElement).style.borderColor = on ? C.red : C.panelBorder;
                    }}
                  >
                    마이페이지
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setShowLogin(true)}
                  className="text-xs font-bold px-4 py-1.5 uppercase tracking-wider transition-all"
                  style={{ background: C.red, color: "#fff", fontFamily: "Share Tech Mono" }}
                  onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = C.redBright; }}
                  onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = C.red; }}
                >
                  Login
                </button>
              )}

              {/* mobile menu */}
              <button className="md:hidden" style={{ color: C.textDim }} onClick={() => setMenuOpen(!menuOpen)}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        {menuOpen && (
          <div style={{ background: "rgba(12,0,0,0.98)", borderTop: `1px solid ${C.panelBorder}` }}>
            {NAV_ITEMS.map((item) => (
              <button key={item} onClick={() => changeNav(item)}
                className="block w-full text-left px-6 py-3 text-sm uppercase tracking-widest"
                style={{ fontFamily: "Cinzel, serif", color: activeNav === item ? C.redBright : C.textDim, borderBottom: `1px solid ${C.panelBorder}` }}>
                {item}
              </button>
            ))}
          </div>
        )}
      </header>

      {/* 로그인 전에는 상품을 일절 보여주지 않는다 */}
      {!session && <Gate onLogin={() => setShowLogin(true)} />}

      {session && onListPage && (
        <>
          {/* ── CODE TABS ──────────────────────────────── */}
          <div className="flex" style={{ background: "rgba(0,0,0,0.55)", borderBottom: `1px solid ${C.panelBorder}` }}>
            {(["red", "purple", "yellow"] as Tier[]).map((tier) => (
              <CodeTab
                key={tier}
                tier={tier}
                active={activeCodeTab === tier}
                userTier={userTier}
                onClick={() => setActiveCodeTab(tier)}
              />
            ))}
          </div>

          {/* ── TIER INFO BANNER ────────────────────────── */}
          {session && userTier && (
            <div
              className="py-2.5 px-6 flex items-center gap-4 flex-wrap"
              style={{
                background: `${TIERS[userTier].color}18`,
                borderBottom: `1px solid ${TIERS[userTier].color}44`,
              }}
            >
              <TierBadge tier={userTier} />
              <span className="text-xs" style={{ color: C.textDim, fontFamily: "Share Tech Mono" }}>
                {TIERS[userTier].desc}
              </span>
              <div className="flex-1" style={{ minWidth: 220, maxWidth: 420 }}>
                <TierProgress spent={session.spent} />
              </div>
            </div>
          )}

          {/* ── HERO ────────────────────────────────────── */}
          <div className="relative overflow-hidden" style={{ minHeight: 180 }}>
            <div
              className="absolute inset-0"
              style={{
                backgroundImage: `url(https://images.unsplash.com/photo-1687349150019-003d3ea38d79?w=1400&h=250&fit=crop&auto=format)`,
                backgroundSize: "cover",
                backgroundPosition: "center 30%",
                filter: "brightness(0.18) saturate(0.3)",
              }}
            />
            <div className="absolute inset-0" style={{ background: "linear-gradient(to right, rgba(15,0,0,0.95) 0%, transparent 50%, rgba(15,0,0,0.95) 100%)" }} />
            <div className="relative max-w-[1280px] mx-auto px-4 md:px-8 py-10">
              <div className="text-xs uppercase tracking-[0.3em] mb-2" style={{ color: TIERS[activeCodeTab].color, fontFamily: "Share Tech Mono" }}>
                // {TIERS[activeCodeTab].label.toUpperCase()} — {TIERS[activeCodeTab].desc}
              </div>
              <h1
                className="font-bold uppercase leading-none mb-2"
                style={{
                  fontFamily: "Cinzel, serif",
                  fontSize: "clamp(22px,4vw,48px)",
                  color: C.text,
                  textShadow: `0 0 30px ${TIERS[activeCodeTab].color}55`,
                }}
              >
                {activeCodeTab === "red" && "VIP PREMIUM COLLECTION"}
                {activeCodeTab === "purple" && "MID-TIER TACTICAL GEAR"}
                {activeCodeTab === "yellow" && "ENTRY GRADE ARSENAL"}
              </h1>
              <p className="text-sm" style={{ color: C.textDim, fontFamily: "Noto Sans KR" }}>
                {TIERS[activeCodeTab].priceRange} 범위 · {activeNav} 카테고리
              </p>
            </div>
          </div>

          {/* ── MAIN ────────────────────────────────────── */}
          <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-6">
            <div className="flex gap-0" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
              <Sidebar category={activeNav} activeSub={activeSub} onSub={setActiveSub} />

              <div className="flex-1 p-5">
                {/* mobile subcats */}
                <div className="flex md:hidden gap-2 flex-wrap mb-4">
                  {(SUBCATS[activeNav] ?? []).map((s) => (
                    <button key={s} onClick={() => setActiveSub(s)}
                      className="text-[10px] uppercase tracking-widest px-2.5 py-1 transition-all"
                      style={{
                        fontFamily: "Share Tech Mono",
                        background: activeSub === s ? C.red : "rgba(0,0,0,0.5)",
                        color: activeSub === s ? "#fff" : C.textMuted,
                        border: `1px solid ${activeSub === s ? C.red : C.panelBorder}`,
                      }}
                    >
                      {s}
                    </button>
                  ))}
                </div>

                {/* sort row */}
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <span className="text-xs uppercase tracking-widest" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      {isSearching ? `검색: ${searchKeyword}` : activeSub === "전체" ? activeNav : activeSub}
                    </span>
                    <TierBadge tier={activeCodeTab} small />
                    <span
                      className="text-xs px-1.5 py-0.5"
                      style={{ background: "rgba(200,30,0,0.12)", color: C.red, fontFamily: "Share Tech Mono", border: `1px solid ${C.redDim}` }}
                    >
                      {listTotal}
                    </span>
                    {listLoading && <Spinner color={C.textMuted} />}
                  </div>
                  <select
                    value={sortKey}
                    onChange={(e) => setSortKey(e.target.value as ApiProductSort)}
                    className="text-[10px] uppercase tracking-wider px-2 py-1 outline-none"
                    style={{ background: "rgba(0,0,0,0.5)", color: C.textMuted, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
                  >
                    <option value="POPULAR">POPULAR</option>
                    <option value="PRICE_ASC">PRICE ↑</option>
                    <option value="PRICE_DESC">PRICE ↓</option>
                    <option value="NEWEST">NEWEST</option>
                  </select>
                </div>

                {/* grid */}
                {listError ? (
                  <div className="flex flex-col items-center justify-center py-16" style={{ border: `1px dashed ${C.panelBorder}` }}>
                    <div className="text-3xl font-bold uppercase mb-2" style={{ fontFamily: "Cinzel, serif", color: C.redDim }}>
                      LOAD FAILED
                    </div>
                    <div className="text-xs" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      // 상품을 불러오지 못했습니다. 백엔드 서버 상태를 확인해 주세요.
                    </div>
                  </div>
                ) : listItems.length > 0 ? (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                    {listItems.map((p) => (
                      <ProductCard
                        key={p.id}
                        p={p}
                        onOpen={() => navigate({ name: "detail", id: String(p.productId) })}
                      />
                    ))}
                  </div>
                ) : !listLoading ? (
                  <div className="flex flex-col items-center justify-center py-16" style={{ border: `1px dashed ${C.panelBorder}` }}>
                    <div className="text-3xl font-bold uppercase mb-2" style={{ fontFamily: "Cinzel, serif", color: C.redDim }}>
                      NO ITEMS
                    </div>
                    <div className="text-xs" style={{ color: C.textMuted, fontFamily: "Share Tech Mono" }}>
                      // {isSearching ? `"${searchKeyword}"` : activeSub} — {TIERS[activeCodeTab].label} 등급 아이템 없음
                    </div>
                  </div>
                ) : null}

                {listHasNext && (
                  <div className="text-center mt-8">
                    <button
                      onClick={loadMoreProducts}
                      disabled={listLoading}
                      className="px-10 py-2.5 text-xs font-bold uppercase tracking-widest transition-all"
                      style={{ border: `1px solid ${C.panelBorder}`, color: C.textDim, fontFamily: "Share Tech Mono", cursor: listLoading ? "wait" : "pointer" }}
                      onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.borderColor = C.red; (e.currentTarget as HTMLButtonElement).style.color = C.text; }}
                      onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.borderColor = C.panelBorder; (e.currentTarget as HTMLButtonElement).style.color = C.textDim; }}
                    >
                      {listLoading ? "불러오는 중…" : "더 보기 →"}
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      {session && view.name === "detail" && detailLoading && (
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-20">
          <div
            className="flex items-center justify-center gap-3 py-20"
            style={{ background: C.panel, border: `1px solid ${C.panelBorder}`, color: C.textDim }}
          >
            <Spinner color={C.redBright} />
            <span className="text-xs" style={{ fontFamily: "Share Tech Mono" }}>
              상품 상세정보를 불러오는 중...
            </span>
          </div>
        </div>
      )}

      {session && view.name === "detail" && !detailLoading && detailError && (
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-20">
          <div
            className="max-w-md mx-auto p-10 text-center"
            style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}
          >
            <div className="text-xl font-bold uppercase mb-3" style={{ fontFamily: "Cinzel, serif", color: C.text }}>
              Load Failed
            </div>
            <p className="text-sm leading-relaxed mb-6" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
              {detailError}
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => navigate({ name: "list" })}
                className="flex-1 py-3 text-sm font-bold uppercase tracking-widest"
                style={{ color: C.textDim, border: `1px solid ${C.panelBorder}`, fontFamily: "Share Tech Mono" }}
              >
                목록으로
              </button>
              <button
                onClick={() => setDetailReloadKey((key) => key + 1)}
                className="flex-1 py-3 text-sm font-bold uppercase tracking-widest"
                style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
              >
                다시 시도
              </button>
            </div>
          </div>
        </div>
      )}

      {session && view.name === "detail" && detailProduct && detailAllowed && (
        <ProductDetail
          p={detailProduct}
          onBack={() => navigate({ name: "list" })}
          onAddToCart={(qty) => addToCart(detailProduct.id, qty)}
          onBuyNow={(qty) => buyNow(detailProduct.id, qty)}
        />
      )}

      {session && view.name === "detail" && detailProduct && !detailAllowed && (
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-20">
          <div className="max-w-md mx-auto p-10 text-center" style={{ background: C.panel, border: `1px solid ${C.panelBorder}` }}>
            <div className="text-xl font-bold uppercase mb-3" style={{ fontFamily: "Cinzel, serif", color: C.text }}>
              Locked
            </div>
            <p className="text-sm leading-relaxed mb-6" style={{ color: C.textDim, fontFamily: "Noto Sans KR, sans-serif", fontWeight: 300 }}>
              {TIERS[detailProduct.tier].label} 등급부터 보실 수 있는 상품입니다.
            </p>
            <button
              onClick={() => navigate({ name: "list" })}
              className="w-full py-3 text-sm font-bold uppercase tracking-widest"
              style={{ background: C.red, color: "#fff", border: `1px solid ${C.redBright}`, fontFamily: "Share Tech Mono" }}
            >
              목록으로 →
            </button>
          </div>
        </div>
      )}

      {session && view.name === "mypage" && (
        <MyPage onBack={() => navigate({ name: "list" })} />
      )}

      {session && view.name === "cart" && (
        <CartView
          lines={cartLines}
          loading={cartLoading}
          error={cartError}
          pendingIds={cartPendingIds}
          onQty={setQty}
          onRemove={removeLine}
          onRetry={() => setCartReloadKey((key) => key + 1)}
          onContinue={() => navigate({ name: "list" })}
          onCheckout={goCheckout}
        />
      )}

      {session && view.name === "checkout" && (
        <CheckoutView
          lines={cartLines}
          userTier={userTier}
          onBack={() => navigate({ name: "cart" })}
          onDone={finishOrder}
        />
      )}

      {session && view.name === "done" && (
        <OrderDone orderNo={view.orderNo} total={view.total} onHome={() => navigate({ name: "list" })} />
      )}

      {/* ── FOOTER ──────────────────────────────────── */}
      <footer className="mt-6 border-t" style={{ borderColor: C.panelBorder, background: "rgba(0,0,0,0.7)" }}>
        <div className="max-w-[1280px] mx-auto px-4 md:px-8 py-10">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
            {[
              { h: "SHOP", links: ["Guns", "Weapons", "Bombs", "Gear", "Ammo"] },
              { h: "SUPPORT", links: ["주문 조회", "반품/교환", "보증", "고객센터", "FAQ"] },
              { h: "MEMBERSHIP", links: ["Code Red", "Code Purple", "Code Yellow", "등급 안내", "혜택 비교"] },
              { h: "COMPANY", links: ["About", "Blog", "Careers", "Legal"] },
            ].map((col) => (
              <div key={col.h}>
                <div className="text-[10px] uppercase tracking-[0.2em] mb-3 font-semibold"
                  style={{ color: C.red, fontFamily: "Share Tech Mono" }}>
                  {col.h}
                </div>
                <ul className="space-y-1.5">
                  {col.links.map((l) => (
                    <li key={l}>
                      <a href="#" className="text-xs transition-colors"
                        style={{ color: C.textMuted, fontFamily: "Noto Sans KR" }}
                        onMouseEnter={(e) => { (e.currentTarget as HTMLAnchorElement).style.color = C.text; }}
                        onMouseLeave={(e) => { (e.currentTarget as HTMLAnchorElement).style.color = C.textMuted; }}>
                        {l}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          <div
            className="flex flex-col md:flex-row items-start md:items-center justify-between gap-2 pt-6 border-t text-[10px]"
            style={{ borderColor: C.panelBorder, color: C.textMuted, fontFamily: "Share Tech Mono" }}
          >
            <span>
              <span style={{ color: C.text, fontFamily: "Cinzel, serif", fontSize: 13 }}>MurderHelp</span>
              {" "}© 2026 All rights reserved.
            </span>
            <div className="flex items-center gap-3">
              <span>BB탄 전용 · 만 18세 이상</span>
              <span className="px-2 py-0.5" style={{ border: `1px solid ${C.panelBorder}`, color: C.red }}>
                AIRSOFT ONLY
              </span>
            </div>
          </div>
        </div>
      </footer>
      {session && <FloatingChatWidget customerId={1} isAdmin={userTier === "green"} />}
    </div>
  );
}
