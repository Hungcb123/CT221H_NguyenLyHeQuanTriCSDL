"use client";

import React from "react";

export const Footer: React.FC = () => {
  return (
    <footer className="bg-stone-100 border-t border-[#CFE3D6] text-stone-600 text-xs">
      <div className="w-full px-5 py-7 flex flex-col sm:flex-row items-center justify-between gap-2 text-stone-600">
        <span className="font-extrabold text-stone-800 text-base font-serif">DailyMart</span>
        <span>© 2026 DailyMart. Mang sự tươi ngon đến tận cửa nhà bạn.</span>
        <a href="/product-image-credits.html" className="underline">Nguồn ảnh minh họa</a>
      </div>
    </footer>
  );
};
