import type { ProductResponse } from "@/types";

// SQL Server decides availability using the same clock as cart validation.
// The browser must not reinterpret timezone-free SQL dates as local dates.
export function productAvailability(product: Pick<ProductResponse, "trangThaiBan">) {
  const labels: Record<string, string> = {
    EXPIRED: "Đã hết hạn sử dụng",
    OUT_OF_STOCK: "Tạm hết hàng",
    STOPPED: "Đã ngừng kinh doanh",
    UNKNOWN_EXPIRY: "Chưa có hạn sử dụng hợp lệ",
    NO_AVAILABLE_TYPES: "Chưa có loại nào còn hàng và còn hạn",
  };
  const available = product.trangThaiBan === "AVAILABLE";
  return { available, label: available ? "Còn hàng" : labels[product.trangThaiBan ?? ""] ?? "Cần cập nhật tình trạng sản phẩm" };
}
