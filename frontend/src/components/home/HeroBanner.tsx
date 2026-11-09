"use client";

import React from "react";
import { ArrowRight, ChefHat } from "lucide-react";

interface HeroBannerProps {
  onSelectPromotion?: () => void;
}

export const HeroBanner: React.FC<HeroBannerProps> = ({ onSelectPromotion }) => {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 mb-6">
      {/* Main Banner: Hải sản cho bữa cơm nhà */}
      <div className="lg:col-span-2 relative rounded-xl overflow-hidden bg-gradient-to-r from-white via-white to-[#E8F1F0] border border-stone-200 p-7 flex flex-col justify-between min-h-[230px]">
        {/* Decorative Background Fresh Salmon Accent */}
        <div
          className="absolute right-0 top-0 bottom-0 w-1/2 bg-contain bg-no-repeat bg-right-bottom pointer-events-none opacity-90 hidden sm:block"
          style={{
            backgroundImage:
              "url('https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=800&auto=format&fit=crop&q=80')",
            mixBlendMode: "multiply",
          }}
        />

        <div className="relative z-10 sm:w-1/2 sm:pr-5">
          <h2 className="text-2xl font-extrabold text-stone-900 leading-tight mb-2 font-serif">
            Hải sản cho bữa cơm nhà
          </h2>
          <p className="text-stone-600 text-xs sm:text-sm mb-4 leading-relaxed">
            Khám phá các loại cá và hải sản, xem giá và chọn số lượng phù hợp cho bữa ăn.
          </p>
          <button
            onClick={onSelectPromotion}
            className="inline-flex items-center space-x-1.5 text-[#08783C] hover:text-[#056331] font-bold text-sm tracking-wide group"
          >
            <span>MUA HẢI SẢN</span>
            <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
          </button>
        </div>
      </div>

      {/* Side Banner Card: Đi chợ theo món ăn */}
      <div className="rounded-xl bg-[#F3F0E9] border border-[#BBD9C5] p-6 flex flex-col justify-between items-center text-center">
        <div className="w-12 h-12 rounded-full bg-amber-100/80 text-[#08783C] flex items-center justify-center mb-2">
          <ChefHat className="w-6 h-6" />
        </div>
        <div className="space-y-2">
          <h3 className="text-lg font-bold text-stone-900 font-serif">Đi chợ theo món ăn</h3>
          <p className="text-stone-600 text-xs leading-relaxed max-w-xs">
            Hỏi Bếp Trưởng AI để tìm công thức, xem nguyên liệu và thêm sản phẩm vào giỏ hàng.
          </p>
        </div>

      </div>
    </div>
  );
};
