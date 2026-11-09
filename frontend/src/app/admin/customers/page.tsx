"use client";
import { useState } from "react";
import {
  CustomerRow,
  adminError,
  adminRequest,
  datetime,
  money,
} from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { AdminTitle, Empty, Notice } from "@/components/admin/AdminUI";
type Ledger = {
  MaGiaoDich: number;
  MaDH: number | null;
  LoaiGiaoDich: string;
  SoDuTruoc: number;
  SoTienThayDoi: number;
  SoDuSau: number;
  NgayGiaoDich: string;
  NoiDung: string;
};
export default function CustomersPage() {
  const list = useAdminResource<CustomerRow[]>("/api/v1/admin/customers");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<CustomerRow | null>(null);
  const [ledger, setLedger] = useState<Ledger[]>([]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const rows = list.data?.filter((c) =>
    `${c.HoTen} ${c.Email} ${c.SoDienThoai} ${c.MaKH}`
      .toLocaleLowerCase("vi")
      .includes(query.toLocaleLowerCase("vi")),
  );
  async function inspect(customer: CustomerRow) {
    setPending(true);
    setError("");
    setSelected(customer);
    setLedger([]);
    try {
      setLedger(
        await adminRequest<Ledger[]>(
          `/api/v1/admin/customers/${customer.MaKH}/wallet`,
        ),
      );
    } catch (e) {
      setError(adminError(e));
    } finally {
      setPending(false);
    }
  }
  return (
    <>
      <AdminTitle
        title="Khách hàng"
        description="Tra cứu thông tin liên hệ, số dư và sổ cái ví. Số dư được thay đổi qua giao dịch, không sửa trực tiếp."
      />
      <Notice error={error || list.error} />
      <div className="ad-toolbar">
        <input
          className="ad-input"
          aria-label="Tìm khách hàng"
          placeholder="Tên, email, số điện thoại hoặc mã"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button className="ad-btn secondary" onClick={list.refresh}>
          Tải lại
        </button>
      </div>
      <div className="ad-table-wrap">
        <table className="ad-table">
          <thead>
            <tr>
              <th>Khách hàng</th>
              <th>Liên hệ</th>
              <th>Địa chỉ</th>
              <th>Số dư ví</th>
              <th>Lịch sử</th>
            </tr>
          </thead>
          <tbody>
            {rows?.map((c) => (
              <tr key={c.MaKH}>
                <td>
                  <strong>{c.HoTen}</strong>
                  <small>#{c.MaKH}</small>
                </td>
                <td>
                  {c.Email}
                  <small>{c.SoDienThoai || "—"}</small>
                </td>
                <td>{c.DiaChi || "—"}</td>
                <td className="number">{money(c.SoDuVi)}</td>
                <td>
                  <button
                    className="ad-btn secondary"
                    disabled={pending}
                    onClick={() => void inspect(c)}
                  >
                    Xem ví
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!rows?.length && (
          <Empty>
            {list.loading ? "Đang tải…" : "Không có khách hàng phù hợp."}
          </Empty>
        )}
      </div>
      {selected && (
        <section className="ad-panel" style={{ marginTop: 24 }}>
          <h2>Ví của {selected.HoTen}</h2>
          <p className="ad-help">100 giao dịch gần nhất, mới nhất trước.</p>
          {pending ? (
            <Empty>Đang tải sổ cái…</Empty>
          ) : (
            <div className="ad-table-wrap">
              <table className="ad-table">
                <thead>
                  <tr>
                    <th>Thời gian / mã</th>
                    <th>Nội dung</th>
                    <th>Trước</th>
                    <th>Thay đổi</th>
                    <th>Sau</th>
                  </tr>
                </thead>
                <tbody>
                  {ledger.map((l) => (
                    <tr key={l.MaGiaoDich}>
                      <td>
                        {datetime(l.NgayGiaoDich)}
                        <small>
                          #{l.MaGiaoDich} · {l.LoaiGiaoDich}
                        </small>
                      </td>
                      <td>
                        {l.NoiDung}
                        {l.MaDH && <small>Đơn #{l.MaDH}</small>}
                      </td>
                      <td className="number">{money(l.SoDuTruoc)}</td>
                      <td className="number">{money(l.SoTienThayDoi)}</td>
                      <td className="number">{money(l.SoDuSau)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!ledger.length && <Empty>Chưa có giao dịch ví.</Empty>}
            </div>
          )}
          <div className="ad-actions">
            <button
              className="ad-btn secondary"
              onClick={() => setSelected(null)}
            >
              Đóng lịch sử
            </button>
          </div>
        </section>
      )}
    </>
  );
}
