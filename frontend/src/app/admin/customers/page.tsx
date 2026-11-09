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
import { AdminTitle, Confirm, Empty, Notice } from "@/components/admin/AdminUI";
import type { WalletResponse } from "@/types";
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
const customerGroups: Record<CustomerRow["NhomKhachHang"], string> = {
  CHUA_MUA: "Chưa mua",
  DANG_HOAT_DONG: "Đang hoạt động",
  THAN_THIET: "Thân thiết",
  CAN_TUONG_TAC_LAI: "Cần tương tác lại",
};

export default function CustomersPage() {
  const list = useAdminResource<CustomerRow[]>("/api/v1/admin/customers");
  const [query, setQuery] = useState("");
  const [group, setGroup] = useState("");
  const [selected, setSelected] = useState<CustomerRow | null>(null);
  const [ledger, setLedger] = useState<Ledger[]>([]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [amount, setAmount] = useState("");
  const [topUp, setTopUp] = useState<{ customer: CustomerRow; amount: number; requestId: string } | null>(null);
  const rows = list.data?.filter((c) =>
    (!group || c.NhomKhachHang === group) && `${c.HoTen} ${c.Email} ${c.SoDienThoai} ${c.MaKH}`
      .toLocaleLowerCase("vi")
      .includes(query.toLocaleLowerCase("vi")),
  );
  async function inspect(customer: CustomerRow) {
    setPending(true);
    setError("");
    setMessage("");
    setAmount("");
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

  async function confirmTopUp() {
    if (!topUp || pending) return;
    setPending(true);
    setError("");
    try {
      const updated = await adminRequest<WalletResponse>("/api/v1/wallets/top-up", {
        method: "POST",
        body: JSON.stringify({ maKH: topUp.customer.MaKH, soTien: topUp.amount, maYeuCau: topUp.requestId }),
      });
      setTopUp(null);
      setAmount("");
      setSelected({ ...topUp.customer, SoDuVi: updated.soDuVi });
      setMessage(`Đã nạp ${money(topUp.amount)} vào ví demo.`);
      list.refresh();
      setLedger(await adminRequest<Ledger[]>(`/api/v1/admin/customers/${topUp.customer.MaKH}/wallet`));
    } catch (failure) {
      setError(adminError(failure));
    } finally {
      setPending(false);
    }
  }
  return (
    <>
      <AdminTitle
        title="Khách hàng"
        description="Tra cứu khách hàng, phân nhóm theo lịch sử mua và kiểm tra sổ cái ví."
      />
      <Notice error={list.error} />
      <section className="ad-guide-section" aria-labelledby="customer-group-rules">
        <h2 id="customer-group-rules">Cách phân nhóm khách hàng</h2>
        <dl className="ad-guides">
          <div className="ad-guide">
            <dt>Đơn hàng được tính</dt>
            <dd>Dùng đơn đã thanh toán hoặc đã giao. Bỏ qua đơn benchmark và đơn giả lập cho mô hình.</dd>
          </div>
          <div className="ad-guide">
            <dt>Điều kiện vào nhóm thân thiết</dt>
            <dd>
              Có mua trong <strong>30 ngày</strong> qua. Trong <strong>90 ngày</strong> qua, có ít nhất <strong>3 đơn</strong> và chi từ <strong>500.000 đ</strong>.
            </dd>
          </div>
        </dl>
      </section>
      <div className="ad-toolbar">
        <input
          className="ad-input"
          aria-label="Tìm khách hàng"
          placeholder="Tên, email, số điện thoại hoặc mã"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select className="ad-select" aria-label="Nhóm khách hàng" value={group} onChange={(e) => setGroup(e.target.value)}>
          <option value="">Tất cả nhóm</option>
          {Object.entries(customerGroups).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
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
              <th>Nhóm khách hàng</th>
              <th>Mua hàng trong 90 ngày</th>
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
                <td><span className="ad-tag">{customerGroups[c.NhomKhachHang]}</span></td>
                <td>
                  <strong>{c.SoDon90Ngay} đơn · {money(c.ChiTieu90Ngay)}</strong>
                  <small>{c.LanMuaCuoi ? `Mua gần nhất ${datetime(c.LanMuaCuoi)}` : "Chưa có đơn hợp lệ"}</small>
                </td>
                <td className="number">{money(c.SoDuVi)}</td>
                <td>
                  <button
                    className="ad-btn secondary"
                    disabled={pending || !!topUp}
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
          <Notice error={error} message={message} />
          <p className="ad-help">Số dư khả dụng <strong>{money(selected.SoDuVi)}</strong></p>
          <form className="ad-actions" onSubmit={(event) => {
            event.preventDefault();
            setError("");
            setMessage("");
            setTopUp({ customer: selected, amount: Number(amount), requestId: crypto.randomUUID() });
          }}>
            <label className="ad-field">
              Số tiền nạp demo (đ)
              <input className="ad-input" type="number" min={1000} max={999999999999999} step={1}
                value={amount} onChange={(event) => setAmount(event.target.value)} required disabled={pending || !!topUp} />
            </label>
            <button type="submit" className="ad-btn secondary self-end" disabled={pending || !!topUp}>Nạp ví demo</button>
          </form>
          {topUp && <div className="mt-4">
            <Confirm title="Xác nhận nạp ví demo"
              description={`Cộng ${money(topUp.amount)} vào ví của ${topUp.customer.HoTen} và ghi giao dịch vào sổ cái.`}
              pending={pending} onCancel={() => setTopUp(null)} onConfirm={() => void confirmTopUp()} />
          </div>}
          <p className="ad-help mt-4">100 giao dịch gần nhất, mới nhất trước.</p>
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
              disabled={pending || !!topUp}
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
