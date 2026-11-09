"use client";

import { useRef, useState } from "react";
import { ImageOff, Check, Minus, Plus, ShoppingBasket, X } from "lucide-react";
import { useModal } from "@/hooks/useModal";
import { ProductResponse } from "@/types";

interface ProductOptionsModalProps {
  product: ProductResponse | null;
  products: ProductResponse[];
  onClose: () => void;
  onAddToCart: (product: ProductResponse, quantity?: number) => Promise<void>;
}

export function ProductOptionsModal({
  product,
  products,
  onClose,
  onAddToCart,
}: ProductOptionsModalProps) {
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);
  const [addedIds, setAddedIds] = useState<number[]>([]);

  const [error, setError] = useState<string | null>(null);
  const pending = useRef(false);
  const modalRef = useModal(Boolean(product), onClose);

  if (!product) return null;

  // API hiện chưa có bảng biến thể. Cấu trúc mảng này sẵn sàng nhận nhiều SKU
  // cùng một sản phẩm khi backend bổ sung dữ liệu quy cách.
  const variants = [product];
  const recommendations = products
    .filter((item) => item.maSP !== product.maSP && item.maDanhMuc === product.maDanhMuc)
    .slice(0, 4);

  const add = async (item: ProductResponse, amount: number) => {
    if (pending.current || item.soLuongTon < amount) return;
    pending.current = true;
    setIsAdding(true);
    setError(null);
    try {
      await onAddToCart(item, amount);
      setAddedIds((current) => [...current, item.maSP]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Chưa thêm được sản phẩm. Vui lòng thử lại.");
    } finally {
      pending.current = false;
      setIsAdding(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/55 p-3 sm:p-6"
      ref={modalRef}
      tabIndex={-1}
      role="dialog"
      aria-modal="true"
      aria-labelledby="product-options-title"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="max-h-[92vh] w-full max-w-4xl overflow-y-auto rounded-xl bg-white shadow-[0_24px_70px_rgba(0,0,0,0.28)]">
        <div className="flex items-start justify-between gap-4 px-5 pb-3 pt-5">
          <div>
            <h2 id="product-options-title" className="text-xl font-extrabold text-slate-800">
              {product.tenSP}
            </h2>
            <p className="mt-1 text-sm text-slate-500">Chọn quy cách và số lượng bạn muốn mua</p>
          </div>
          <button
            onClick={onClose}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200 hover:text-slate-800"
            aria-label="Đóng cửa sổ chọn sản phẩm"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && <p role="alert" className="mx-5 mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
        <div className={`grid gap-3 px-5 pb-4 ${variants.length > 1 ? "sm:grid-cols-3" : "sm:grid-cols-1"}`}>
          {variants.map((variant) => (
            <article key={variant.maSP} className="overflow-hidden rounded-lg border-2 border-[#079447] bg-white">
              <div className="grid grid-cols-[90px_minmax(0,1fr)] items-center gap-4 p-4 sm:grid-cols-[170px_1fr]">
                {variant.hinhAnh ? (
                  <img
                    src={variant.hinhAnh}
                    alt={variant.tenSP}
                    className="aspect-square w-full rounded-lg object-cover"
                  />
                ) : (
                  <div className="aspect-square w-full flex flex-col items-center justify-center gap-2 rounded-lg bg-stone-50 text-stone-600">
                    <ImageOff className="h-7 w-7" aria-hidden="true" />
                    <span className="text-xs">Chưa có ảnh</span>
                  </div>
                )}
                <div>
                  <p className="text-sm font-semibold text-slate-500">{variant.donViTinh}</p>
                  <strong className="mt-2 block text-xl text-slate-900">
                    {variant.giaHienTai.toLocaleString("vi-VN")}đ
                  </strong>
                  <p className="mt-2 text-xs text-slate-500">Còn {variant.soLuongTon} sản phẩm</p>
                </div>
              </div>
              <div className="grid h-13 grid-cols-[52px_1fr_52px] border-t border-emerald-100 bg-emerald-50">
                <button
                  onClick={() => setQuantity((value) => Math.max(1, value - 1))}
                  className="flex items-center justify-center border-r border-emerald-100 text-emerald-800 hover:bg-emerald-100"
                  aria-label="Giảm số lượng"
                  disabled={isAdding || quantity <= 1}
                >
                  <Minus className="h-5 w-5" />
                </button>
                <span className="flex items-center justify-center text-sm font-extrabold text-slate-800">
                  {quantity} × {variant.donViTinh}
                </span>
                <button
                  onClick={() => setQuantity((value) => Math.min(variant.soLuongTon, value + 1))}
                  className="flex items-center justify-center border-l border-emerald-100 text-emerald-800 hover:bg-emerald-100"
                  aria-label="Tăng số lượng"
                  disabled={isAdding || quantity >= variant.soLuongTon}
                >
                  <Plus className="h-5 w-5" />
                </button>
              </div>
              <button
                onClick={() => add(product, quantity)}
                disabled={isAdding || variant.soLuongTon <= 0}
                className="flex h-12 w-full items-center justify-center gap-2 bg-[#079447] text-sm font-extrabold text-white hover:bg-[#057a3a] disabled:opacity-50"
              >
                {addedIds.includes(variant.maSP) ? <Check className="h-4 w-4" /> : <ShoppingBasket className="h-4 w-4" />}
                {isAdding ? "Đang thêm..." : addedIds.includes(variant.maSP) ? "Đã thêm vào giỏ" : "Mua sản phẩm này"}
              </button>
            </article>
          ))}
        </div>

        {recommendations.length > 0 && (
          <section className="bg-slate-50 px-5 py-5">
            <h3 className="mb-4 text-center text-base font-extrabold uppercase text-slate-600">
              Mua kèm tiện lợi hơn
            </h3>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {recommendations.map((item) => {
                const isAdded = addedIds.includes(item.maSP);
                return (
                  <article key={item.maSP} className="flex min-w-0 flex-col overflow-hidden rounded-lg bg-white ring-1 ring-slate-200">
                    {item.hinhAnh ? (
                      <img
                        src={item.hinhAnh}
                        alt={item.tenSP}
                        className="aspect-[4/3] w-full object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="aspect-[4/3] w-full flex flex-col items-center justify-center gap-2 rounded-lg bg-stone-50 text-stone-600">
                        <ImageOff className="h-7 w-7" aria-hidden="true" />
                        <span className="text-xs">Chưa có ảnh</span>
                      </div>
                    )}
                    <div className="flex flex-1 flex-col p-3">
                      <p className="line-clamp-2 min-h-10 text-sm text-slate-700">{item.tenSP}</p>
                      <strong className="mt-2 text-sm text-slate-900">
                        {item.giaHienTai.toLocaleString("vi-VN")}đ
                        <span className="font-normal text-slate-500">/{item.donViTinh}</span>
                      </strong>
                    </div>
                    <button
                      onClick={() => add(item, 1)}
                      disabled={isAdded || isAdding || item.soLuongTon <= 0}
                      className="h-11 bg-emerald-50 text-sm font-extrabold text-[#08783c] hover:bg-emerald-100 disabled:bg-emerald-100"
                    >
                      {isAdded ? "Đã thêm" : "Mua kèm"}
                    </button>
                  </article>
                );
              })}
            </div>
          </section>
        )}

        <div className="p-5">
          <button
            onClick={onClose}
            className="h-13 w-full rounded-lg border-2 border-[#079447] text-sm font-extrabold uppercase text-[#08783c] hover:bg-emerald-50"
          >
            Hoàn tất
          </button>
        </div>
      </div>
    </div>
  );
}
