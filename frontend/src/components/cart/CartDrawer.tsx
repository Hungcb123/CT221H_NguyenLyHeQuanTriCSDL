"use client";

import React, { useState } from "react";
import { X, Trash2, ShoppingBag, ArrowRight, ShieldCheck } from "lucide-react";
import { useModal } from "@/hooks/useModal";
import { CartResponse } from "@/types";

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartResponse | null;
  onCheckout: () => void;
  error: string | null;
  onRetry: () => Promise<void>;
  onRemove: (productId: number) => Promise<void>;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  isOpen,
  onClose,
  cart,
  onCheckout, error, onRetry, onRemove,
}) => {
  const modalRef = useModal(isOpen, onClose);
  const [removing, setRemoving] = useState<number | null>(null);
  const [removeError, setRemoveError] = useState<string | null>(null);
  async function remove(productId: number) {
    if (removing !== null) return;
    setRemoving(productId);
    setRemoveError(null);
    try { await onRemove(productId); }
    catch (cause) { setRemoveError(cause instanceof Error ? cause.message : "Chưa xóa được sản phẩm. Vui lòng thử lại."); }
    finally { setRemoving(null); }
  }
  if (!isOpen) return null;

  return (
    <div ref={modalRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="cart-title" className="fixed inset-0 z-[70] overflow-hidden">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-stone-900/40 backdrop-blur-xs transition-opacity"
        onClick={onClose}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-0 sm:pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col justify-between">
          {/* Header */}
          <div className="p-4 bg-[#08783C] text-white flex items-center justify-between shadow-xs">
            <div className="flex items-center space-x-2">
              <ShoppingBag className="w-5 h-5 text-amber-300" />
              <h2 id="cart-title" className="text-base font-bold font-serif">Giỏ Hàng Của Bạn</h2>
              {cart && (
                <span className="bg-white/20 text-white text-xs px-2 py-0.5 rounded-full font-bold">
                  {cart.tongSoLuong} món
                </span>
              )}
            </div>
            <button
              aria-label="Đóng giỏ hàng"
              onClick={onClose}
              className="p-3 rounded-lg text-white/80 hover:bg-white/10 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Cart Item List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3 divide-y divide-stone-100">
            {(error || removeError) && <div role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">
              <p>{error || removeError}</p>{error && <button onClick={() => void onRetry()} className="min-h-11 font-bold underline">Thử lại</button>}
            </div>}
            {error ? null : (!cart || cart.danhSachMon.length === 0) ? (
              <div className="text-center py-16 text-stone-500">
                <ShoppingBag className="w-12 h-12 mx-auto text-stone-300 mb-3" />
                <p className="font-semibold text-sm">Giỏ hàng của bạn đang trống</p>
                <p className="text-xs text-stone-400 mt-1">Hãy chọn các thực phẩm tươi sạch để bổ sung vào giỏ.</p>
              </div>
            ) : (
              cart.danhSachMon.map((item) => (
                <div key={item.maSP} className="pt-3 first:pt-0 flex items-center justify-between gap-3">
                  <img
                    src={item.hinhAnh || "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=200&auto=format&fit=crop&q=80"}
                    alt={item.tenSP}
                    className="w-14 h-14 object-cover rounded-lg border border-stone-200 shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <h4 className="text-xs font-semibold text-stone-800 truncate">{item.tenSP}</h4>
                    <div className="text-[11px] text-stone-500 mt-0.5">
                      Đơn giá: {item.donGia.toLocaleString("vi-VN")}đ / {item.donViTinh}
                    </div>
                    <div className="text-xs font-bold text-[#B91C1C] mt-1">
                      {item.thanhTien.toLocaleString("vi-VN")}đ
                    </div>
                  </div>
                  <button aria-label={`Xóa ${item.tenSP} khỏi giỏ`} disabled={removing !== null} onClick={() => void remove(item.maSP)} className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-red-700 hover:bg-red-50 disabled:opacity-50"><Trash2 className="h-4 w-4" /></button>
                  <div className="text-xs font-bold text-stone-700 bg-stone-100 px-2 py-1 rounded">
                    x{item.soLuong}
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer & Checkout Action */}
          {!error && cart && cart.danhSachMon.length > 0 && (
            <div className="p-4 bg-stone-50 border-t border-stone-200 space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-stone-600 font-medium">Tạm tính giỏ hàng:</span>
                <span className="font-extrabold text-[#B91C1C] text-lg">
                  {cart.tongTien.toLocaleString("vi-VN")} VND
                </span>
              </div>

              <div className="flex items-center space-x-1.5 text-[11px] text-emerald-800 bg-emerald-50 p-2 rounded border border-emerald-200/50">
                <ShieldCheck className="w-4 h-4 shrink-0 text-emerald-600" />
                <span>Kiểm tra giỏ hàng và địa chỉ giao hàng trước khi thanh toán.</span>
              </div>

              <button
                disabled={removing !== null}
                onClick={() => {
                  onClose();
                  onCheckout();
                }}
                className="w-full py-3 bg-[#08783C] hover:bg-[#056331] text-white font-bold text-sm rounded-xl flex items-center justify-center space-x-2 shadow-md transition-colors"
              >
                <span>TIẾN HÀNH THANH TOÁN</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
