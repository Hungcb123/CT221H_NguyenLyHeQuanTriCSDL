"use client";

import React from "react";
import { ImageOff } from "lucide-react";
import { ProductResponse } from "@/types";
import { productAvailability } from "@/services/product-availability";

interface ProductCardProps {
  product: ProductResponse;
  onSelect: (product: ProductResponse) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product, onSelect }) => {

  const availability = productAvailability(product);

  // Determine badge label and color
  const badgeText = product.tag || (product.dangKhuyenMai && product.phanTramGiam ? `-${product.phanTramGiam}%` : null);
  const isNew = badgeText === "Mới";

  return (
    <div className="group bg-white rounded-lg border border-stone-200 overflow-hidden hover:border-[#079447]/50 hover:shadow-md transition-all duration-200 flex flex-col justify-between">
      <div>
        {/* Image & Badge Area */}
        <div className="relative aspect-square w-full bg-stone-50 overflow-hidden flex items-center justify-center p-2.5">
          {badgeText && (
            <span
              className={`absolute left-2.5 top-2.5 z-10 max-w-[85%] rounded-sm px-2 py-0.5 text-xs font-bold leading-snug shadow-xs ${
                isNew
                  ? "bg-emerald-600 text-white"
                  : "bg-[#B91C1C] text-white"
              }`}
            >
              {badgeText}
            </span>
          )}

          {product.hinhAnh ? (
            <img
              src={product.hinhAnh}
              alt={product.tenSP}
              className="w-full h-full object-cover rounded-lg group-hover:scale-105 transition-transform duration-300"
              loading="lazy"
            />
          ) : (
            <div className="aspect-square w-full flex flex-col items-center justify-center gap-2 rounded-lg bg-stone-50 text-stone-600">
              <ImageOff className="h-7 w-7" aria-hidden="true" />
              <span className="text-xs">Chưa có ảnh</span>
            </div>
          )}
        </div>

        {/* Product Details */}
        <div className="p-3">
          <h3 className="line-clamp-2 min-h-10 text-sm font-semibold leading-snug text-stone-800 transition-colors group-hover:text-[#08783c]">
            {product.tenSP}
          </h3>

          {!availability.available && <p className="mt-2 text-sm font-semibold text-red-800">{availability.label}</p>}

          {/* Price Area */}
          <div className="mt-2.5 flex items-baseline gap-2 flex-wrap">
            <span className="text-base font-extrabold text-[#B91C1C]">
              {product.giaHienTai.toLocaleString("vi-VN")}đ
            </span>
            {product.giaGoc > product.giaHienTai && (
              <span className="text-sm text-stone-600 line-through">
                {product.giaGoc.toLocaleString("vi-VN")}đ
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Button Action */}
      <div className="p-3 pt-0">
        <button
          onClick={() => onSelect(product)}
          className="min-h-11 w-full rounded-lg border border-[#079447] px-3 py-2 text-sm font-bold text-[#08783c] transition-colors hover:bg-[#079447] hover:text-white"
        >
          {availability.available ? "Mua" : "Xem tình trạng"}
        </button>
      </div>
    </div>
  );
};
