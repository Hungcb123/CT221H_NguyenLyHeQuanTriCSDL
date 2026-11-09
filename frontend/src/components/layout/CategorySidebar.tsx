"use client";

import React from "react";
import {
  Menu,
  ChevronRight,
  Fish,
  Salad,
  Flame,
  Wheat,
  Coffee,
  Milk,
  Cookie,
} from "lucide-react";
import { Category } from "@/types";

interface CategorySidebarProps {
  categories: Category[];
  selectedCategoryId: number | null;
  onSelectCategory: (id: number | null) => void;
  hideOnMobile?: boolean;
}

export const CategorySidebar: React.FC<CategorySidebarProps> = ({
  categories,
  selectedCategoryId,
  onSelectCategory,
  hideOnMobile,
}) => {
  // Helper to map category name to Lucide icon
  const getCategoryIcon = (name: string) => {
    const lower = name.toLowerCase();
    if (lower.includes("thịt") || lower.includes("cá") || lower.includes("hải sản")) {
      return <Fish className="w-4 h-4" />;
    }
    if (lower.includes("rau") || lower.includes("củ") || lower.includes("trái cây")) {
      return <Salad className="w-4 h-4" />;
    }
    if (lower.includes("dầu") || lower.includes("gia vị") || lower.includes("nước chấm")) {
      return <Flame className="w-4 h-4" />;
    }
    if (lower.includes("gạo") || lower.includes("bột") || lower.includes("đồ khô")) {
      return <Wheat className="w-4 h-4" />;
    }
    if (lower.includes("nước") || lower.includes("giải khát")) {
      return <Coffee className="w-4 h-4" />;
    }
    if (lower.includes("sữa")) {
      return <Milk className="w-4 h-4" />;
    }
    return <Cookie className="w-4 h-4" />;
  };

  return (
    <aside className={`w-full shrink-0 flex-col justify-between border-r border-[#CFE3D6] bg-white lg:w-[325px] ${hideOnMobile ? "hidden lg:flex" : "flex"}`}>
      <div>
        {/* Sidebar Title Header matching mockup */}
        <div className="m-4 mb-2 lg:mb-6 rounded-lg bg-[#079447] text-white px-7 py-4 flex items-center space-x-3 font-bold tracking-wide text-base uppercase">
          <Menu className="w-5 h-5" />
          <span>DANH MỤC SẢN PHẨM</span>
        </div>

        {/* Categories List */}
        <nav aria-label="Danh mục sản phẩm" className="flex overflow-x-auto lg:block lg:divide-y lg:divide-stone-100 text-sm">
          {/* All option */}
          <button
            aria-pressed={selectedCategoryId === null}
            onClick={() => onSelectCategory(null)}
            className={`w-auto shrink-0 lg:w-full text-left px-4 py-3 flex items-center justify-between transition-colors ${
              selectedCategoryId === null
                ? "bg-emerald-50 text-[#08783C] font-bold"
                : "text-stone-700 hover:bg-stone-50 hover:text-[#08783C]"
            }`}
          >
            <div className="flex items-center space-x-3">
              <Menu className="h-4 w-4 text-[#08783C]" />
              <span>Tất cả sản phẩm</span>
            </div>
            <ChevronRight className="w-4 h-4 text-stone-400" />
          </button>

          {categories.map((cat) => {
            const isSelected = selectedCategoryId === cat.maDanhMuc;
            return (
              <button
                key={cat.maDanhMuc}
                aria-pressed={isSelected}
                onClick={() => onSelectCategory(cat.maDanhMuc)}
                className={`w-auto shrink-0 lg:w-full text-left px-4 py-3 flex items-center justify-between transition-colors ${
                  isSelected
                    ? "bg-emerald-50 text-[#08783C] font-bold"
                    : "text-stone-700 hover:bg-stone-50 hover:text-[#08783C]"
                }`}
              >
                <div className="flex items-center space-x-3 truncate">
                  <span className={`${isSelected ? "text-[#08783C]" : "text-stone-500"}`}>
                    {getCategoryIcon(cat.tenDanhMuc)}
                  </span>
                  <span className="truncate">{cat.tenDanhMuc}</span>
                </div>
                <ChevronRight className="w-4 h-4 text-stone-400 shrink-0" />
              </button>
            );
          })}
        </nav>
      </div>


    </aside>
  );
};
