"use client";

import React from "react";

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-[#CFE3D6] bg-stone-100 text-sm text-stone-600">
      <div className="w-full px-5 py-7 flex flex-col sm:flex-row items-center justify-between gap-2 text-stone-600">
        <span className="text-base font-extrabold text-stone-800">DailyMart</span>
        <span>© 2026 DailyMart. Mang sự tươi ngon đến tận cửa nhà bạn.</span>
        <a href="/product-image-credits.html" className="underline">Nguồn ảnh minh họa</a>
      </div>
    </footer>
  );
};
