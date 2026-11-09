"use client";

import React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
}) => {
  if (totalPages <= 1) return null;

  // Generate page numbers array
  const pages: (number | string)[] = [];
  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) pages.push(i);
  } else {
    pages.push(1);
    if (currentPage > 3) pages.push("...");
    const start = Math.max(2, currentPage - 1);
    const end = Math.min(totalPages - 1, currentPage + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (currentPage < totalPages - 2) pages.push("...");
    pages.push(totalPages);
  }

  return (
    <div className="flex items-center justify-center space-x-1.5 py-8">
      {/* Previous Page */}
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
        className="w-9 h-9 rounded-lg border border-stone-200 flex items-center justify-center text-stone-600 hover:bg-stone-50 disabled:opacity-40 disabled:pointer-events-none transition-colors"
        aria-label="Trang trước"
      >
        <ChevronLeft className="w-4 h-4" />
      </button>

      {/* Number Buttons */}
      {pages.map((p, idx) => {
        if (p === "...") {
          return (
            <span key={`dots-${idx}`} className="w-9 h-9 flex items-center justify-center text-stone-400 text-sm">
              ...
            </span>
          );
        }
        const pageNum = Number(p);
        const isActive = pageNum === currentPage;
        return (
          <button
            key={`page-${pageNum}`}
            onClick={() => onPageChange(pageNum)}
            className={`w-9 h-9 rounded-lg text-xs font-bold transition-colors ${
              isActive
                ? "bg-[#08783C] text-white shadow-xs"
                : "border border-stone-200 text-stone-700 hover:bg-stone-50"
            }`}
          >
            {pageNum}
          </button>
        );
      })}

      {/* Next Page */}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
        className="w-9 h-9 rounded-lg border border-stone-200 flex items-center justify-center text-stone-600 hover:bg-stone-50 disabled:opacity-40 disabled:pointer-events-none transition-colors"
        aria-label="Trang sau"
      >
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  );
};
