"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  adminApi,
  adminError,
  AdminOverview,
  datetime,
  money,
} from "@/services/admin-api";
import { AdminTitle, Empty, Notice } from "@/components/admin/AdminUI";
import { alertLabels, AuditEntry } from "@/components/admin/AuditEntry";

export default function AdminDashboard() {
  const [data, setData] = useState<AdminOverview | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    adminApi
      .overview()
      .then((d) => {
        if (active) setData(d);
      })
      .catch((e) => {
        if (active) setError(adminError(e));
      });
    return () => {
      active = false;
    };
  }, []);
  return (
    <>
      <AdminTitle
        title="Tổng quan cửa hàng"
        description="Tình hình hàng hóa, đơn hàng và các cảnh báo cần xử lý."
      />
      <Notice error={error} />
      {!data && !error && <p role="status">Đang tải tổng quan…</p>}
      {data && (
        <>
          <dl className="ad-stats">
            <div>
              <dt>Sản phẩm đang kinh doanh</dt>
              <dd>{data.products}</dd>
            </div>
            <div>
              <dt>Tồn kho ≤ 5</dt>
              <dd>{data.lowStock}</dd>
            </div>
            <div>
              <dt>Cận hạn hoặc hết hạn</dt>
              <dd>{data.expiring}</dd>
            </div>
            <div>
              <dt>Đơn hàng nghiệp vụ</dt>
              <dd>{data.orders}</dd>
            </div>
          </dl>
          <div className="ad-split">
            <section className="ad-panel">
              <h2>Cảnh báo gần đây</h2>
              {data.recentAlerts.length ? (
                <ul className="ad-log-list">
                  {data.recentAlerts.map((a) => (
                    <li key={a.MaLog}>
                      <span className="ad-tag">{alertLabels[a.LoaiSuKien] ?? a.LoaiSuKien}</span>
                      <AuditEntry event={a.LoaiSuKien} content={a.NoiDung} />
                      <small>
                        {datetime(a.NgayGhi)} · {a.NguoiThucHien}
                      </small>
                    </li>
                  ))}
                </ul>
              ) : (
                <Empty>Chưa có cảnh báo.</Empty>
              )}
              <div className="ad-actions">
                <Link className="ad-btn secondary" href="/admin/alerts">
                  Xem nhật ký
                </Link>
              </div>
            </section>
            <aside>
              <section className="ad-panel">
                <h2>Doanh thu đã thanh toán</h2>
                <p style={{ fontSize: 26, fontWeight: 750 }}>
                  {money(data.revenue)}
                </p>
                <p className="ad-help" style={{ marginTop: 12 }}>
                  Tính từ đơn bán đã thanh toán hoặc đã giao. Tách riêng{" "}
                  {(data.demoOrders ?? 0).toLocaleString("vi-VN")} đơn mô phỏng AI và{" "}
                  {data.benchmarkOrders.toLocaleString("vi-VN")} đơn benchmark.
                </p>
              </section>
              <section className="ad-panel">
                <h2>Công việc quản trị</h2>
                <div className="ad-actions">
                  <Link className="ad-btn" href="/admin/products">
                    Quản lý hàng hóa
                  </Link>
                  <Link className="ad-btn secondary" href="/admin/ai">
                    Kiểm tra AI & dự báo
                  </Link>
                  <Link className="ad-btn secondary" href="/admin/orders">
                    Xử lý đơn hàng
                  </Link>
                </div>
              </section>
            </aside>
          </div>
        </>
      )}
    </>
  );
}
