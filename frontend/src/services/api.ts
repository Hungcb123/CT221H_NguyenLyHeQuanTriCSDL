import {
  ApiResponse,
  Category,
  ProductPageResponse,
  ProductResponse,
  CartResponse,
  AddToCartRequest,
  ChatMessageRequest,
  ChatMessageResponse,
  HybridSearchResponse,
  ProductCreateRequest,
  WalletResponse,
  CheckoutResponse,
} from "@/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "";

// Keep the browser model stable while mapping the backend's Vietnamese DTO fields.
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, { cache: "no-store", ...init });
  } catch {
    throw new Error("Không kết nối được máy chủ. Vui lòng kiểm tra kết nối và thử lại.");
  }
  const json = await res.json().catch(() => null);
  if (!res.ok || !json?.success || (typeof json.statusCode === "number" && json.statusCode < 0)) {
    throw new Error(json?.message || "Yêu cầu chưa thành công. Vui lòng thử lại.");
  }
  return json.data as T;
}

export const apiService = {
  async createProduct(req: ProductCreateRequest): Promise<ProductResponse> {
    return request("/api/v1/products", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(req),
    });
  },

  async getCategories(): Promise<Category[]> {
    const data = await request<Category[]>("/api/v1/categories");
    if (!Array.isArray(data)) throw new Error("Dữ liệu danh mục không hợp lệ. Vui lòng thử lại.");
    return data;
  },

  // Lấy danh sách sản phẩm phân trang
  async getProducts(params?: {
    categoryId?: number;
    keyword?: string;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: string;
    page?: number;
    size?: number;
  }): Promise<ProductPageResponse> {
    const page = params?.page || 1;
    const size = params?.size || 12;

    const searchParams = new URLSearchParams();
    if (params?.categoryId) searchParams.set("categoryId", params.categoryId.toString());
    if (params?.keyword) searchParams.set("keyword", params.keyword);
    if (params?.minPrice !== undefined) searchParams.set("minPrice", params.minPrice.toString());
    if (params?.maxPrice !== undefined) searchParams.set("maxPrice", params.maxPrice.toString());
    if (params?.sortBy) searchParams.set("sortBy", params.sortBy);
    searchParams.set("page", page.toString());
    searchParams.set("size", size.toString());
    const res = await fetch(`${API_BASE_URL}/api/v1/products?${searchParams}`, { cache: "no-store" });
    const json = await res.json().catch(() => null);
    if (!res.ok || !json?.success || !Array.isArray(json.data?.items)) {
      throw new Error(json?.message || "Không tải được sản phẩm. Vui lòng thử lại.");
    }
    return {
      products: json.data.items,
      page: json.data.currentPage,
      size: json.data.pageSize,
      totalElements: json.data.totalRecords,
      totalPages: json.data.totalPages,
    };
  },

  // Tìm kiếm ngữ nghĩa bằng ONNX + Qdrant
  async searchSemantic(query: string, limit: number = 6): Promise<HybridSearchResponse> {
    const res = await fetch(
      `${API_BASE_URL}/api/v1/search/semantic?query=${encodeURIComponent(query)}&limit=${limit}&minScore=0.3`,
      { cache: "no-store" }
    );
    const json: ApiResponse<HybridSearchResponse> | null = await res.json().catch(() => null);
    if (!res.ok || !json?.success || !Array.isArray(json.data?.products) || !Array.isArray(json.data?.relatedProducts)
      || !["HYBRID", "KEYWORD_FALLBACK"].includes(json.data.mode)) {
      throw new Error(json?.message || "Không thể tìm kiếm thông minh. Vui lòng thử lại.");
    }
    return json.data;
  },

  async getCart(customerId: number = 1): Promise<CartResponse> {
    const data = await request<Omit<CartResponse, "danhSachMon"> & { items: (CartResponse["danhSachMon"][number] & { soLuongTon?: number })[] }>(`/api/v1/cart/${customerId}`);
    if (!Array.isArray(data?.items)) throw new Error("Dữ liệu giỏ hàng không hợp lệ. Vui lòng thử lại.");
    return { ...data, danhSachMon: data.items.map((item) => ({ ...item, soLuongTonHienTai: item.soLuongTon })) };
  },

  async addToCart(req: AddToCartRequest): Promise<void> {
    if (!Number.isInteger(req.quantity) || req.quantity < 1) throw new Error("Số lượng mua phải là số nguyên từ 1 trở lên.");
    await request("/api/v1/cart/items", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ maKH: req.customerId, maSP: req.productId, soLuong: req.quantity }),
    });
  },

  async removeFromCart(customerId: number, productId: number): Promise<void> {
    await request("/api/v1/cart/items", {
      method: "DELETE", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ maKH: customerId, maSP: productId }),
    });
  },

  async getWallet(customerId: number = 1): Promise<WalletResponse> {
    const data = await request<WalletResponse>(`/api/v1/wallets/${customerId}`);
    if (!data || !Number.isFinite(data.soDuVi)) throw new Error("Không đọc được số dư ví. Vui lòng thử lại.");
    return data;
  },

  async checkout(address: string, note: string, customerId: number = 1): Promise<CheckoutResponse> {
    const data = await request<CheckoutResponse>("/api/v1/orders/checkout", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ maKH: customerId, diaChiGiaoHang: address.trim(), ghiChu: note.trim() }),
    });
    if (data?.statusCode !== 1 || !data.maDH || !Number.isFinite(data.tongTien) || !Number.isFinite(data.soDuConLai)) {
      throw new Error(data?.message || "Chưa xác nhận được kết quả thanh toán. Hãy kiểm tra đơn hàng trước khi thử lại.");
    }
    return data;
  },

  async chatWithBot(req: ChatMessageRequest): Promise<ChatMessageResponse> {
    return request("/api/v1/ai/chatbot/chat", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(req),
    });
  },
};
