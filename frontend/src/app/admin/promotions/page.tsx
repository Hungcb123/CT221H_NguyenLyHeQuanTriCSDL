"use client";
import { useState } from "react";
import {
  Promotion,
  adminError,
  adminRequest,
  datetime,
  money,
} from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { AdminTitle, Confirm, Empty, Notice } from "@/components/admin/AdminUI";
const promotionTypes = {
  FLASH_SALE: "Giảm giá chớp nhoáng",
  XA_KHO_CAN_DATE: "Xả kho cận hạn",
  GIAM_THEO_MUA: "Giảm giá theo mùa",
  TRI_AN_KHACH_HANG: "Tri ân khách hàng",
};
const fresh = () => ({
  tenKM: "",
  moTa: "",
  loaiKM: "FLASH_SALE",
  ngayBatDau: "",
  ngayKetThuc: "",
});
export default function PromotionsPage() {
  const list = useAdminResource<Promotion[]>(
    "/api/v1/promotions?activeOnly=false",
  );
  const [draft, setDraft] = useState(fresh);
  const [editing, setEditing] = useState<number | null>(null);
  const [form, setForm] = useState(false);
  const [detail, setDetail] = useState<Promotion | null>(null);
  const [product, setProduct] = useState("");
  const [price, setPrice] = useState("");
  const [quota, setQuota] = useState("");
  const [days, setDays] = useState(2);
  const [discount, setDiscount] = useState(30);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [confirm, setConfirm] = useState<{
    title: string;
    description: string;
    path: string;
    method: string;
    body?: string;
  } | null>(null);
  async function inspect(id: number) {
    setPending(true);
    setError("");
    try {
      setDetail(await adminRequest<Promotion>(`/api/v1/promotions/${id}`));
    } catch (e) {
      setError(adminError(e));
    } finally {
      setPending(false);
    }
  }
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setMessage("");
    if (draft.ngayKetThuc <= draft.ngayBatDau) {
      setError("Thời gian kết thúc phải sau thời gian bắt đầu.");
      return;
    }
    setPending(true);
    try {
      await adminRequest(
        editing ? `/api/v1/admin/promotions/${editing}` : "/api/v1/promotions",
        {
          method: editing ? "PUT" : "POST",
          body: JSON.stringify({
            ...draft,
            ngayBatDau: draft.ngayBatDau + ":00",
            ngayKetThuc: draft.ngayKetThuc + ":00",
          }),
        },
      );
      setForm(false);
      list.refresh();
      setDetail(null);
      setMessage("Đã lưu chương trình.");
    } catch (e) {
      setError(adminError(e));
    } finally {
      setPending(false);
    }
  }
  return (
    <>
      <AdminTitle
        title="Khuyến mãi"
        description="Tạo chương trình, quản lý giá ưu đãi và suất bán. Có thể tạm dừng chương trình mà vẫn giữ lịch sử."
      >
        <button
          className="ad-btn"
          onClick={() => {
            setEditing(null);
            setDraft(fresh());
            setForm(true);
          }}
        >
          Thêm chương trình
        </button>
      </AdminTitle>
      <Notice error={error || list.error} message={message} />
      {confirm && (
        <Confirm
          title={confirm.title}
          description={confirm.description}
          pending={pending}
          onCancel={() => setConfirm(null)}
          onConfirm={async () => {
            setPending(true);
            setError("");
            setMessage("");
            try {
              await adminRequest(confirm.path, {
                method: confirm.method,
                body: confirm.body,
              });
              setConfirm(null);
              setDetail(null);
              list.refresh();
              setMessage("Đã cập nhật khuyến mãi.");
            } catch (e) {
              setError(adminError(e));
            } finally {
              setPending(false);
            }
          }}
        />
      )}
      {form && (
        <section className="ad-panel">
          <h2>
            {editing ? `Sửa chương trình #${editing}` : "Chương trình mới"}
          </h2>
          <form onSubmit={submit}>
            <div className="ad-form-grid">
              <label className="ad-field">
                Tên chương trình
                <input
                  className="ad-input"
                  required
                  maxLength={150}
                  value={draft.tenKM}
                  onChange={(e) =>
                    setDraft({ ...draft, tenKM: e.target.value })
                  }
                />
              </label>
              <label className="ad-field">
                Loại
                <select
                  className="ad-select"
                  value={draft.loaiKM}
                  onChange={(e) =>
                    setDraft({ ...draft, loaiKM: e.target.value })
                  }
                >
                  {Object.entries(promotionTypes).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </label>
              <label className="ad-field">
                Bắt đầu
                <input
                  className="ad-input"
                  type="datetime-local"
                  required
                  value={draft.ngayBatDau}
                  onChange={(e) =>
                    setDraft({ ...draft, ngayBatDau: e.target.value })
                  }
                />
              </label>
              <label className="ad-field">
                Kết thúc
                <input
                  className="ad-input"
                  type="datetime-local"
                  required
                  value={draft.ngayKetThuc}
                  onChange={(e) =>
                    setDraft({ ...draft, ngayKetThuc: e.target.value })
                  }
                />
              </label>
              <label className="ad-field ad-span-full">
                Mô tả
                <textarea
                  className="ad-textarea"
                  maxLength={500}
                  value={draft.moTa}
                  onChange={(e) => setDraft({ ...draft, moTa: e.target.value })}
                />
              </label>
            </div>
            <div className="ad-actions">
              <button className="ad-btn" disabled={pending}>
                {pending ? "Đang lưu…" : "Lưu chương trình"}
              </button>
              <button
                type="button"
                className="ad-btn secondary"
                disabled={pending}
                onClick={() => setForm(false)}
              >
                Hủy
              </button>
            </div>
          </form>
        </section>
      )}
      <div className="ad-table-wrap">
        <table className="ad-table">
          <thead>
            <tr>
              <th>Chương trình</th>
              <th>Thời gian</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.map((p) => (
              <tr key={p.maKM}>
                <td>
                  <strong>{p.tenKM}</strong>
                  <small>
                    #{p.maKM} · {p.loaiKM}
                  </small>
                </td>
                <td>
                  {datetime(p.ngayBatDau)}
                  <small>Đến {datetime(p.ngayKetThuc)}</small>
                </td>
                <td>
                  <span className={`ad-tag ${p.trangThai ? "" : "bad"}`}>
                    {p.trangThai ? "Đã bật" : "Tạm dừng"}
                  </span>
                </td>
                <td>
                  <div className="ad-row-actions">
                    <button
                      disabled={pending}
                      onClick={() => void inspect(p.maKM)}
                    >
                      Sản phẩm
                    </button>
                    <button
                      onClick={() => {
                        setEditing(p.maKM);
                        setDraft({
                          tenKM: p.tenKM,
                          moTa: p.moTa || "",
                          loaiKM: p.loaiKM,
                          ngayBatDau: p.ngayBatDau.slice(0, 16),
                          ngayKetThuc: p.ngayKetThuc.slice(0, 16),
                        });
                        setForm(true);
                      }}
                    >
                      Sửa
                    </button>
                    <button
                      disabled={pending}
                      onClick={() =>
                        setConfirm({
                          title: p.trangThai
                            ? "Tạm dừng chương trình?"
                            : "Bật chương trình?",
                          description: `Áp dụng cho ${p.tenKM}. Thời gian hiệu lực vẫn được kiểm tra khi bán hàng.`,
                          path: `/api/v1/admin/promotions/${p.maKM}/active`,
                          method: "PATCH",
                          body: JSON.stringify({ active: !p.trangThai }),
                        })
                      }
                    >
                      {p.trangThai ? "Tạm dừng" : "Bật"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!list.data?.length && (
          <Empty>{list.loading ? "Đang tải…" : "Chưa có chương trình."}</Empty>
        )}
      </div>
      {detail && (
        <section className="ad-panel" style={{ marginTop: 24 }}>
          <h2>Sản phẩm · {detail.tenKM}</h2>
          <div className="ad-table-wrap">
            <table className="ad-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Giá KM</th>
                  <th>Giảm</th>
                  <th>Đã bán / suất</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {detail.items?.map((p) => (
                  <tr key={p.maSP}>
                    <td>
                      {p.tenSP}
                      <small>#{p.maSP}</small>
                    </td>
                    <td>{money(p.giaKhuyenMai)}</td>
                    <td>{p.phanTramGiam}%</td>
                    <td>
                      {p.soLuongDaBan} /{" "}
                      {p.soLuongKhuyenMai ?? "Không giới hạn"}
                    </td>
                    <td>
                      <button
                        className="ad-btn secondary"
                        disabled={pending}
                        onClick={() =>
                          setConfirm({
                            title: "Gỡ sản phẩm khỏi chương trình?",
                            description:
                              "Giá ưu đãi này sẽ không còn được áp dụng cho lần mua mới. Lịch sử đơn hàng vẫn được giữ.",
                            path: `/api/v1/admin/promotions/${detail.maKM}/products/${p.maSP}`,
                            method: "DELETE",
                          })
                        }
                      >
                        Gỡ…
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!detail.items?.length && <Empty>Chưa có sản phẩm.</Empty>}
          </div>
          <h3>Thêm sản phẩm / cập nhật ưu đãi</h3>
          <p className="ad-help">
            Giá khuyến mãi phải thấp hơn giá gốc. Mã sản phẩm có thể tra trong
            mục Sản phẩm.
          </p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setPending(true);
              setError("");
              try {
                await adminRequest(
                  `/api/v1/promotions/${detail.maKM}/products`,
                  {
                    method: "POST",
                    body: JSON.stringify({
                      maSP: Number(product),
                      giaKhuyenMai: Number(price),
                      soLuongKhuyenMai: quota ? Number(quota) : null,
                    }),
                  },
                );
                setDetail(
                  await adminRequest<Promotion>(
                    `/api/v1/promotions/${detail.maKM}`,
                  ),
                );
                setProduct("");
                setPrice("");
                setQuota("");
                list.refresh();
                setMessage("Đã lưu sản phẩm khuyến mãi.");
              } catch (e) {
                setError(adminError(e));
              } finally {
                setPending(false);
              }
            }}
          >
            <div className="ad-form-grid">
              <label className="ad-field">
                Mã sản phẩm
                <input
                  className="ad-input"
                  type="number"
                  min="1"
                  step="1"
                  required
                  value={product}
                  onChange={(e) => setProduct(e.target.value)}
                />
              </label>
              <label className="ad-field">
                Giá ưu đãi (đ)
                <input
                  className="ad-input"
                  type="number"
                  min="1"
                  step="1"
                  required
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                />
              </label>
              <label className="ad-field">
                Số suất (bỏ trống = không giới hạn)
                <input
                  className="ad-input"
                  type="number"
                  min="1"
                  step="1"
                  value={quota}
                  onChange={(e) => setQuota(e.target.value)}
                />
              </label>
            </div>
            <div className="ad-actions">
              <button className="ad-btn" disabled={pending}>
                Lưu ưu đãi
              </button>
              <button
                type="button"
                className="ad-btn secondary"
                onClick={() => setDetail(null)}
              >
                Đóng
              </button>
            </div>
          </form>
        </section>
      )}
      <section className="ad-panel" style={{ marginTop: 24 }}>
        <h2>Xả kho cận hạn</h2>
        <p className="ad-help">
          Quét sản phẩm cận hạn và tạo ưu đãi thực tế bằng stored procedure.
        </p>
        <div className="ad-form-grid" style={{ marginTop: 16 }}>
          <label className="ad-field">
            Số ngày cận hạn
            <input
              className="ad-input"
              type="number"
              min="1"
              max="30"
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
            />
          </label>
          <label className="ad-field">
            Giảm giá (%)
            <input
              className="ad-input"
              type="number"
              min="1"
              max="99"
              value={discount}
              onChange={(e) => setDiscount(Number(e.target.value))}
            />
          </label>
        </div>
        <div className="ad-actions">
          <button
            className="ad-btn secondary"
            disabled={
              pending || days < 1 || days > 30 || discount < 1 || discount > 99
            }
            onClick={() =>
              setConfirm({
                title: "Kích hoạt xả kho cận hạn?",
                description: `Tạo ưu đãi giảm ${discount}% cho hàng còn hạn không quá ${days} ngày.`,
                path: `/api/v1/promotions/auto-clearance?soNgayCanDate=${days}&phanTramGiam=${discount}`,
                method: "POST",
              })
            }
          >
            Quét và áp dụng…
          </button>
        </div>
      </section>
    </>
  );
}
