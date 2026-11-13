import { datetime, money } from "@/services/admin-api";

export const alertLabels: Record<string, string> = {
  CANH_BAO_HET_HANG: "Hết hàng",
  CANH_BAO_GIA_BAT_THUONG: "Giá bất thường",
  CANH_BAO_CAN_DATE: "Sản phẩm cận hạn",
  CANH_BAO_RUI_RO_THUC_PHAM: "Rủi ro hư hỏng",
  CAP_NHAT_SAN_PHAM: "Cập nhật sản phẩm",
  XOA_MEM_SAN_PHAM: "Xóa mềm sản phẩm",
  ADMIN_ORDER_STATUS: "Đổi trạng thái đơn hàng",
};

const fieldLabels: Record<string, string> = {
  TenSP: "Tên sản phẩm",
  MoTa: "Mô tả",
  HinhAnh: "Ảnh",
  Gia: "Giá",
  DonViTinh: "Đơn vị tính",
  SoLuongTon: "Tồn kho",
  HanSuDung: "Hạn sử dụng",
  NhietDoBaoQuan: "Nhiệt độ bảo quản",
  MaDanhMuc: "Mã danh mục",
  TrangThaiXoa: "Trạng thái",
};

type Snapshot = Record<string, string | number | boolean | null>;
const beforePrefix = "Trước: ";
const separator = " | Sau: ";

function snapshots(content: string): [Snapshot, Snapshot] | undefined {
  if (!content.startsWith(beforePrefix)) return;
  for (let split = content.indexOf(separator); split !== -1; split = content.indexOf(separator, split + separator.length)) {
    try {
      const before = JSON.parse(content.slice(beforePrefix.length, split));
      const after = JSON.parse(content.slice(split + separator.length));
      if (before && after && typeof before === "object" && typeof after === "object" &&
          !Array.isArray(before) && !Array.isArray(after)) return [before, after];
    } catch {
      // The separator can also occur inside a product description.
    }
  }
}

function formatValue(field: string, value: Snapshot[string] | undefined): string {
  if (value === null || value === undefined || value === "") return "—";
  if (field === "Gia" && typeof value === "number") return money(value);
  if (field === "HanSuDung" && typeof value === "string") return datetime(value);
  if (field === "TrangThaiXoa" && typeof value === "boolean") return value ? "Đã ẩn" : "Đang bán";
  return String(value);
}

export function AuditEntry({ event, content }: { event: string; content: string }) {
  if (event !== "CAP_NHAT_SAN_PHAM") return <p className="wrap-anywhere">{content}</p>;

  const parsed = snapshots(content);
  const changes = parsed && Object.keys(fieldLabels).filter(key => parsed[0][key] !== parsed[1][key]);
  return (
    <div className="min-w-0 space-y-2">
      {parsed && changes?.length ? (
        <>
          <p className="font-semibold">{parsed[1].TenSP || parsed[0].TenSP || "Sản phẩm"}</p>
          <ul className="space-y-1">
            {changes.map(key => (
              <li className="wrap-anywhere" key={key}>
                <strong>{fieldLabels[key]}</strong>{" "}
                {key === "MoTa" || key === "HinhAnh" ? "đã thay đổi" :
                  `${formatValue(key, parsed[0][key])} → ${formatValue(key, parsed[1][key])}`}
              </li>
            ))}
          </ul>
        </>
      ) : <p>Có thay đổi thông tin sản phẩm.</p>}
      <details>
        <summary className="cursor-pointer font-semibold text-emerald-800">Xem dữ liệu gốc</summary>
        <pre className="ad-json">{parsed ? JSON.stringify({ "Trước": parsed[0], "Sau": parsed[1] }, null, 2) : content}</pre>
      </details>
    </div>
  );
}
