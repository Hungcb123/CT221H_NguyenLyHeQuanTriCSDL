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
  const response = await fetch("/api/v1/admin/auth/session", {
    cache: "no-store",
    credentials: "same-origin",
  });
  const payload = await response.json();
  if (!response.ok || !payload.success)
    throw new Error(payload.message || "Không kiểm tra được phiên đăng nhập.");
  currentSession = payload.data;
  return payload.data;
}

export async function adminRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const session = currentSession ?? (await getAdminSession());
  const headers = new Headers(init.headers);
  headers.set(session.csrfHeader, session.csrfToken);
  if (init.body && !(init.body instanceof URLSearchParams) && !(init.body instanceof FormData))
    headers.set("Content-Type", "application/json");
  const response = await fetch(path, {
    ...init,
    headers,
    cache: "no-store",
    credentials: "same-origin",
  });
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
