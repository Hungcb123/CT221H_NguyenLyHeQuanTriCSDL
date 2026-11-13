"use client";

import React, { useState } from "react";
import { X, Trash2, ShoppingBag, ArrowRight, ShieldCheck, Minus, Plus } from "lucide-react";
import { useModal } from "@/hooks/useModal";
import { CartResponse } from "@/types";
import { CartItemThumbnail } from "./CartItemThumbnail";

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartResponse | null;
  onCheckout: () => void;
  error: string | null;
  onRetry: () => Promise<void>;
  onRemove: (productId: number) => Promise<void>;
  onChangeQuantity: (productId: number, quantity: number) => Promise<void>;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  isOpen,
  onClose,
  cart,
  onCheckout, error, onRetry, onRemove, onChangeQuantity,
}) => {
  const modalRef = useModal(isOpen, onClose);
  const [pending, setPending] = useState(false);
  const [changeError, setChangeError] = useState<string | null>(null);
  async function changeCart(action: () => Promise<void>) {
    if (pending) return;
    setPending(true);
    setChangeError(null);
    try { await action(); }
    catch (cause) { setChangeError(cause instanceof Error ? cause.message : "Chưa cập nhật được giỏ hàng. Vui lòng thử lại."); }
    finally { setPending(false); }
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
              <h2 id="cart-title" className="text-base font-bold">Giỏ hàng của bạn</h2>
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
            {(error || changeError) && <div role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">
              <p>{error || changeError}</p>{error && <button onClick={() => void onRetry()} className="min-h-11 font-bold underline">Thử lại</button>}
            </div>}
            {error ? null : (!cart || cart.danhSachMon.length === 0) ? (
              <div className="text-center py-16 text-stone-500">
                <ShoppingBag className="w-12 h-12 mx-auto text-stone-300 mb-3" />
                <p className="font-semibold text-sm">Giỏ hàng của bạn đang trống</p>
                <p className="text-xs text-stone-400 mt-1">Hãy chọn các thực phẩm tươi sạch để bổ sung vào giỏ.</p>
              </div>
            ) : (
              cart.danhSachMon.map((item) => (
                <div key={item.maSP} className="flex items-start gap-3 pt-3 first:pt-0">
                  <CartItemThumbnail item={item} />
                  <div className="flex-1 min-w-0">
                    <h4 className="break-words text-sm font-semibold text-stone-800">{item.tenSP}</h4>
                    <div className="mt-0.5 text-sm text-stone-600">
                      Đơn giá: {item.donGia.toLocaleString("vi-VN")}đ / {item.donViTinh}
                    </div>
                    <div className="mt-1 text-sm font-bold text-[#B91C1C]">
                      {item.thanhTien.toLocaleString("vi-VN")}đ
                    </div>
                    <div className="mt-2 inline-flex items-center rounded-lg border border-stone-200">
                      <button type="button" aria-label={`Giảm số lượng ${item.tenSP}`}
                        disabled={pending || item.soLuong <= 1}
                        onClick={() => void changeCart(() => onChangeQuantity(item.maSP, item.soLuong - 1))}
                        className="grid size-11 place-items-center text-emerald-800 hover:bg-emerald-50 disabled:opacity-40">
                        <Minus className="size-4" aria-hidden="true" />
                      </button>
                      <span aria-live="polite" className="min-w-8 px-1 text-center text-sm font-semibold tabular-nums">{item.soLuong}</span>
                      <button type="button" aria-label={`Tăng số lượng ${item.tenSP}`}
                        disabled={pending || item.conDuHang === false || item.soLuong >= (item.soLuongTonHienTai ?? Infinity)}
                        onClick={() => void changeCart(() => onChangeQuantity(item.maSP, item.soLuong + 1))}
                        className="grid size-11 place-items-center text-emerald-800 hover:bg-emerald-50 disabled:opacity-40">
                        <Plus className="size-4" aria-hidden="true" />
                      </button>
                    </div>
                    {item.conDuHang === false && <p className="mt-1 text-sm text-red-800">Không thể thanh toán số lượng hiện tại.</p>}
                  </div>
                  <button type="button" aria-label={`Xóa ${item.tenSP} khỏi giỏ`} disabled={pending} onClick={() => void changeCart(() => onRemove(item.maSP))} className="flex size-11 shrink-0 items-center justify-center rounded-lg text-red-700 hover:bg-red-50 disabled:opacity-50"><Trash2 className="size-4" aria-hidden="true" /></button>
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

              <div className="flex items-center gap-2 rounded border border-emerald-200/50 bg-emerald-50 p-2 text-sm text-emerald-800">
                <ShieldCheck className="w-4 h-4 shrink-0 text-emerald-600" />
                <span>Kiểm tra giỏ hàng và địa chỉ giao hàng trước khi thanh toán.</span>
              </div>

              <button
                disabled={pending || cart.danhSachMon.some((item) => item.conDuHang === false)}
                onClick={() => {
                  onClose();
                  onCheckout();
                }}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08783C] py-3 text-sm font-bold text-white shadow-md transition-colors hover:bg-[#056331] disabled:opacity-50"
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
