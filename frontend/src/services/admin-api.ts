import type {
  Category,
  HybridSearchResponse,
  ProductCreateRequest,
} from "@/types";

export interface AdminSession {
  authenticated: boolean;
  username: string;
  csrfToken: string;
  csrfHeader: string;
}
export interface AdminProduct extends ProductCreateRequest {
  maSP: number;
  trangThaiXoa: boolean;
  tonKhoCacLoai?: { maSP: number; tenLoai: string; soLuongTon: number; trangThaiXoa: boolean }[];
}
export interface AdminPage<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface AdminOrder {
  MaDH: number;
  MaKH: number;
  HoTen: string;
  NgayTao: string;
  TongTien: number;
  TrangThai: string;
  DiaChiGiaoHang: string;
  PhuongThucThanhToan?: string;
  NguonDon: "SALE" | "BENCHMARK" | "DEMO_FORECAST";
}
export interface AdminAlert {
  MaLog: number;
  LoaiSuKien: string;
  NoiDung: string;
  MaSP: number | null;
  NguoiThucHien: string;
  NgayGhi: string;
}
export interface AdminOverview {
  products: number;
  lowStock: number;
  expiring: number;
  orders: number;
  revenue: number;
  benchmarkOrders: number;
  demoOrders: number;
  recentAlerts: AdminAlert[];
}
export interface ForecastResult {
  maDuBao: number | null;
  maSP: number;
  tenSP: string;
  donViTinh: string;
  ngayDuBao: string;
  soLuongDuBaoTieuThu: number;
  soLuongTonHienTai: number;
  soLuongDeXuatNhap: number;
  ruiRoHuHong: string;
  doTreInferenceMs: number | null;
  thuatToan?: string;
  ghiChuDuBao?: string;
  featuresUsed: Record<string, number | string>;
}
export interface ForecastEvaluation {
  catalogSize: number;
  trainSamples: number;
  testSamples: number;
  splitMethod: string;
  mae: number;
  rmse: number;
  r2: number;
  wapePercent: number;
  volumeAccuracyPercent: number;
  restockAccuracyPercent: number;
  restockPrecisionPercent: number;
  restockRecallPercent: number;
  restockF1Percent: number;
  spoilageRiskAccuracyPercent: number;
  spoilageRiskMacroF1Percent: number;
  spoilageRiskWeightedF1Percent: number;
}
export interface AIStatus {
  embedding: { ready: boolean; name: string; dimension: number };
  qdrant: {
    status: string;
    pointsCount: number;
    collectionName: string;
    distanceMetric: string;
  };
  forecast: {
    ready: boolean;
    name: string;
    features: string[];
    historySource: string;
    confidenceCalibrated: boolean;
    evaluation?: ForecastEvaluation;
  };
}
export interface BenchmarkResult {
  totalOrdersInDatabase: number;
  benchmarkOrdersInDatabase: number;
  withoutIndex: {
    executionTimeMs: number;
    recordsReturned: number;
    accessMethod: string;
  };
  withCoveringIndex: {
    executionTimeMs: number;
    recordsReturned: number;
    accessMethod: string;
  };
  speedupFactor: string;
  timeReductionPercentage: string;
  academicEvaluation: string;
}
export interface Promotion {
  dangHieuLuc: boolean;
  soLuongSanPham: number;
  trangThaiHienThi: "ACTIVE" | "PAUSED" | "UPCOMING" | "ENDED";
  maKM: number;
  tenKM: string;
  moTa: string;
  loaiKM: string;
  ngayBatDau: string;
  ngayKetThuc: string;
  trangThai: boolean;
  items?: PromotionItem[];
}
export interface PromotionItem {
  maSP: number;
  tenSP: string;
  giaKhuyenMai: number;
  phanTramGiam: number;
  soLuongKhuyenMai?: number;
  soLuongDaBan: number;
}
export interface CustomerRow {
  MaKH: number;
  HoTen: string;
  Email: string;
  SoDienThoai: string;
  DiaChi: string;
  SoDuVi: number;
  NhomKhachHang: "CHUA_MUA" | "CAN_TUONG_TAC_LAI" | "THAN_THIET" | "DANG_HOAT_DONG";
  LanMuaCuoi: string | null;
  SoDon90Ngay: number;
  ChiTieu90Ngay: number;
}

let currentSession: AdminSession | null = null;

export async function getAdminSession(): Promise<AdminSession> {
  let response: Response;
  try {
    response = await fetch("/api/v1/admin/auth/session", {
      cache: "no-store",
      credentials: "same-origin",
    });
  } catch {
    throw new Error("Không kết nối được máy chủ quản trị (cổng 8085). Vui lòng kiểm tra backend.");
  }
  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload?.success)
    throw new Error(payload?.message || "Không kiểm tra được phiên đăng nhập.");
  currentSession = payload.data;
  return payload.data;
}

async function rawAdminFetch(path: string, init: RequestInit, session: AdminSession): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set(session.csrfHeader, session.csrfToken);
  if (init.body && !(init.body instanceof URLSearchParams) && !(init.body instanceof FormData))
    headers.set("Content-Type", "application/json");
  return fetch(path, {
    ...init,
    headers,
    cache: "no-store",
    credentials: "same-origin",
  });
}

