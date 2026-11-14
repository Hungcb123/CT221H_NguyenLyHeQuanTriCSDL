"use client";

import { useState } from "react";
import { Upload } from "lucide-react";
import { adminError, uploadAdminProductImage } from "@/services/admin-api";

export interface ProductTypeFormItem {
  maSP?: number;
  tenLoai: string;
  gia: string;
  donViTinh: string;
  soLuongTon: string;
  hanSuDung: string;
  nhietDoBaoQuan?: string;
  hinhAnh?: string;
  moTa?: string;
  trangThaiXoa?: boolean;
}

export interface ProductFormParent {
  gia: string;
  donViTinh: string;
  hanSuDung: string;
  nhietDoBaoQuan?: string;
  hinhAnh?: string;
  moTa?: string;
}

export function cleanPositiveIntInput(raw: string): string {
  return raw.replace(/\D+/g, "").replace(/^0+/, "");
}

export function cleanNonNegativeIntInput(raw: string): string {
  if (raw === "") return "";
  const digits = raw.replace(/\D+/g, "");
  if (!digits) return "";
  return digits.replace(/^0+(?=\d)/, "");
}

export function ProductTypeEditor({ types, product, disabled, onChange, onUploading }: {
  types: ProductTypeFormItem[];
  product: ProductFormParent;
  disabled: boolean;
  onChange: (types: ProductTypeFormItem[]) => void;
  onUploading: (busy: boolean) => void;
}) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  function update(index: number, values: Partial<ProductTypeFormItem>) {
    onChange(types.map((type, i) => i === index ? { ...type, ...values } : type));
  }
  return (
    <section className="mt-7 rounded-lg border border-stone-200 p-4" aria-label="Các loại của sản phẩm">
      <h3 className="text-lg font-bold">Các loại của sản phẩm</h3>
      <p className="ad-help">Thêm các loại của sản phẩm này nếu cần. Mỗi loại có tên, giá và tồn kho riêng. Bấm Lưu sản phẩm để lưu tất cả thay đổi.</p>
      {error && <p role="alert" className="text-red-800">{error}</p>}
      {types.length === 0 && <p className="my-3 text-sm text-stone-600">Chưa có loại nào. Bạn vẫn có thể bán sản phẩm như bình thường.</p>}
      {types.map((type, index) => (
        <fieldset key={type.maSP ?? `new-${index}`} className="my-4 rounded-lg border border-stone-200 p-4" disabled={disabled || uploading}>
          <legend className="px-2 font-semibold">Loại {index + 1}{type.trangThaiXoa ? " · Đã ẩn" : ""}</legend>
          <fieldset disabled={!!type.trangThaiXoa}>
            <div className="ad-form-grid">
              <label className="ad-field">
                Tên loại
                <input
                  className="ad-input"
                  required
                  maxLength={80}
                  placeholder="Ví dụ: Khay 300g"
                  value={type.tenLoai}
                  onChange={(e) => update(index, { tenLoai: e.target.value })}
                />
              </label>
              <label className="ad-field">
                Giá của loại (đ)
                <input
                  className="ad-input"
                  type="text"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  placeholder="Ví dụ: 35000"
                  required
                  value={type.gia}
                  onChange={(e) => update(index, { gia: cleanPositiveIntInput(e.target.value) })}
                />
                {type.gia && <small>{Number(type.gia).toLocaleString("vi-VN")} đ</small>}
              </label>
              <label className="ad-field">
                Đơn vị tính
                <input
                  className="ad-input"
                  required
                  maxLength={20}
                  placeholder="gói, khay, bó…"
                  value={type.donViTinh}
                  onChange={(e) => update(index, { donViTinh: e.target.value })}
                />
              </label>
              <label className="ad-field">
                Tồn kho của loại
                <input
                  className="ad-input"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]+"
                  placeholder="Ví dụ: 30"
                  required
                  value={type.soLuongTon}
                  onChange={(e) => update(index, { soLuongTon: cleanNonNegativeIntInput(e.target.value) })}
                />
              </label>
              <label className="ad-field">
                Hạn sử dụng
                <input
                  className="ad-input"
                  type="datetime-local"
                  required
                  value={type.hanSuDung}
                  onChange={(e) => update(index, { hanSuDung: e.target.value })}
                />
              </label>
              <label className="ad-field">
                Nhiệt độ bảo quản
                <input
                  className="ad-input"
                  maxLength={50}
                  placeholder="Ví dụ: Ngăn mát 4 - 8°C"
                  value={type.nhietDoBaoQuan || ""}
                  onChange={(e) => update(index, { nhietDoBaoQuan: e.target.value })}
                />
              </label>
              <div className="ad-field ad-span-full">
                <span>Ảnh của loại</span>
                <input
                  className="ad-input"
                  maxLength={1000}
                  placeholder="Dán URL ảnh hoặc bấm nút tải ảnh bên dưới"
                  value={type.hinhAnh || ""}
                  onChange={(e) => update(index, { hinhAnh: e.target.value })}
                />
                <div className="mt-1 flex flex-wrap items-center gap-3">
                  <label className="ad-btn secondary" style={{ cursor: disabled || uploading ? "not-allowed" : "pointer" }}>
                    <Upload size={15} aria-hidden="true" />
                    {uploading ? "Đang tải ảnh…" : "Chọn ảnh từ máy"}
                    <input
                      type="file"
                      hidden
                      accept="image/jpeg,image/png,image/webp"
                      disabled={disabled || uploading}
                      onChange={async (event) => {
                        const file = event.target.files?.[0];
                        event.target.value = "";
                        if (!file) return;
                        setUploading(true);
                        onUploading(true);
                        setError("");
                        try {
                          const result = await uploadAdminProductImage(file, type.maSP);
                          update(index, { hinhAnh: result.url });
                        } catch (failure) {
                          setError(adminError(failure));
                        } finally {
                          setUploading(false);
                          onUploading(false);
                        }
                      }}
                    />
                  </label>
                  <small>JPG, PNG hoặc WebP (tối đa 5 MB).</small>
                </div>
                {type.hinhAnh && (
                  <img src={type.hinhAnh} alt={`Ảnh loại ${type.tenLoai || index + 1}`} className="mt-2 h-20 w-20 rounded border object-contain bg-white" />
                )}
              </div>
              <label className="ad-field ad-span-full">
                Mô tả loại
                <textarea
                  className="ad-textarea"
                  value={type.moTa || ""}
                  onChange={(e) => update(index, { moTa: e.target.value })}
                />
              </label>
            </div>
          </fieldset>
          <div className="ad-actions">
            {type.maSP ? (
              <button type="button" className="ad-btn secondary" onClick={() => update(index, { trangThaiXoa: !type.trangThaiXoa })}>
                {type.trangThaiXoa ? "Khôi phục loại" : "Ẩn loại"}
              </button>
            ) : (
              <button type="button" className="ad-btn secondary" onClick={() => onChange(types.filter((_, i) => i !== index))}>
                Bỏ loại này
              </button>
            )}
          </div>
        </fieldset>
      ))}
      <button
        type="button"
        className="ad-btn secondary"
        disabled={disabled || uploading || types.length >= 100}
        onClick={() =>
          onChange([
            ...types,
            {
              tenLoai: "",
              gia: product.gia || "",
              donViTinh: product.donViTinh,
              soLuongTon: "",
              hanSuDung: product.hanSuDung && new Date(product.hanSuDung) > new Date() ? product.hanSuDung : "",
              nhietDoBaoQuan: product.nhietDoBaoQuan || "",
              hinhAnh: product.hinhAnh || "",
              moTa: product.moTa || "",
            },
          ])
        }
      >
        + Thêm loại
      </button>
    </section>
  );
}
