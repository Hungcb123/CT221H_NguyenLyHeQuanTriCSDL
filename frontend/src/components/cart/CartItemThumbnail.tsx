"use client";

import { ImageOff } from "lucide-react";
import { useState } from "react";
import type { CartItem } from "@/types";

export function CartItemThumbnail({ item }: { item: CartItem }) {
  const [failedSource, setFailedSource] = useState<string | null>(null);
  const source = item.hinhAnh?.trim();

  return (
    <div className="grid size-14 shrink-0 place-items-center overflow-hidden rounded-lg border border-stone-200 bg-stone-50">
      {source && source !== failedSource ? (
        <img
          src={source}
          alt={item.tenSP}
          className="size-full object-cover"
          onError={() => setFailedSource(source)}
        />
      ) : (
        <span role="img" aria-label={`Chưa có ảnh của ${item.tenSP}`} title="Chưa có ảnh sản phẩm" className="text-stone-500">
          <ImageOff className="size-5" aria-hidden="true" />
        </span>
      )}
    </div>
  );
}
