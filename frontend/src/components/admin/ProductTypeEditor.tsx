"use client";

import { useState } from "react";
import type { ProductCreateRequest, ProductTypeDraft } from "@/types";
import { adminRequest, adminError } from "@/services/admin-api";

export function ProductTypeEditor({ types, product, disabled, onChange, onUploading }: {
  types: ProductTypeDraft[];
  product: ProductCreateRequest;
  disabled: boolean;
  onChange: (types: ProductTypeDraft[]) => void;
  onUploading: (busy: boolean) => void;
}) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  function update(index: number, values: Partial<ProductTypeDraft>) {
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
              <label className="ad-field">Tên loại<input className="ad-input" required maxLength={80} placeholder="Ví dụ: Loại đặc biệt" value={type.tenLoai} onChange={(e) => update(index, { tenLoai: e.target.value })} /></label>
              <label className="ad-field">Giá của loại (đ)<input className="ad-input" type="number" min="1" step="1" required value={type.gia} onChange={(e) => update(index, { gia: Number(e.target.value) })} /></label>
              <label className="ad-field">Đơn vị tính<input className="ad-input" required maxLength={20} value={type.donViTinh} onChange={(e) => update(index, { donViTinh: e.target.value })} /></label>
              <label className="ad-field">Tồn kho của loại<input className="ad-input" type="number" min="0" step="1" required value={type.soLuongTon} onChange={(e) => update(index, { soLuongTon: Number(e.target.value) })} /></label>
              <label className="ad-field">Hạn sử dụng<input className="ad-input" type="datetime-local" required value={type.hanSuDung} onChange={(e) => update(index, { hanSuDung: e.target.value })} /></label>
              <label className="ad-field">Nhiệt độ bảo quản<input className="ad-input" maxLength={50} value={type.nhietDoBaoQuan || ""} onChange={(e) => update(index, { nhietDoBaoQuan: e.target.value })} /></label>
              <label className="ad-field ad-span-full">Ảnh của loại<input className="ad-input" maxLength={1000} placeholder="URL ảnh hoặc tải ảnh bên dưới" value={type.hinhAnh || ""} onChange={(e) => update(index, { hinhAnh: e.target.value })} />
                <input type="file" accept="image/jpeg,image/png,image/webp" onChange={async (event) => {
                  const file = event.target.files?.[0]; event.target.value = "";
                  if (!file) return;
                  if (file.size > 5 * 1024 * 1024) { setError("Ảnh tối đa 5 MB."); return; }
                  setUploading(true); onUploading(true); setError("");
                  try {
                    const body = new FormData(); body.append("file", file);
                    const result = await adminRequest<{ url: string }>("/api/v1/admin/product-images", { method: "POST", body });
                    update(index, { hinhAnh: result.url });
                  } catch (failure) { setError(adminError(failure)); }
                  finally { setUploading(false); onUploading(false); }
                }} />
                {type.hinhAnh && <img src={type.hinhAnh} alt={`Ảnh loại ${type.tenLoai || index + 1}`} className="mt-2 h-20 w-20 rounded border object-contain" />}
              </label>
              <label className="ad-field ad-span-full">Mô tả loại<textarea className="ad-textarea" value={type.moTa || ""} onChange={(e) => update(index, { moTa: e.target.value })} /></label>
            </div>
          </fieldset>
          <div className="ad-actions">
            {type.maSP ? <button type="button" className="ad-btn secondary" onClick={() => update(index, { trangThaiXoa: !type.trangThaiXoa })}>{type.trangThaiXoa ? "Khôi phục loại" : "Ẩn loại"}</button>
              : <button type="button" className="ad-btn secondary" onClick={() => onChange(types.filter((_, i) => i !== index))}>Bỏ loại này</button>}
          </div>
        </fieldset>
      ))}
      <button type="button" className="ad-btn secondary" disabled={disabled || uploading || types.length >= 100} onClick={() => onChange([...types, {
        tenLoai: "", gia: product.gia, donViTinh: product.donViTinh, soLuongTon: 0,
        hanSuDung: product.hanSuDung && new Date(product.hanSuDung) > new Date() ? product.hanSuDung : "",
        nhietDoBaoQuan: product.nhietDoBaoQuan || "", hinhAnh: product.hinhAnh || "", moTa: product.moTa || "",
      }])}>+ Thêm loại</button>
    </section>
  );
}