export async function adminRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  let session = currentSession ?? (await getAdminSession());
  let response: Response;
  try {
    response = await rawAdminFetch(path, init, session);
  } catch {
    // Refresh session once in case stale CSRF / early socket close caused a network reset
    session = await getAdminSession();
    if (!session.authenticated && path !== "/api/v1/admin/auth/login") {
      currentSession = null;
      window.dispatchEvent(new Event("admin:unauthorized"));
      throw new Error("Phiên quản trị đã hết hạn. Vui lòng đăng nhập lại (dữ liệu đang nhập vẫn được giữ).");
    }
    try {
      response = await rawAdminFetch(path, init, session);
    } catch {
      throw new Error("Không kết nối được máy chủ để gửi dữ liệu. Vui lòng thử lại.");
    }
  }

  // Transparently refresh stale CSRF token once on 403
  if (response.status === 403 && path !== "/api/v1/admin/auth/login") {
    const refreshed = await getAdminSession().catch(() => null);
    if (refreshed?.authenticated) {
      response = await rawAdminFetch(path, init, refreshed);
    } else {
      currentSession = null;
      window.dispatchEvent(new Event("admin:unauthorized"));
      throw new Error("Phiên quản trị đã hết hạn. Vui lòng đăng nhập lại (dữ liệu đang nhập vẫn được giữ).");
    }
  }

  const payload = await response.json().catch(() => null);
  if (response.status === 401) {
    currentSession = null;
    window.dispatchEvent(new Event("admin:unauthorized"));
  }
  if (!response.ok || !payload?.success)
    throw new Error(
      payload?.message || `Yêu cầu không thành công (${response.status}).`,
    );
  return payload.data;
}

function readFileAsDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Không đọc được tệp ảnh từ máy."));
    reader.readAsDataURL(blob);
  });
}

async function normalizeImageForUpload(file: File): Promise<string> {
  const supportedMime = ["image/jpeg", "image/png", "image/webp"].includes(file.type);
  if (supportedMime && file.size <= 4.5 * 1024 * 1024) {
    return readFileAsDataUrl(file);
  }
  // Auto-compress oversized images or convert unsupported image formats (BMP/AVIF/GIF) via Canvas
  const bitmapUrl = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error("Định dạng ảnh không hợp lệ. Vui lòng chọn JPG, PNG hoặc WebP."));
      el.src = bitmapUrl;
    });
    const maxSide = 1600;
    const scale = Math.min(1, maxSide / Math.max(img.naturalWidth || 1, img.naturalHeight || 1));
    const width = Math.max(1, Math.round(img.naturalWidth * scale));
    const height = Math.max(1, Math.round(img.naturalHeight * scale));
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext("2d");
    if (!ctx) return readFileAsDataUrl(file);
    ctx.drawImage(img, 0, 0, width, height);
    const outType = file.type === "image/png" ? "image/png" : "image/jpeg";
    return canvas.toDataURL(outType, 0.88);
  } finally {
    URL.revokeObjectURL(bitmapUrl);
  }
}

export async function uploadAdminProductImage(
  file: File,
  productId?: number,
): Promise<{ url: string; savedToProduct?: boolean }> {
  if (file.size > 15 * 1024 * 1024) {
    throw new Error("Ảnh tối đa 15 MB.");
  }
  const data = await normalizeImageForUpload(file);
  return adminRequest<{ url: string; savedToProduct?: boolean }>(
    "/api/v1/admin/product-images",
    {
      method: "POST",
      body: JSON.stringify({ data, productId: productId ?? null }),
    },
  );
}

export async function adminLogin(username: string, password: string) {
  await getAdminSession();
  await adminRequest("/api/v1/admin/auth/login", {
    method: "POST",
    body: new URLSearchParams({ username, password }),
  });
  return getAdminSession();
}

export async function adminLogout() {
  await adminRequest("/api/v1/admin/auth/logout", { method: "POST" });
  currentSession = null;
}

export const adminApi = {
  categories: () => adminRequest<Category[]>("/api/v1/categories"),
  products: (query = "", deleted = false, page = 1) =>
    adminRequest<AdminPage<AdminProduct>>(
      `/api/v1/admin/products?${new URLSearchParams({ query, deleted: String(deleted), page: String(page), size: "20" })}`,
    ),
  overview: () => adminRequest<AdminOverview>("/api/v1/admin/overview"),
  ai: () => adminRequest<AIStatus>("/api/v1/admin/ai/status"),
  search: (query: string) =>
    adminRequest<HybridSearchResponse>(
      `/api/v1/search/semantic?query=${encodeURIComponent(query)}&limit=12&minScore=0.3`,
    ),
  forecasts: (id: number) =>
    adminRequest<ForecastResult[]>(`/api/v1/ai/forecast/history/${id}`),
};

export function adminError(error: unknown) {
  return error instanceof Error ? error.message : "Thao tác không thành công.";
}
export function money(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
}
export function datetime(value: string | number | null | undefined) {
  return value ? new Date(value).toLocaleString("vi-VN") : "—";
}
