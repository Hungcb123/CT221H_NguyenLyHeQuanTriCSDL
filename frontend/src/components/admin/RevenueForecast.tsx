"use client";

import { useState } from "react";
import { Empty, Notice } from "@/components/admin/AdminUI";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { money } from "@/services/admin-api";

type RevenueEstimate = {
  source: string;
  historyDays: number;
  sellingDays: number;
  total: number;
  days: { date: string; revenue: number }[];
};

const formatDate = new Intl.DateTimeFormat("vi-VN", { timeZone: "UTC", dateStyle: "medium" });

export function RevenueForecast() {
  const [days, setDays] = useState("7");
  const [source, setSource] = useState("SALE");
  const result = useAdminResource<RevenueEstimate>(
    `/api/v1/admin/ai/revenue-forecast?${new URLSearchParams({ days, source })}`,
  );
  const data = !result.loading && result.data?.source === source &&
    (result.data.days.length === 0 || result.data.days.length === Number(days)) ? result.data : null;

  return (
    <section className="ad-panel" aria-labelledby="revenue-forecast-heading">
      <h2 id="revenue-forecast-heading">Dự báo doanh thu theo kỳ</h2>
      <p className="ad-help">
        Ước tính từ lượng bán theo thứ trong tối đa 56 ngày, giới hạn theo tồn kho hiện tại và hạn sử dụng.
        Áp giá ưu đãi có hiệu lực lúc 12 giờ theo lịch lưu trong hệ thống và trừ dần suất còn lại.
        Dự báo không giả định có nhập hàng mới.
      </p>
      <div className="ad-toolbar">
        <select className="ad-select" aria-label="Kỳ dự báo doanh thu" value={days} onChange={event => setDays(event.target.value)}>
          <option value="1">Ngày mai</option>
          <option value="7">7 ngày tới</option>
          <option value="30">30 ngày tới</option>
        </select>
        <select className="ad-select" aria-label="Nguồn dữ liệu dự báo doanh thu" value={source} onChange={event => setSource(event.target.value)}>
          <option value="SALE">Đơn bán</option>
          <option value="DEMO_FORECAST">Dữ liệu mô phỏng AI</option>
        </select>
        <button type="button" className="ad-btn secondary" disabled={result.loading} onClick={result.refresh}>Tính lại</button>
      </div>
      <Notice error={result.error} />
      {result.loading && <p role="status">Đang ước tính doanh thu…</p>}
      {data && !result.error && (data.days.length ? (
        <>
          <p className="ad-help">
            Có doanh số trong {data.sellingDays}/{data.historyDays} ngày quan sát.
            {source === "DEMO_FORECAST" && " Đang xem kịch bản dùng dữ liệu mô phỏng."}
          </p>
          <p className="my-3 text-xl font-bold text-emerald-900">Tổng dự kiến {money(data.total)}</p>
          <div className="ad-table-wrap">
            <table className="ad-table" style={{ minWidth: 0 }}>
              <thead><tr><th>Ngày</th><th>Doanh thu dự kiến</th></tr></thead>
              <tbody>
                {data.days.map(day => <tr key={day.date}>
                  <td>{formatDate.format(new Date(day.date))}</td><td className="number">{money(day.revenue)}</td>
                </tr>)}
              </tbody>
            </table>
          </div>
        </>
      ) : <Empty>Chưa có đơn đã thanh toán hoặc đã giao trong kỳ dữ liệu đã chọn.</Empty>)}
    </section>
  );
}
