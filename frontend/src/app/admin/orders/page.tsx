"use client";
import { useState } from "react";
import {
  adminError,
  adminRequest,
  AdminPage,
  AdminOrder,
  money,
  datetime,
} from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import {
  AdminTitle,
  Confirm,
  Empty,
  Notice,
  Pagination,
} from "@/components/admin/AdminUI";

const orderStatuses: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  PAID: "Đã thanh toán",
  DELIVERED: "Đã giao hàng",
  CANCELLED: "Đã hủy",
};

interface Detail {
  maDH: number;
  tenKhachHang: string;
  soDienThoai: string;
  diaChiGiaoHang: string;
  trangThai: string;
  tongTien: number;
  items: {
    maSP: number;
    tenSP: string;
    soLuong: number;
    donGia: number;
    thanhTien: number;
  }[];
}
export default function OrdersPage() {
  const [status, setStatus] = useState("");
  const [includeBenchmark, setIncludeBenchmark] = useState(false);
  const [page, setPage] = useState(1);
  const list = useAdminResource<AdminPage<AdminOrder>>(
    `/api/v1/admin/orders?${new URLSearchParams({ status, includeBenchmark: String(includeBenchmark), page: String(page) })}`,
  );
  const [detail, setDetail] = useState<Detail | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [pending, setPending] = useState(false);
  const [change, setChange] = useState<{
    id: number;
    status: string;
    paid: boolean;
  } | null>(null);
  return (
    <>
      <AdminTitle
        title="Đơn hàng"
        description="Xem hóa đơn, xác nhận giao hàng hoặc hủy đơn với hoàn ví và tồn kho nguyên tử."
      />
      <Notice error={error || list.error} message={message} />
      {change && (
        <Confirm
          title={`${change.status === "DELIVERED" ? "Xác nhận đã giao" : "Hủy"} đơn #${change.id}?`}
          description={
            change.status === "DELIVERED"
              ? "Chỉ xác nhận sau khi đơn đã được giao thành công."
              : change.paid
                ? "Hệ thống sẽ hoàn toàn bộ tiền vào ví, hoàn số lượng tồn và ghi sổ cái trong cùng giao dịch. Không thực hiện lại nếu đơn đã hủy."
                : "Đơn chưa thanh toán sẽ được chuyển sang đã hủy."
          }
          pending={pending}
          onCancel={() => setChange(null)}
          onConfirm={async () => {
            setPending(true);
            setError("");
            try {
              await adminRequest(`/api/v1/admin/orders/${change.id}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status: change.status }),
              });
              setChange(null);
              setDetail(null);
              list.refresh();
              setMessage("Đã xử lý đơn hàng.");
            } catch (e) {
              setError(adminError(e));
            } finally {
              setPending(false);
            }
          }}
        />
      )}
      <div className="ad-toolbar">
        <select
          className="ad-select"
          aria-label="Trạng thái đơn hàng"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(1);
          }}
        >
          <option value="">Tất cả trạng thái</option>
          {Object.entries(orderStatuses).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        <label>
          <input
            type="checkbox"
            checked={includeBenchmark}
            onChange={(e) => {
              setIncludeBenchmark(e.target.checked);
              setPage(1);
            }}
          />{" "}
          Hiển thị thêm đơn benchmark
        </label>
        <button className="ad-btn secondary" onClick={list.refresh}>
          Tải lại
        </button>
      </div>
      {detail && (
        <section className="ad-panel">
          <div className="ad-toolbar">
            <h2>Hóa đơn #{detail.maDH}</h2>
            <button
              className="ad-btn secondary"
              onClick={() => setDetail(null)}
            >
              Đóng chi tiết
            </button>
          </div>
          <p>
            {detail.tenKhachHang} · {detail.soDienThoai}
          </p>
          <p className="ad-help">{detail.diaChiGiaoHang}</p>
          <div className="ad-table-wrap" style={{ marginTop: 16 }}>
            <table className="ad-table">
              <thead>
                <tr>
                  <th>Mặt hàng</th>
                  <th>Số lượng</th>
                  <th>Đơn giá</th>
                  <th>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                {detail.items.map((i) => (
                  <tr key={i.maSP}>
                    <td>{i.tenSP}</td>
                    <td>{i.soLuong}</td>
                    <td>{money(i.donGia)}</td>
                    <td>{money(i.thanhTien)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p style={{ marginTop: 16, fontWeight: 700 }}>
            Tổng: {money(detail.tongTien)}
          </p>
        </section>
      )}
      <div className="ad-table-wrap">
        <table className="ad-table">
          <thead>
            <tr>
              <th>Đơn hàng</th>
              <th>Khách hàng</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.items.map((o) => (
              <tr key={o.MaDH}>
                <td>
                  <strong>#{o.MaDH}</strong>
                  <small>{datetime(o.NgayTao)}</small>
                  {o.NguonDon === "BENCHMARK" && <span className="ad-tag">Benchmark</span>}
                </td>
                <td>
                  {o.HoTen}
                  <small>{o.DiaChiGiaoHang}</small>
                </td>
                <td className="number">{money(o.TongTien)}</td>
                <td>
                  <span className="ad-tag">{orderStatuses[o.TrangThai] ?? o.TrangThai}</span>
                </td>
                <td>
                  <div className="ad-row-actions">
                    <button
                      onClick={async () => {
                        setError("");
                        try {
                          setDetail(
                            await adminRequest<Detail>(
                              `/api/v1/admin/orders/${o.MaDH}`,
                            ),
                          );
                          window.scrollTo({ top: 0, behavior: "smooth" });
                        } catch (e) {
                          setError(adminError(e));
                        }
                      }}
                    >
                      Chi tiết
                    </button>
                    {o.NguonDon === "SALE" && o.TrangThai === "PAID" && (
                      <button
                        onClick={() =>
                          setChange({
                            id: o.MaDH,
                            status: "DELIVERED",
                            paid: true,
                          })
                        }
                      >
                        Đã giao
                      </button>
                    )}
                    {o.NguonDon === "SALE" && ["PAID", "PENDING"].includes(o.TrangThai) && (
                      <button
                        onClick={() =>
                          setChange({
                            id: o.MaDH,
                            status: "CANCELLED",
                            paid: o.TrangThai === "PAID",
                          })
                        }
                      >
                        Hủy đơn
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!list.data?.items.length && (
          <Empty>
            {list.loading ? "Đang tải đơn hàng…" : "Không có đơn hàng phù hợp."}
          </Empty>
        )}
      </div>
      <Pagination
        page={page}
        totalPages={list.data?.totalPages ?? 0}
        onChange={setPage}
      />
    </>
  );
}
