"use client";
import { useState } from "react";
import { Category } from "@/types";
import { adminError, adminRequest } from "@/services/admin-api";
import { useAdminResource } from "@/components/admin/useAdminResource";
import { AdminTitle, Confirm, Empty, Notice } from "@/components/admin/AdminUI";

export default function CategoriesPage() {
  const list = useAdminResource<Category[]>("/api/v1/categories");
  const [id, setId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [remove, setRemove] = useState<Category | null>(null);
  return (
    <>
      <AdminTitle
        title="Danh mục"
        description="Tổ chức nhóm hàng. Danh mục có sản phẩm sẽ được giữ để bảo toàn dữ liệu."
      />
      <Notice error={error || list.error} message={message} />
      {remove && (
        <Confirm
          title={`Xóa ${remove.tenDanhMuc}?`}
          description="Chỉ có thể xóa danh mục không chứa bất kỳ sản phẩm nào, kể cả sản phẩm đang ẩn."
          pending={pending}
          onCancel={() => setRemove(null)}
          onConfirm={async () => {
            setPending(true);
            setError("");
            try {
              await adminRequest(
                `/api/v1/admin/categories/${remove.maDanhMuc}`,
                { method: "DELETE" },
              );
              setRemove(null);
              list.refresh();
              setMessage("Đã xóa danh mục trống.");
            } catch (e) {
              setError(adminError(e));
            } finally {
              setPending(false);
            }
          }}
        />
      )}
      <div className="ad-split">
        <section>
          <div className="ad-table-wrap">
            <table className="ad-table">
              <thead>
                <tr>
                  <th>Danh mục</th>
                  <th>Mô tả</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {list.data?.map((c) => (
                  <tr key={c.maDanhMuc}>
                    <td>
                      <strong>{c.tenDanhMuc}</strong>
                      <small>#{c.maDanhMuc}</small>
                    </td>
                    <td>{c.moTa || "—"}</td>
                    <td>
                      <div className="ad-row-actions">
                        <button
                          onClick={() => {
                            setId(c.maDanhMuc);
                            setName(c.tenDanhMuc);
                            setDescription(c.moTa ?? "");
                          }}
                        >
                          Sửa
                        </button>
                        <button onClick={() => setRemove(c)}>Xóa</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!list.data?.length && (
              <Empty>{list.loading ? "Đang tải…" : "Chưa có danh mục."}</Empty>
            )}
          </div>
        </section>
        <section className="ad-panel">
          <h2>{id ? `Sửa danh mục #${id}` : "Thêm danh mục"}</h2>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setPending(true);
              setError("");
              setMessage("");
              try {
                await adminRequest(
                  `/api/v1/admin/categories${id ? `/${id}` : ""}`,
                  {
                    method: id ? "PUT" : "POST",
                    body: JSON.stringify({
                      tenDanhMuc: name.trim(),
                      moTa: description.trim(),
                    }),
                  },
                );
                setId(null);
                setName("");
                setDescription("");
                list.refresh();
                setMessage("Đã lưu danh mục.");
              } catch (failure) {
                setError(adminError(failure));
              } finally {
                setPending(false);
              }
            }}
          >
            <div className="ad-form-grid">
              <label className="ad-field ad-span-full">
                Tên danh mục
                <input
                  className="ad-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  maxLength={100}
                />
              </label>
              <label className="ad-field ad-span-full">
                Mô tả
                <textarea
                  className="ad-textarea"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  maxLength={255}
                />
              </label>
            </div>
            <div className="ad-actions">
              <button className="ad-btn" disabled={pending}>
                {pending ? "Đang lưu…" : "Lưu danh mục"}
              </button>
              {id && (
                <button
                  type="button"
                  className="ad-btn secondary"
                  onClick={() => {
                    setId(null);
                    setName("");
                    setDescription("");
                  }}
                >
                  Hủy
                </button>
              )}
            </div>
          </form>
        </section>
      </div>
    </>
  );
}
