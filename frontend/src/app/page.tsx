"use client";

import React, { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useCustomer } from "@/components/customer/CustomerProvider";
import { Header } from "@/components/layout/Header";
import { CategorySidebar } from "@/components/layout/CategorySidebar";
import { HeroBanner } from "@/components/home/HeroBanner";
import { ProductCard } from "@/components/product/ProductCard";
import { ProductOptionsModal } from "@/components/product/ProductOptionsModal";
import { Footer } from "@/components/layout/Footer";
import { FloatingChatbot } from "@/components/chatbot/FloatingChatbot";
import { CartDrawer } from "@/components/cart/CartDrawer";
import { apiService } from "@/services/api";
import { Category, ProductResponse, CartResponse } from "@/types";
import { Sparkles, ArrowRight } from "lucide-react";

export default function HomePage() {
  const router = useRouter();
  const { customer } = useCustomer();
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [relatedProducts, setRelatedProducts] = useState<ProductResponse[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null); // Default active on Vegetables as in mockup
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [activeNavFilter, setActiveNavFilter] = useState<string>("HANG_MOI");
  const [cartError, setCartError] = useState<string | null>(null);
  const [categoryError, setCategoryError] = useState<string | null>(null);
  const [storedCart, setCart] = useState<CartResponse | null>(null);
  const cart = customer && storedCart?.maKH === customer.maKH ? storedCart : null;
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isSemanticResult, setIsSemanticResult] = useState(false);
  const [semanticScoreNotice, setSemanticScoreNotice] = useState<string | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<ProductResponse | null>(null);

  // Fetch initial data
  useEffect(() => {
    // Load categories
    apiService.getCategories().then(setCategories).catch(() => setCategoryError("Không tải được danh mục."));

    // Load cart
  }, []);

  useEffect(() => {
    let cancelled = false;
    if (customer) void apiService.getCart(customer.maKH).then(value => { if(!cancelled){setCart(value);setCartError(null);} }).catch(() => { if(!cancelled)setCartError("Không tải được giỏ hàng."); });
    return () => { cancelled = true; };
  }, [customer]);

  const [search, setSearch] = useState<{ keyword: string } | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);
  const loadMoreRef = useRef<HTMLDivElement>(null);
  const hasMore = !search && currentPage < totalPages;

  const loadCart = async () => {
    if (!customer) { setCart(null); return; }
    try {
      const cartData = await apiService.getCart(customer.maKH);
      setCart(cartData);
      setCartError(null);
    } catch {
      setCartError("Không tải được giỏ hàng. Vui lòng thử lại.");
    }
  };

  // Cleanup prevents a slower, obsolete request from replacing a newer search/filter.
  useEffect(() => {
    let cancelled = false;
    async function fetchProducts() {
      setIsLoading(true);
      setLoadError(null);
      setIsSemanticResult(Boolean(search));
      setSemanticScoreNotice(null);
      try {
        if (search) {
          const searchResult = await apiService.searchSemantic(search.keyword, 12);
          if (cancelled) return;
          const mappedProducts: ProductResponse[] = searchResult.products.map((product) => ({
            ...product,
            tag: product.matchType === "EXACT_KEYWORD" ? "Khớp từ khóa"
              : product.matchType === "HYBRID" ? "Khớp kết hợp" : "Khớp ngữ nghĩa",
          }));
          setProducts(mappedProducts);
          setRelatedProducts(searchResult.relatedProducts.map((product) => ({ ...product, tag: "Tham khảo" })));
          setTotalPages(1);
          setSemanticScoreNotice(searchResult.mode === "KEYWORD_FALLBACK"
            ? `Tìm thấy ${mappedProducts.length} sản phẩm theo từ khóa. Tìm kiếm AI tạm thời không khả dụng.`
            : `Tìm thấy ${mappedProducts.length} sản phẩm bằng tìm kiếm kết hợp AI và từ khóa.`);
        } else {
          const res = await apiService.getProducts({
            categoryId: selectedCategoryId || undefined,
            sortBy: activeNavFilter === "GIA_ASC" ? "GIA_ASC" : "MOI_NHAT",
            page: currentPage,
            size: 12,
          });
          if (cancelled) return;
          setProducts((previous) => currentPage === 1 ? res.products : [
            ...previous,
            ...res.products.filter((product) => !previous.some((item) => item.maSP === product.maSP)),
          ]);
          setTotalPages(res.products.length === 0 ? currentPage : res.totalPages);
        }
      } catch (error) {
        if (!cancelled) setLoadError(error instanceof Error
          ? error.message : "Không tải được sản phẩm. Vui lòng thử lại.");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }
    void fetchProducts();
    return () => { cancelled = true; };
  }, [selectedCategoryId, currentPage, activeNavFilter, search, retry]);

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target || !hasMore || isLoading || loadError || !("IntersectionObserver" in window)) return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        observer.disconnect();
        setIsLoading(true);
        setCurrentPage((page) => page + 1);
      }
    }, { rootMargin: "300px" });
    observer.observe(target);
    return () => observer.disconnect();
  }, [hasMore, isLoading, loadError]);

  const resetProducts = () => {
    setProducts([]);
    setRelatedProducts([]);
    setCurrentPage(1);
    setTotalPages(0);
    setIsLoading(true);
    setLoadError(null);
    setSearch(null);
    setSearchTerm("");
    setRetry((value) => value + 1);
  };

  const handleSearch = (keyword: string) => {
    resetProducts();
    setSearchTerm(keyword);
    setSearch({ keyword });
  };

  const handleAddToCart = async (product: ProductResponse, quantity = 1) => {
    if (!customer) { router.push("/login?next=/"); return; }
    await apiService.addToCart({ customerId: customer.maKH, productId: product.maSP, quantity });
    await loadCart();
  };

  return (
    <div className="min-h-screen bg-[#F5FAF7] text-stone-900 flex flex-col font-sans">
      {/* 1. Header matching mockup */}
      <Header
        cartCount={cart?.tongSoLuong ?? 0}
        onOpenCart={() => { if(!customer){router.push("/login?next=/");return;} setIsCartOpen(true); void loadCart(); }}
        onSearch={handleSearch}
        searchTerm={searchTerm}
        onSearchTermChange={setSearchTerm}
        activeFilter={activeNavFilter}
        setActiveFilter={(filter) => {
          resetProducts();
          setActiveNavFilter(filter);
        }}
      />

      {categoryError && <div role="alert" className="bg-amber-50 px-5 py-3 text-sm text-amber-900">
        {categoryError} <button className="underline" onClick={() => { void apiService.getCategories().then((data) => { setCategories(data); setCategoryError(null); }).catch(() => setCategoryError("Không tải được danh mục. Vui lòng thử lại.")); }}>Thử lại</button>
      </div>}
      {/* 2. Main Container: Sidebar + Content */}
      <main className="w-full flex-1">
        <div className="flex flex-col lg:flex-row items-stretch">
          {/* Left Category Sidebar */}
          <CategorySidebar
            categories={categories}
            hideOnMobile={Boolean(search)}
            selectedCategoryId={selectedCategoryId}
            onSelectCategory={(id) => {
              setSelectedCategoryId(id);
              resetProducts();
            }}
          />

          {/* Right Main Content Area */}
          <div className="flex-1 min-w-0 w-full bg-[#F7F9FC] px-5 py-5">
            {/* Hero Banners */}
            {!search && <HeroBanner
              onSelectPromotion={() => {
                resetProducts();
                setActiveNavFilter("HANG_MOI");
                const seafood = categories.find((category) => /hải sản|thủy sản/i.test(category.tenDanhMuc));
                setSelectedCategoryId(seafood?.maDanhMuc ?? null);
              }}
            />}

            {/* Semantic Search AI Alert Banner (when active) */}
            {isSemanticResult && (
              <div className="mb-4 flex flex-wrap items-center justify-between gap-x-4 gap-y-2 rounded-xl border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm leading-relaxed text-emerald-900 sm:text-base">
                <div className="flex min-w-0 items-start gap-2.5" role="status">
                  <Sparkles className="mt-1 size-4 shrink-0 text-emerald-700" aria-hidden="true" />
                  <span className="font-medium">{semanticScoreNotice ?? (isLoading ? "Đang tìm sản phẩm…" : "")}</span>
                </div>
                <button
                  type="button"
                  onClick={resetProducts}
                  className="customer-action"
                >
                  Xóa tìm kiếm
                </button>
              </div>
            )}

            {/* Product Section Header */}
            <div className="mb-5 flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-stone-200 pb-3">
              <h1 className="text-xl font-bold leading-snug text-stone-900 sm:text-2xl">
                {search ? "Kết quả tìm kiếm" : "Sản phẩm nổi bật"}
              </h1>
              <button
                type="button"
                onClick={() => {
                  setSelectedCategoryId(null);
                  resetProducts();
                }}
                className="customer-action"
              >
                <span>Xem tất cả</span>
                <ArrowRight aria-hidden="true" />
              </button>
            </div>

            {/* Product Grid: 6 columns on lg screen matching the mockup */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 sm:gap-4">
              {products.map((product) => (
                <ProductCard
                  key={product.maSP}
                  product={product}
                  onSelect={setSelectedProduct}
                />
              ))}
            </div>

            <div ref={loadMoreRef} className="flex min-h-20 flex-wrap gap-3 items-center justify-center py-6 text-sm text-stone-600">
              <div role="status" aria-live="polite">
                {isLoading && "Đang tải sản phẩm…"}
                {!isLoading && !loadError && products.length === 0 && "Không tìm thấy sản phẩm phù hợp."}
                {!isLoading && !loadError && products.length > 0 && !hasMore && `Đã hiển thị tất cả ${products.length} sản phẩm${isSemanticResult ? " phù hợp" : ""}.`}
              </div>
              {!isLoading && loadError && <p role="alert" className="mr-3 text-red-700">{loadError}</p>}
              {!isLoading && (loadError || hasMore) && (
                <button
                  type="button"
                  className="rounded-lg border border-[#079447] px-4 py-2 font-semibold text-[#08783c] hover:bg-emerald-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#08783c]"
                  onClick={() => {
                    setIsLoading(true);
                    if (loadError) setRetry((value) => value + 1);
                    else setCurrentPage((page) => page + 1);
                  }}
                >
                  {loadError ? "Thử lại" : "Tải thêm sản phẩm"}
                </button>
              )}
            </div>

            {!isLoading && !loadError && relatedProducts.length > 0 && (
              <section aria-labelledby="related-products-heading" className="mt-6 border-t border-stone-200 pt-6">
                <h2 id="related-products-heading" className="text-lg font-bold text-stone-900">
                  Sản phẩm khác có thể tham khảo
                </h2>
                <p className="mt-2 mb-5 text-sm text-stone-600">
                  Các sản phẩm này không khớp đầy đủ yêu cầu tìm kiếm của bạn.
                </p>
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 sm:gap-4">
                  {relatedProducts.map((product) => (
                    <ProductCard key={product.maSP} product={product} onSelect={setSelectedProduct} />
                  ))}
                </div>
              </section>
            )}
          </div>
        </div>
      </main>

      {/* 3. Footer matching mockup */}
      <Footer />

      {/* 4. Floating AI Chatbot Culinary Assistant Widget */}
      <FloatingChatbot
        key={customer?.maKH ?? "guest"}
        onCartUpdated={loadCart}
        onNavigateCheckout={() => router.push("/checkout")}
      />

      <ProductOptionsModal
        key={selectedProduct?.maSP ?? "closed"}
        product={selectedProduct}
        products={[...products, ...relatedProducts]}
        onClose={() => setSelectedProduct(null)}
        onAddToCart={handleAddToCart}
        onSelect={setSelectedProduct}
      />

      {/* 5. Cart Slide-over Drawer */}
      <CartDrawer
        isOpen={isCartOpen && !!customer}
        onClose={() => setIsCartOpen(false)}
        cart={cart}
        error={cartError}
        onRetry={loadCart}
        onRemove={async (productId) => { if(customer)await apiService.removeFromCart(customer.maKH, productId); await loadCart(); }}
        onChangeQuantity={async (productId, quantity) => {
          if (!customer) return;
          setCart(await apiService.updateCartQuantity({ customerId: customer.maKH, productId, quantity }));
          setCartError(null);
        }}
        onCheckout={() => router.push("/checkout")}
      />
    </div>
  );
}
