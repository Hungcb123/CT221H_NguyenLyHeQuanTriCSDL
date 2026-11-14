"use client";

import { useEffect, useRef, useState } from "react";
import { Plus, RotateCw, Upload } from "lucide-react";
import { Category, ProductResponse } from "@/types";
import {
  adminError,
  adminRequest,
  AdminPage,
  AdminProduct,
  datetime,
  money,
  uploadAdminProductImage,
} from "@/services/admin-api";
import {
  cleanNonNegativeIntInput,
  cleanPositiveIntInput,
  ProductTypeEditor,
  ProductTypeFormItem,
} from "@/components/admin/ProductTypeEditor";
import { useAdminResource } from "@/components/admin/useAdminResource";
import {
  AdminTitle,
  Confirm,
  Empty,
  Notice,
  Pagination,
} from "@/components/admin/AdminUI";

interface ProductFormDraft {
  tenSP: string;
  hinhAnh: string;
  moTa: string;
  gia: string;
  donViTinh: string;
  soLuongTon: string;
  hanSuDung: string;
  nhietDoBaoQuan: string;
  maDanhMuc: number;
  cacLoai: ProductTypeFormItem[];
}

const DRAFT_STORAGE_KEY = "dailymart_admin_new_product_draft_v1";

function defaultExpiryLocal(daysAhead = 7): string {
  const d = new Date(Date.now() + daysAhead * 24 * 60 * 60 * 1000);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const fresh = (category: number): ProductFormDraft => ({
  tenSP: "",
  hinhAnh: "",
  moTa: "",
  gia: "",
  donViTinh: "",
  soLuongTon: "",
  hanSuDung: defaultExpiryLocal(7),
  nhietDoBaoQuan: "",
  maDanhMuc: category,
  cacLoai: [],
});

type Confirmation = {
  title: string;
  description: string;
  run: () => Promise<unknown>;
};

export default function ProductAdminPage() {
  const [query, setQuery] = useState("");
  const [deleted, setDeleted] = useState(false);
  const [page, setPage] = useState(1);
  const list = useAdminResource<AdminPage<AdminProduct>>(
    `/api/v1/admin/products?${new URLSearchParams({ query, deleted: String(deleted), page: String(page), size: "20" })}`,
  );
  const categories = useAdminResource<Category[]>("/api/v1/categories");
  const [typesLoading, setTypesLoading] = useState(false);
  const [typesError, setTypesError] = useState("");
  const [typesUploading, setTypesUploading] = useState(false);
  const loadVersion = useRef(0);
  const [draft, setDraft] = useState<ProductFormDraft | null>(() => {
    if (typeof window === "undefined") return null;
    try {
      const saved = window.sessionStorage.getItem(DRAFT_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as ProductFormDraft;
        if (parsed && typeof parsed.tenSP === "string") return parsed;
      }
    } catch {}
    return null;
  });
  const [editing, setEditing] = useState<number | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const categoryName = (id: number) =>
    categories.data?.find((c) => c.maDanhMuc === id)?.tenDanhMuc ?? `#${id}`;

  useEffect(() => {
    try {
      if (draft && editing === null) {
        window.sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
      } else if (!draft && editing === null) {
        window.sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      }
    } catch {}
  }, [draft, editing]);

  function field<K extends keyof ProductFormDraft>(
    key: K,
    value: ProductFormDraft[K],
  ) {
    setDraft((current) => (current ? { ...current, [key]: value } : current));
  }

  return (
    <>
      <AdminTitle
        title="Sản phẩm"
        description="Quản lý sản phẩm và thêm các loại ngay trong form tạo hoặc chỉnh sửa."
      >
        <button
          className="ad-btn"
          disabled={!categories.data?.length || pending || typesUploading}
          onClick={() => {
            loadVersion.current++;
            setTypesLoading(false);
            setTypesError("");
            setDraft(fresh(categories.data![0].maDanhMuc));
            setEditing(null);
            setMessage("");
            setError("");
            window.scrollTo({ top: 0, behavior: "smooth" });
          }}
        >
          <Plus size={17} aria-hidden="true" />
          Thêm sản phẩm
        </button>
      </AdminTitle>
      <Notice
        error={error || list.error || categories.error}
        message={message}
      />
      {confirmation && (
        <Confirm
          title={confirmation.title}
          description={confirmation.description}
          pending={pending}
          onCancel={() => setConfirmation(null)}
          onConfirm={async () => {
            setPending(true);
            setError("");
            setMessage("");
            try {
              await confirmation.run();
              setConfirmation(null);
              setMessage("Đã cập nhật sản phẩm.");
              list.refresh();
            } catch (e) {
              setError(adminError(e));
            } finally {
              setPending(false);
            }
          }}
        />
      )}
      {draft && (
        <section className="ad-panel">
          <h2>
            {editing ? `Sửa sản phẩm #${editing}` : "Thông tin sản phẩm mới"}
          </h2>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setPending(true);
              setError("");
              setMessage("");
              try {
                const parsedGia = Number(draft.gia);
                const parsedTon = Number(draft.soLuongTon);
                if (!draft.gia || !Number.isFinite(parsedGia) || parsedGia < 1) {
                  throw new Error("Vui lòng nhập giá niêm yết hợp lệ (từ 1đ trở lên).");
                }
                if (draft.soLuongTon === "" || !Number.isFinite(parsedTon) || parsedTon < 0) {
                  throw new Error("Vui lòng nhập số lượng tồn kho hợp lệ (từ 0 trở lên).");
                }
                if (!editing && new Date(draft.hanSuDung) <= new Date()) {
                  throw new Error("Hạn dùng sản phẩm mới phải ở tương lai.");
                }
                await adminRequest(
                  `/api/v1/products${editing ? `/${editing}` : ""}`,
                  {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify({
                      ...draft,
                      tenSP: draft.tenSP.trim(),
                      donViTinh: draft.donViTinh.trim(),
                      gia: parsedGia,
                      soLuongTon: parsedTon,
                      hanSuDung: draft.hanSuDung.length === 16 ? `${draft.hanSuDung}:00` : draft.hanSuDung,
                      cacLoai: (draft.cacLoai || []).map((type) => ({
                        ...type,
                        tenLoai: type.tenLoai.trim(),
                        donViTinh: type.donViTinh.trim(),
                        gia: Number(type.gia),
                        soLuongTon: Number(type.soLuongTon),
                        hanSuDung: type.hanSuDung.length === 16 ? type.hanSuDung + ":00" : type.hanSuDung,
                      })),
                    }),
                  },
                );
                try {
                  window.sessionStorage.removeItem(DRAFT_STORAGE_KEY);
                } catch {}
                setDraft(null);
                setEditing(null);
                setMessage(
                  editing ? "Đã lưu thay đổi." : "Đã tạo sản phẩm.",
                );
                list.refresh();
              } catch (failure) {
                setError(adminError(failure));
              } finally {
                setPending(false);
              }
            }}
          >
            <div className="ad-form-grid">
              <label className="ad-field">
                Tên sản phẩm
                <input
                  className="ad-input"
                  value={draft.tenSP}
                  onChange={(e) => field("tenSP", e.target.value)}
                  placeholder="Ví dụ: Cà rốt mini Đà Lạt"
                  required
                  maxLength={200}
                />
              </label>
              <label className="ad-field">
                Danh mục
                <select
                  className="ad-select"
                  value={draft.maDanhMuc}
                  onChange={(e) => field("maDanhMuc", Number(e.target.value))}
                >
                  {categories.data?.map((c) => (
                    <option key={c.maDanhMuc} value={c.maDanhMuc}>
                      {c.tenDanhMuc}
                    </option>
                  ))}
                </select>
              </label>
              <label className="ad-field">
                Giá niêm yết (đ)
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  className="ad-input"
                  placeholder="Nhập giá bán, VD: 35000"
                  value={draft.gia}
                  onChange={(e) => field("gia", cleanPositiveIntInput(e.target.value))}
                  required
                />
                {draft.gia && (
                  <small>{Number(draft.gia).toLocaleString("vi-VN")} đ</small>
                )}
              </label>
              <label className="ad-field">
                Đơn vị tính
                <input
                  className="ad-input"
                  value={draft.donViTinh}
                  onChange={(e) => field("donViTinh", e.target.value)}
                  placeholder="kg, bó, khay…"
                  required
                  maxLength={20}
                />
              </label>
              <label className="ad-field">
                Số lượng tồn
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]+"
                  className="ad-input"
                  placeholder="Nhập số lượng tồn, VD: 50"
                  value={draft.soLuongTon}
                  onChange={(e) => field("soLuongTon", cleanNonNegativeIntInput(e.target.value))}
                  required
                />
              </label>
              <label className="ad-field">
                Hạn sử dụng
                <input
                  type="datetime-local"
                  className="ad-input"
                  value={draft.hanSuDung}
                  onChange={(e) => field("hanSuDung", e.target.value)}
                  required
                />
              </label>
              <label className="ad-field">
                Nhiệt độ bảo quản
                <input
                  className="ad-input"
                  value={draft.nhietDoBaoQuan ?? ""}
                  onChange={(e) => field("nhietDoBaoQuan", e.target.value)}
                  placeholder="Ví dụ: Ngăn mát 4 - 8°C"
                  maxLength={50}
                />
              </label>
              <div className="ad-field ad-span-full">
                <span>Ảnh sản phẩm</span>
                <input
                  className="ad-input"
                  value={draft.hinhAnh ?? ""}
                  onChange={(e) => field("hinhAnh", e.target.value)}
                  placeholder="Dán URL ảnh https://… hoặc tải ảnh từ máy bên dưới"
                  maxLength={1000}
                />
                <div className="mt-1 flex flex-wrap items-center gap-3">
                  <label
                    className="ad-btn secondary"
                    style={{ cursor: pending ? "not-allowed" : "pointer" }}
                  >
                    <Upload size={15} aria-hidden="true" />
                    {pending ? "Đang tải ảnh…" : "Chọn ảnh từ máy"}
                    <input
                      type="file"
                      hidden
                      accept="image/*"
                      disabled={pending}
                      onChange={async (e) => {
                        const file = e.target.files?.[0];
                        e.target.value = "";
                        if (!file) return;
                        setPending(true);
                        setError("");
                        setMessage("");
                        try {
                          const uploaded = await uploadAdminProductImage(
                            file,
                            editing ?? undefined,
                          );
                          field("hinhAnh", uploaded.url);
                          if (uploaded.savedToProduct) {
                            setMessage("Đã tải ảnh lên và lưu trực tiếp vào sản phẩm.");
                            list.refresh();
                          } else {
                            setMessage("Đã tải ảnh lên máy chủ thành công (ảnh đã được giữ sẵn trong biểu mẫu).");
                          }
                        } catch (failure) {
                          setError(adminError(failure));
                        } finally {
                          setPending(false);
                        }
                      }}
                    />
                  </label>
                  <small>
                    Hỗ trợ JPG, PNG, WebP… Khi sửa sản phẩm có sẵn, ảnh tải lên sẽ tự động lưu luôn.
                  </small>
                </div>
                {draft.hinhAnh && (
                  <div className="mt-2 flex items-center gap-4">
                    <img
                      src={draft.hinhAnh}
                      alt="Xem trước ảnh sản phẩm"
                      className="h-32 w-32 rounded-lg border object-contain bg-white"
                    />
                    <button
                      type="button"
                      className="ad-btn secondary"
                      disabled={pending}
                      onClick={() => field("hinhAnh", "")}
                    >
                      Xóa ảnh
                    </button>
                  </div>
                )}
              </div>
              <label className="ad-field ad-span-full">
                Mô tả và công dụng
                <textarea
                  className="ad-textarea"
                  value={draft.moTa ?? ""}
                  onChange={(e) => field("moTa", e.target.value)}
                  placeholder="Mô tả hương vị, cách chế biến, món ăn phù hợp, giá trị dinh dưỡng…"
                />
                <small>
                  Dùng mô tả đúng thực tế để hỗ trợ tìm kiếm ngữ nghĩa.
                </small>
              </label>
            </div>
            <ProductTypeEditor
              types={draft.cacLoai || []}
              product={draft}
              disabled={pending || typesLoading}
              onChange={(values) => field("cacLoai", values)}
              onUploading={setTypesUploading}
            />
            {typesLoading && <p role="status">Đang tải các loại của sản phẩm…</p>}
            {typesError && (
              <p role="alert" className="text-red-800">
                {typesError} Hãy mở lại sản phẩm để tải lại trước khi lưu.
              </p>
            )}
            <div className="ad-actions">
              <button
                className="ad-btn"
                disabled={pending || typesLoading || !!typesError || typesUploading}
              >
                {pending ? "Đang lưu…" : "Lưu sản phẩm"}
              </button>
              <button
                type="button"
                className="ad-btn secondary"
                disabled={pending}
                onClick={() => {
                  loadVersion.current++;
                  try {
                    window.sessionStorage.removeItem(DRAFT_STORAGE_KEY);
                  } catch {}
                  setDraft(null);
                  setEditing(null);
                  setTypesError("");
                }}
              >
                Hủy chỉnh sửa
              </button>
            </div>
          </form>
        </section>
      )}
      <div className="ad-toolbar">
        <input
          aria-label="Tìm sản phẩm quản trị"
          className="ad-input"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setPage(1);
          }}
          placeholder="Tìm tên hoặc mã sản phẩm…"
        />
        <select
          className="ad-select"
          aria-label="Trạng thái sản phẩm"
          value={String(deleted)}
          onChange={(e) => {
            setDeleted(e.target.value === "true");
            setPage(1);
          }}
        >
          <option value="false">Đang kinh doanh</option>
          <option value="true">Đã xóa</option>
        </select>
        <button className="ad-btn secondary" onClick={list.refresh}>
          <RotateCw size={15} aria-hidden="true" />
          Tải lại
        </button>
        <span className="ad-help">
          {list.data?.totalElements ?? 0} sản phẩm
        </span>
      </div>
      {list.loading && (
        <p role="status" className="ad-help">
          Đang tải danh sách…
        </p>
      )}
      <div className="ad-table-wrap">
        <table className="ad-table">
          <thead>
            <tr>
              <th>Mặt hàng</th>
              <th>Danh mục</th>
              <th>Giá niêm yết</th>
              <th>Tồn</th>
              <th>Hạn dùng</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.items.map((p) => (
              <tr key={p.maSP}>
                <td>
                  {p.hinhAnh && (
                    <img
                      src={p.hinhAnh}
                      alt={p.tenSP}
                      className="mb-2 h-14 w-14 rounded-md border bg-white object-contain"
                      loading="lazy"
                    />
                  )}
                  <strong>{p.tenSP}</strong>
                  <small>
                    #{p.maSP} · {p.donViTinh}
                  </small>
                </td>
                <td>{categoryName(p.maDanhMuc)}</td>
                <td className="number">{money(p.gia)}</td>
                <td>
                  <span className={`ad-tag ${p.soLuongTon <= 5 ? "bad" : ""}`}>
                    {p.soLuongTon}
                  </span>
                </td>
                <td>
                  {datetime(p.hanSuDung)}
                  {new Date(p.hanSuDung) < new Date() && (
                    <small>Đã hết hạn</small>
                  )}
                </td>
                <td>
                  <div className="ad-row-actions">
                    {!deleted && (
                      <button
                        disabled={pending || typesUploading}
                        onClick={async () => {
                          const version = ++loadVersion.current;
                          setTypesLoading(true);
                          setTypesError("");
                          setEditing(p.maSP);
                          setDraft({
                            tenSP: p.tenSP,
                            hinhAnh: p.hinhAnh ?? "",
                            moTa: p.moTa ?? "",
                            gia: String(p.gia),
                            donViTinh: p.donViTinh,
                            soLuongTon: String(p.soLuongTon),
                            hanSuDung: p.hanSuDung.slice(0, 16),
                            nhietDoBaoQuan: p.nhietDoBaoQuan ?? "",
                            maDanhMuc: p.maDanhMuc,
                            cacLoai: [],
                          });
                          setError("");
                          window.scrollTo({ top: 0, behavior: "smooth" });
                          try {
                            const types = await adminRequest<ProductResponse[]>(
                              `/api/v1/admin/products/${p.maSP}/types`,
                            );
                            if (version === loadVersion.current) {
                              setDraft((current) =>
                                current
                                  ? {
                                      ...current,
                                      cacLoai: types.map((t) => ({
                                        maSP: t.maSP,
                                        tenLoai: t.tenLoai || "",
                                        gia: String(t.giaGoc),
                                        donViTinh: t.donViTinh,
                                        soLuongTon: String(t.soLuongTon),
                                        hanSuDung: t.hanSuDung.slice(0, 16),
                                        nhietDoBaoQuan: t.nhietDoBaoQuan ?? "",
                                        hinhAnh: t.hinhAnh ?? "",
                                        moTa: t.moTa ?? "",
                                        trangThaiXoa: t.trangThaiXoa,
                                      })),
                                    }
                                  : current,
                              );
                            }
                          } catch (failure) {
                            if (version === loadVersion.current) {
                              setTypesError(adminError(failure));
                            }
                          } finally {
                            if (version === loadVersion.current) {
                              setTypesLoading(false);
                            }
                          }
                        }}
                      >
                        Sửa
                      </button>
                    )}
                    <button
                      onClick={() =>
                        setConfirmation({
                          title: deleted
                            ? `Khôi phục ${p.tenSP}?`
                            : `Xóa ${p.tenSP}?`,
                          description: deleted
                            ? "Sản phẩm và các loại đang kinh doanh được đưa lại vào cửa hàng; các loại đã ẩn riêng vẫn được giữ ẩn. Kiểm tra tồn kho và hạn dùng trước khi bán."
                            : "Sản phẩm cùng các loại sẽ ngừng hiển thị và ngừng bán. Lịch sử đơn hàng được giữ nguyên; bạn có thể khôi phục trong bộ lọc Đã xóa.",
                          run: () =>
                            adminRequest(
                              deleted
                                ? `/api/v1/admin/products/${p.maSP}/restore`
                                : `/api/v1/products/${p.maSP}`,
                              { method: deleted ? "POST" : "DELETE" },
                            ),
                        })
                      }
                    >
                      {deleted ? "Khôi phục" : "Xóa"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!list.loading && !list.data?.items.length && (
          <Empty>Không có sản phẩm trong bộ lọc này.</Empty>
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
