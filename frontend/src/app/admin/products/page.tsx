"use client";

import { useState } from "react";
import { Plus, RotateCw } from "lucide-react";
import { Category, ProductCreateRequest } from "@/types";
import {
  adminError,
  adminRequest,
  AdminPage,
  AdminProduct,
  datetime,
  money,
} from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import {
  AdminTitle,
  Confirm,
  Empty,
  Notice,
  Pagination,
} from "@/components/admin/AdminUI";

const fresh = (category: number): ProductCreateRequest => ({
  tenSP: "",
  hinhAnh: "",
  moTa: "",
  gia: 0,
  donViTinh: "",
  soLuongTon: 0,
  hanSuDung: "",
  nhietDoBaoQuan: "",
  maDanhMuc: category,
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
  const [draft, setDraft] = useState<ProductCreateRequest | null>(null);
  const [editing, setEditing] = useState<number | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const categoryName = (id: number) =>
    categories.data?.find((c) => c.maDanhMuc === id)?.tenDanhMuc ?? `#${id}`;
  function field<K extends keyof ProductCreateRequest>(
    key: K,
    value: ProductCreateRequest[K],
  ) {
    setDraft((current) => (current ? { ...current, [key]: value } : current));
  }
  return (
    <>
      <AdminTitle
        title="Sản phẩm"
        description="Quản lý giá niêm yết, tồn kho, hạn dùng và trạng thái kinh doanh."
      >
        <button
          className="ad-btn"
          disabled={!categories.data?.length}
          onClick={() => {
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
                if (!editing && new Date(draft.hanSuDung) <= new Date())
                  throw new Error("Hạn dùng sản phẩm mới phải ở tương lai.");
                await adminRequest(
                  `/api/v1/products${editing ? `/${editing}` : ""}`,
                  {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify({
                      ...draft,
                      tenSP: draft.tenSP.trim(),
                      donViTinh: draft.donViTinh.trim(),
                      hanSuDung: `${draft.hanSuDung}:00`,
                    }),
                  },
                );
                setDraft(null);
                setEditing(null);
                setMessage(
                  editing
                    ? "Đã lưu thay đổi; trigger và đồng bộ vector được thực thi ở backend."
                    : "Đã tạo sản phẩm.",
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
                  type="number"
                  className="ad-input"
                  min="1"
                  step="1"
                  value={draft.gia}
                  onChange={(e) => field("gia", Number(e.target.value))}
                  required
                />
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
                  type="number"
                  className="ad-input"
                  min="0"
                  step="1"
                  value={draft.soLuongTon}
                  onChange={(e) => field("soLuongTon", Number(e.target.value))}
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
                  maxLength={50}
                />
              </label>
              <label className="ad-field ad-span-full">
                Ảnh sản phẩm
                <input
                  className="ad-input"
                  value={draft.hinhAnh ?? ""}
                  onChange={(e) => field("hinhAnh", e.target.value)}
                  placeholder="Dán URL ảnh https://… hoặc tải ảnh từ máy bên dưới"
                  maxLength={1000}
                />
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  disabled={pending}
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    e.target.value = "";
                    if (!file) return;
                    if (file.size > 5 * 1024 * 1024) {
                      setError("Ảnh tối đa 5 MB.");
                      return;
                    }
                    setPending(true);
                    setError("");
                    try {
                      const body = new FormData();
                      body.append("file", file);
                      const uploaded = await adminRequest<{ url: string }>("/api/v1/admin/product-images", { method: "POST", body });
                      field("hinhAnh", uploaded.url);
                    } catch (failure) {
                      setError(adminError(failure));
                    } finally {
                      setPending(false);
                    }
                  }}
                />
                <small>Chọn ảnh JPG, PNG hoặc WebP, tối đa 5 MB. Bấm Lưu sản phẩm để áp dụng.</small>
                {draft.hinhAnh && (
                  <div className="flex items-center gap-4">
                    <img src={draft.hinhAnh} alt="Xem trước ảnh sản phẩm" className="h-32 w-32 rounded-lg border object-contain bg-white" />
                    <button type="button" className="ad-btn secondary" disabled={pending} onClick={() => field("hinhAnh", "")}>Xóa ảnh</button>
                  </div>
                )}
              </label>
              <label className="ad-field ad-span-full">
                Mô tả và công dụng
                <textarea
                  className="ad-textarea"
                  value={draft.moTa ?? ""}
                  onChange={(e) => field("moTa", e.target.value)}
                />
                <small>
                  Dùng mô tả đúng thực tế để hỗ trợ tìm kiếm ngữ nghĩa.
                </small>
              </label>
            </div>
            <div className="ad-actions">
              <button className="ad-btn" disabled={pending}>
                {pending ? "Đang lưu…" : "Lưu sản phẩm"}
              </button>
              <button
                type="button"
                className="ad-btn secondary"
                disabled={pending}
                onClick={() => setDraft(null)}
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
          <option value="true">Đã ẩn</option>
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
                  {p.hinhAnh && <img src={p.hinhAnh} alt={p.tenSP} className="mb-2 h-14 w-14 rounded-md border bg-white object-contain" loading="lazy" />}
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
                        onClick={() => {
                          setEditing(p.maSP);
                          setDraft({
                            ...p,
                            moTa: p.moTa ?? "",
                            hanSuDung: p.hanSuDung.slice(0, 16),
                          });
                          setError("");
                          window.scrollTo({ top: 0, behavior: "smooth" });
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
                            : `Ẩn ${p.tenSP}?`,
                          description: deleted
                            ? "Sản phẩm được đưa lại vào danh sách kinh doanh. Kiểm tra tồn kho và hạn dùng trước khi bán."
                            : "Sản phẩm sẽ bị ẩn khỏi cửa hàng và chỉ mục tìm kiếm; lịch sử mua hàng được giữ nguyên.",
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
                      {deleted ? "Khôi phục" : "Ẩn sản phẩm"}
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
