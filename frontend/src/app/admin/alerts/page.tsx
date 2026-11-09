"use client";
import { useState } from "react";
import { datetime } from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { AdminTitle, Empty, Notice } from "@/components/admin/AdminUI";
type Alert = {
  maLog: number;
  loaiSuKien: string;
  noiDung: string;
  maSP: number | null;
  nguoiThucHien: string;
  ngayGhi: string;
};
export default function AlertsPage() {
  const [event, setEvent] = useState("");
  const [product, setProduct] = useState("");
  const list = useAdminResource<Alert[]>(
    product
      ? `/api/v1/alerts/product/${product}`
      : event
        ? `/api/v1/alerts/filter?eventType=${encodeURIComponent(event)}`
        : "/api/v1/alerts",
  );
  return (
    <>
      <AdminTitle
        title="Cảnh báo & kiểm toán"
        description="Nhật ký thực tế từ trigger SQL Server và thao tác quản trị. Mặc định hiển thị 20 sự kiện mới nhất."
      />
      <Notice error={list.error} />
      <div className="ad-toolbar">
        <select
          className="ad-select"
          aria-label="Loại sự kiện"
          value={event}
          onChange={(e) => {
            setEvent(e.target.value);
            setProduct("");
          }}
        >
          <option value="">20 sự kiện mới nhất</option>
          {[
            "CANH_BAO_HET_HANG",
            "CANH_BAO_GIA_BAT_THUONG",
            "CANH_BAO_CAN_DATE",
            "XOA_MEM_SAN_PHAM",
            "ADMIN_ORDER_STATUS",
          ].map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
        <input
          className="ad-input"
          type="number"
          min="1"
          step="1"
          aria-label="Mã sản phẩm để tra toàn bộ nhật ký"
          placeholder="Hoặc lọc theo mã sản phẩm"
          value={product}
          onChange={(e) => {
            setProduct(/^[1-9]\d*$/.test(e.target.value) ? e.target.value : "");
            setEvent("");
          }}
        />
        <button className="ad-btn secondary" onClick={list.refresh}>
          Tải lại
        </button>
      </div>
      <div className="ad-table-wrap">
        <table className="ad-table">
          <thead>
            <tr>
              <th>Thời gian</th>
              <th>Sự kiện</th>
              <th>Nội dung</th>
              <th>Sản phẩm</th>
              <th>Thực hiện</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.map((a) => (
              <tr key={a.maLog}>
                <td>
                  {datetime(a.ngayGhi)}
                  <small>#{a.maLog}</small>
                </td>
                <td>
                  <span className="ad-tag">{a.loaiSuKien}</span>
                </td>
                <td>{a.noiDung}</td>
                <td>{a.maSP ? `#${a.maSP}` : "—"}</td>
                <td>{a.nguoiThucHien || "Hệ thống"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!list.data?.length && (
          <Empty>
            {list.loading ? "Đang tải…" : "Không có sự kiện phù hợp."}
          </Empty>
        )}
      </div>
    </>
  );
}
