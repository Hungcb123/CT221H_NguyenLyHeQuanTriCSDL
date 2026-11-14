"use client";
import { useState, useEffect } from "react";
import {
  Promotion,
  adminError,
  adminRequest,
  datetime,
  money,
} from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { AdminTitle, Confirm, Empty, Notice } from "@/components/admin/AdminUI";
import { promotionInput, promotionUtc, promotionTime } from "@/services/promotion-time";
import { apiService } from "@/services/api";
import { ProductResponse } from "@/types";
const promotionStates = { ACTIVE: "Đang diễn ra", PAUSED: "Tạm dừng", UPCOMING: "Chưa bắt đầu", ENDED: "Đã kết thúc" };
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
  const [days, setDays] = useState("2");
  const [discount, setDiscount] = useState("30");
  const [scanning, setScanning] = useState(false);
  const [scanPreview, setScanPreview] = useState<{
    soNgayCanDate: number;
    phanTramGiam: number;
    tongSanPham: number;
    soSanPhamMoi: number;
    soSanPhamCapNhat: number;
    items: Array<{
      maSP: number;
      tenSP: string;
      donViTinh: string;
      soLuongTon: number;
      hanSuDung: string;
      soNgayConLai: number;
      giaGoc: number;
      giaKhuyenMai: number;
      phanTramGiam: number;
      daCoTrongXaKho: boolean;
    }>;
  } | null>(null);
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
  const [productQuery, setProductQuery] = useState("");
  const [productChoices, setProductChoices] = useState<ProductResponse[]>([]);
  const [typeChoices, setTypeChoices] = useState<ProductResponse[]>([]);
  const [family, setFamily] = useState("");
  const [choicesError, setChoicesError] = useState("");
  const detailId = detail?.maKM;
  useEffect(() => {
    if (!detailId) return;
    let active = true;
    const timeout = setTimeout(() => {
      apiService.getProducts({ keyword: productQuery || undefined, size: 20 }).then(page => {
        if (active) { setProductChoices(page.products); setChoicesError(""); }
      }).catch(() => { if (active) setChoicesError("Không tải được danh sách sản phẩm."); });
    }, 250);
    return () => { active = false; clearTimeout(timeout); };
  }, [detailId, productQuery]);
  useEffect(() => {
    let active = true;
    if (!family) return;
    apiService.getProductTypes(Number(family)).then(items => {
      if (active) { setTypeChoices(items); setProduct(items.length === 1 ? String(items[0].maSP) : ""); setChoicesError(""); }
    }).catch(() => { if (active) setChoicesError("Không tải được quy cách sản phẩm."); });
    return () => { active = false; };
  }, [family]);
  async function inspect(id: number) {
    setFamily(""); setProduct(""); setTypeChoices([]); setPrice(""); setQuota("");
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
      const saved = await adminRequest<Promotion>(
        editing ? `/api/v1/admin/promotions/${editing}` : "/api/v1/promotions",
        {
          method: editing ? "PUT" : "POST",
          body: JSON.stringify({
            ...draft,
            ngayBatDau: promotionUtc(draft.ngayBatDau),
            ngayKetThuc: promotionUtc(draft.ngayKetThuc),
          }),
        },
      );
      setForm(false);
      list.refresh();
      setDetail(await adminRequest<Promotion>(`/api/v1/promotions/${editing ?? saved.maKM}`));
      setMessage("Đã lưu chương trình. Thêm sản phẩm và giá ưu đãi bên dưới để khách có thể mua.");
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
              const result = await adminRequest<{ maKM?: number; soLuongSanPhamDaThem?: number }>(confirm.path, {
                method: confirm.method,
                body: confirm.body,
              });
              setConfirm(null);
              setDetail(null);
              list.refresh();
              if (confirm.path.includes("auto-clearance")) {
                setScanPreview(null);
                setMessage(
                  `Đã áp dụng xả kho thành công: ${result.soLuongSanPhamDaThem ?? 0} sản phẩm cận hạn đã được cập nhật ưu đãi.`,
                );
              } else {
                setMessage("Đã cập nhật khuyến mãi.");
              }
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
                  {promotionTime(p.ngayBatDau)}
                  <small>Đến {promotionTime(p.ngayKetThuc)}</small>
                </td>
                <td>
                  <span className={`ad-tag ${p.trangThai ? "" : "bad"}`}>
                    {promotionStates[p.trangThaiHienThi] || (p.trangThai ? "Đã bật" : "Tạm dừng")}
                  </span>
                  <small>{p.soLuongSanPham || 0} sản phẩm{!p.soLuongSanPham ? " · Chưa có ưu đãi để bán" : ""}</small>
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
                          ngayBatDau: promotionInput(p.ngayBatDau),
                          ngayKetThuc: promotionInput(p.ngayKetThuc),
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
          <p className="ad-help">Chỉ ưu đãi còn hiệu lực, còn suất và còn hàng bán được mới xuất hiện trên trang chủ. Với sản phẩm nhiều quy cách, hãy thêm từng quy cách cần giảm giá.</p>
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
            Chọn sản phẩm và đúng quy cách bán. Giá ưu đãi phải thấp hơn giá gốc.
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
              <label className="ad-field">Tìm sản phẩm<input className="ad-input" value={productQuery} onChange={e => setProductQuery(e.target.value)} placeholder="Tên sản phẩm…" /></label>
              <label className="ad-field">Sản phẩm<select className="ad-select" required value={family} onChange={e => { setFamily(e.target.value); setProduct(""); setTypeChoices([]); }}><option value="">Chọn sản phẩm</option>{productChoices.map(p => <option key={p.maSP} value={p.maSP}>{p.tenSP}</option>)}</select></label>
              <label className="ad-field">Quy cách / mã bán<select className="ad-select" required value={product} onChange={e => setProduct(e.target.value)}><option value="">Chọn quy cách</option>{typeChoices.map(p => <option key={p.maSP} value={p.maSP}>{p.tenLoai || p.tenSP} · #{p.maSP} · {money(p.giaGoc)}</option>)}</select></label>
              {choicesError && <p role="alert">{choicesError}</p>}
              <label className="ad-field">
                Giá ưu đãi (đ)
                <input
                  className="ad-input"
                  type="text"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  placeholder="Nhập giá ưu đãi, VD: 25000"
                  required
                  value={price}
                  onChange={(e) => setPrice(e.target.value.replace(/\D+/g, "").replace(/^0+/, ""))}
                />
                {price && <small>{Number(price).toLocaleString("vi-VN")} đ</small>}
              </label>
              <label className="ad-field">
                Số suất (bỏ trống = không giới hạn)
                <input
                  className="ad-input"
                  type="text"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  placeholder="Bỏ trống nếu không giới hạn"
                  value={quota}
                  onChange={(e) => setQuota(e.target.value.replace(/\D+/g, "").replace(/^0+/, ""))}
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
          Bước 1: Quét danh sách mặt hàng sắp hết hạn để kiểm tra giá và tồn kho. Bước 2: Xác nhận áp dụng ưu đãi xả kho bằng Stored Procedure.
        </p>
        <div className="ad-form-grid" style={{ marginTop: 16 }}>
          <label className="ad-field">
            Số ngày cận hạn
            <input
              className="ad-input"
              type="text"
              inputMode="numeric"
              placeholder="1 - 30"
              value={days}
              onChange={(e) => setDays(e.target.value.replace(/\D+/g, "").replace(/^0+/, ""))}
            />
          </label>
          <label className="ad-field">
            Giảm giá (%)
            <input
              className="ad-input"
              type="text"
              inputMode="numeric"
              placeholder="1 - 99"
              value={discount}
              onChange={(e) => setDiscount(e.target.value.replace(/\D+/g, "").replace(/^0+/, ""))}
            />
          </label>
        </div>
        {(() => {
          const validInputs =
            !!days &&
            !!discount &&
            Number(days) >= 1 &&
            Number(days) <= 30 &&
            Number(discount) >= 1 &&
            Number(discount) <= 99;
          const isCurrentScan =
            !!scanPreview &&
            scanPreview.soNgayCanDate === Number(days) &&
            Number(scanPreview.phanTramGiam) === Number(discount);
          const canApply = validInputs && isCurrentScan && scanPreview.items.length > 0 && !pending && !scanning;

          return (
            <>
              <div className="ad-actions">
                <button
                  type="button"
                  className="ad-btn secondary"
                  disabled={!validInputs || pending || scanning}
                  onClick={async () => {
                    setScanning(true);
                    setError("");
                    setMessage("");
                    try {
                      const res = await adminRequest<NonNullable<typeof scanPreview>>(
                        `/api/v1/promotions/auto-clearance/preview?soNgayCanDate=${Number(days)}&phanTramGiam=${Number(discount)}`,
                      );
                      setScanPreview(res);
                    } catch (e) {
                      setError(adminError(e));
                    } finally {
                      setScanning(false);
                    }
                  }}
                >
                  {scanning ? "Đang quét kho…" : isCurrentScan ? "1. Quét lại danh sách" : "1. Quét hàng cận hạn"}
                </button>

                <button
                  type="button"
                  className="ad-btn"
                  disabled={!canApply}
                  title={
                    !isCurrentScan
                      ? "Vui lòng bấm Quét hàng cận hạn trước khi áp dụng"
                      : scanPreview?.items.length === 0
                        ? "Không có sản phẩm cận hạn nào để áp dụng"
                        : undefined
                  }
                  onClick={() =>
                    setConfirm({
                      title: "Xác nhận áp dụng xả kho cận hạn?",
                      description: `Áp dụng mức giảm ${Number(discount)}% cho ${scanPreview!.items.length} sản phẩm còn hạn không quá ${Number(days)} ngày vừa quét.`,
                      path: `/api/v1/promotions/auto-clearance?soNgayCanDate=${Number(days)}&phanTramGiam=${Number(discount)}`,
                      method: "POST",
                    })
                  }
                >
                  {isCurrentScan && scanPreview.items.length > 0
                    ? `2. Áp dụng xả kho (${scanPreview.items.length} sản phẩm)`
                    : "2. Áp dụng xả kho"}
                </button>
              </div>

              {scanPreview && !isCurrentScan && (
                <p className="ad-help" style={{ marginTop: 10, color: "#9a3412" }}>
                  Thông số ngày hoặc mức giảm vừa thay đổi — vui lòng bấm <strong>1. Quét hàng cận hạn</strong> để cập nhật danh sách trước khi áp dụng.
                </p>
              )}

              {scanPreview && isCurrentScan && (
                <div style={{ marginTop: 16 }}>
                  {scanPreview.items.length === 0 ? (
                    <Empty>
                      Không có sản phẩm nào còn hạn trong vòng {scanPreview.soNgayCanDate} ngày tới cần xả kho.
                    </Empty>
                  ) : (
                    <>
                      <p className="ad-help" style={{ marginBottom: 10 }}>
                        Kết quả quét: tìm thấy <strong>{scanPreview.tongSanPham}</strong> mặt hàng còn hạn &le;{" "}
                        <strong>{scanPreview.soNgayCanDate} ngày</strong> ({scanPreview.soSanPhamMoi} sản phẩm mới,{" "}
                        {scanPreview.soSanPhamCapNhat} sản phẩm cập nhật mức giảm {Number(scanPreview.phanTramGiam)}%).
                      </p>
                      <div className="ad-table-wrap">
                        <table className="ad-table">
                          <thead>
                            <tr>
                              <th>Mặt hàng</th>
                              <th>Tồn kho</th>
                              <th>Hạn sử dụng</th>
                              <th>Giá gốc</th>
                              <th>Giá sau giảm (-{Number(scanPreview.phanTramGiam)}%)</th>
                              <th>Trạng thái</th>
                            </tr>
                          </thead>
                          <tbody>
                            {scanPreview.items.map((item) => (
                              <tr key={item.maSP}>
                                <td>
                                  <strong>{item.tenSP}</strong>
                                  <small>
                                    #{item.maSP} · {item.donViTinh}
                                  </small>
                                </td>
                                <td>{item.soLuongTon}</td>
                                <td>
                                  {datetime(item.hanSuDung)}
                                  <small>Còn khoảng {item.soNgayConLai} ngày</small>
                                </td>
                                <td className="number">{money(item.giaGoc)}</td>
                                <td className="number">
                                  <strong style={{ color: "#08783c" }}>{money(item.giaKhuyenMai)}</strong>
                                </td>
                                <td>
                                  <span className="ad-tag">
                                    {item.daCoTrongXaKho ? "Cập nhật giá xả kho" : "Mới đưa vào xả kho"}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </>
                  )}
                </div>
              )}
            </>
          );
        })()}
      </section>
    </>
  );
}
