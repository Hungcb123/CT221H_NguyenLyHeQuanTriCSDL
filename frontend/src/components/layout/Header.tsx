"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Search, ShoppingCart, Sparkles } from "lucide-react";
import { AccountMenu } from "@/components/customer/AccountMenu";

interface HeaderProps {
  cartCount: number;
  onOpenCart: () => void;
  onSearch: (keyword: string) => void;
  activeFilter: string;
  setActiveFilter: (filter: string) => void;
}

export const Header: React.FC<HeaderProps> = ({
  cartCount,
  onOpenCart,
  onSearch,
  activeFilter,
  setActiveFilter,
}) => {
  const [searchTerm, setSearchTerm] = useState("");

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchTerm.trim()) {
      onSearch(searchTerm.trim());
    }
  };

  return (
    <header className="sticky top-0 z-40 border-b border-emerald-900 bg-[#075B36] text-white shadow-[0_6px_18px_rgba(4,78,43,0.16)]">
      {/* Main Header Bar */}
      <div className="w-full px-5 py-3">
        <div className="flex flex-wrap items-center justify-between gap-3 lg:gap-6">
          {/* Logo */}
          <Link href="/" className="flex items-center space-x-2 shrink-0">
            <span className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
              DailyMart
            </span>
          </Link>

          {/* Search Bar with AI Semantic Toggle */}
          <form
            onSubmit={handleSearchSubmit}
            className="order-last w-full lg:order-none lg:min-w-48 lg:flex-1 max-w-[660px] relative flex items-center"
          >
            <div className="w-full relative flex items-center">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm tên sản phẩm hoặc nhu cầu: nguyên liệu nấu canh chua…"
                aria-label="Tìm kiếm sản phẩm theo tên hoặc nhu cầu"
                className="w-full h-11 pl-4 pr-20 sm:pr-32 rounded-full border border-white/30 bg-white text-stone-900 text-base transition-all outline-hidden focus:border-emerald-300"
              />
              <span className="absolute right-12 flex items-center gap-1 text-xs font-semibold text-emerald-800 pointer-events-none">
                <Sparkles className="w-3 h-3" aria-hidden="true" />
                <span className="hidden sm:inline">Thông minh</span>
              </span>

              {/* Submit Search Button */}
              <button
                type="submit"
                className="absolute right-1 w-9 h-9 rounded-full bg-[#079447] hover:bg-[#057a3a] text-white flex items-center justify-center transition-colors"
                title="Tìm kiếm"
              >
                <Search className="w-4 h-4" />
              </button>
            </div>
          </form>

          {/* Quick Navigation Links */}
          <nav className="hidden xl:flex items-center space-x-4 text-sm font-medium text-white/90">
            <button
              onClick={() => setActiveFilter("HANG_MOI")}
              className={`pb-1 transition-colors ${
                activeFilter === "HANG_MOI"
                  ? "text-white border-b-2 border-emerald-300"
                  : "hover:text-white"
              }`}
            >
              HÀNG MỚI
            </button>
            <button
              onClick={() => setActiveFilter("GIA_ASC")}
              className={`pb-1 transition-colors ${
                activeFilter === "GIA_ASC"
                  ? "text-white border-b-2 border-emerald-300"
                  : "hover:text-white"
              }`}
            >
              GIÁ TĂNG DẦN
            </button>
          </nav>

          {/* Action Buttons: Cart & Account */}
          <div className="flex items-center space-x-3 shrink-0">
            {/* Cart Button */}
            <button
              onClick={onOpenCart}
              className="relative p-2 text-white hover:bg-white/10 transition-colors rounded-full flex items-center"
              title="Xem giỏ hàng"
              aria-label="Xem giỏ hàng"
            >
              <ShoppingCart className="w-6 h-6" />
              {cartCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-600 text-white font-bold text-xs rounded-full w-5 h-5 flex items-center justify-center shadow-xs">
                  {cartCount}
                </span>
              )}
            </button>

            <AccountMenu />
          </div>
        </div>
      </div>
      <nav aria-label="Sắp xếp sản phẩm" className="flex gap-2 overflow-x-auto px-5 pb-3 xl:hidden">
        {[{ value: "HANG_MOI", label: "Hàng mới" }, { value: "GIA_ASC", label: "Giá tăng dần" }].map((item) => <button key={item.value} aria-pressed={activeFilter === item.value} onClick={() => setActiveFilter(item.value)} className={`min-h-11 shrink-0 rounded-lg px-4 text-sm font-semibold ${activeFilter === item.value ? "bg-white text-emerald-900" : "text-white hover:bg-white/10"}`}>{item.label}</button>)}
      </nav>
    </header>
  );
};
