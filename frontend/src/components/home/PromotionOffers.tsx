"use client";

import { useEffect, useState } from "react";
import { Clock3, Zap } from "lucide-react";
import { apiService } from "@/services/api";
import { promotionTime } from "@/services/promotion-time";
import { PromotionCampaign, ProductResponse } from "@/types";
import { ProductCard } from "@/components/product/ProductCard";

export function PromotionOffers({ onSelect }: { onSelect: (product: ProductResponse) => void }) {
  const [campaigns, setCampaigns] = useState<PromotionCampaign[]>([]);
  const [error, setError] = useState("");
  const [expanded, setExpanded] = useState<number[]>([]);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const items = await apiService.getPromotionOffers();
        if (active) { setCampaigns(items); setError(""); }
      } catch { if (active) { setCampaigns([]); setError("Chưa tải được ưu đãi đang diễn ra."); } }
    }
    void load();
    const interval = setInterval(() => { if (!document.hidden) void load(); }, 30000);
    const refresh = () => { if (!document.hidden) void load(); };
    document.addEventListener("visibilitychange", refresh);
    window.addEventListener("focus", refresh);
    return () => { active = false; clearInterval(interval); document.removeEventListener("visibilitychange", refresh); window.removeEventListener("focus", refresh); };
  }, [retry]);
  if (error) return <div role="alert" className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">{error} <button type="button" className="underline" onClick={() => setRetry(n => n+1)}>Thử lại</button></div>;
  const visible = campaigns.filter(c => c.products.length);
  if (!visible.length) return null;
  return <section aria-labelledby="promotion-heading" className="mb-8 rounded-xl border border-orange-200 bg-orange-50/60 p-3 sm:p-5">
    <h2 id="promotion-heading" className="mb-4 flex items-center gap-2 text-xl font-extrabold text-orange-900"><Zap className="size-6 text-orange-600" aria-hidden="true" />Flash Sale & ưu đãi hôm nay</h2>
    {visible.map(c => <section key={c.maKM} aria-label={c.tenKM} className="mb-6 last:mb-0">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div><h3 className="text-lg font-bold text-stone-900">{c.tenKM}</h3>{c.moTa && <p className="mt-1 text-sm text-stone-600">{c.moTa}</p>}
          <p className="mt-2 flex items-center gap-1.5 text-sm font-medium text-orange-900"><Clock3 className="size-4" aria-hidden="true" />Đến {promotionTime(c.ngayKetThuc)} · {c.products.length} ưu đãi</p>
        </div>
        {c.products.length > 6 && <button type="button" className="customer-action" aria-expanded={expanded.includes(c.maKM)} onClick={() => setExpanded(ids => ids.includes(c.maKM) ? ids.filter(id => id !== c.maKM) : [...ids,c.maKM])}>{expanded.includes(c.maKM) ? "Thu gọn" : "Xem tất cả ưu đãi"}</button>}
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">{(expanded.includes(c.maKM) ? c.products : c.products.slice(0,6)).map(p => <ProductCard key={p.maSP} product={p} onSelect={onSelect} />)}</div>
    </section>)}
  </section>;
}
