export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  timestamp?: string;
}

export interface Category {
  maDanhMuc: number;
  tenDanhMuc: string;
  moTa?: string;
  icon?: string;
}

export interface ProductResponse {
  maSP: number;
  tenSP: string;
  moTa?: string;
  gia: number;
  giaGoc: number;
  giaHienTai: number;
  dangKhuyenMai: boolean;
  phanTramGiam?: number;
  tenChuongTrinhKM?: string;
  maKM?: number;
  donViTinh: string;
  soLuongTon: number;
  hanSuDung: string;
  nhietDoBaoQuan?: string;
  maDanhMuc: number;
  tenDanhMuc?: string;
  trangThaiXoa?: boolean;
  hinhAnh?: string;
  tag?: string;
}

export interface ProductCreateRequest {
  hinhAnh?: string;
  tenSP: string;
  moTa?: string;
  gia: number;
  donViTinh: string;
  soLuongTon: number;
  hanSuDung: string;
  nhietDoBaoQuan?: string;
  maDanhMuc: number;
}

export interface ProductPageResponse {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  products: ProductResponse[];
}

export interface CartItem {
  maChiTiet?: number;
  maSP: number;
  tenSP: string;
  donGia: number;
  soLuong: number;
  thanhTien: number;
  donViTinh: string;
  hinhAnh?: string;
  soLuongTonHienTai?: number;
  conDuHang?: boolean;
}

export interface CartResponse {
  maGioHang: number;
  maKH: number;
  tongTien: number;
  tongSoLuong: number;
  danhSachMon: CartItem[];
}

export interface AddToCartRequest {
  customerId: number;
  productId: number;
  quantity: number;
}

export interface IngredientItemDto {
  maSP: number;
  tenSP: string;
  soLuongDinhLuong: number;
  donViTinh: string;
  donGia: number;
  thanhTien: number;
  tonKhoHienTai: number;
  conHang: boolean;
  lyDoKhongKhaDung?: string;
  thayTheCho?: string;
}

export interface RecipeDetailDto {
  maCongThuc: number;
  tenMonAn: string;
  moTa: string;
  khauPhan: number;
  huongDanNau: string;
  tongTienUocTinh: number;
  duNguyenLieuTrongKho: boolean;
  nguyenLieuList: IngredientItemDto[];
}

export interface ChatMessageRequest {
  recipeId?: number;
  customerId?: number;
  message: string;
  conversationId?: string;
}

export interface ChatMessageResponse {
  suggestedRecipes?: RecipeDetailDto[];
  reply: string;
  mcpToolInvoked?: string;
  recipeSuggested?: RecipeDetailDto;
  canAddToCart?: boolean;
  cartAdded?: boolean;
  itemsAddedCount?: number;
  totalCartPriceAdded?: number;
  checkoutUrl?: string;
}

export interface SemanticProductResponse extends ProductResponse {
  similarityScore: number;
  keywordScore: number;
  rankingScore: number;
  matchType: "SEMANTIC_VECTOR" | "EXACT_KEYWORD" | "HYBRID";
}

export interface HybridSearchResponse {
  products: SemanticProductResponse[];
  relatedProducts: SemanticProductResponse[];
  mode: "HYBRID" | "KEYWORD_FALLBACK";
  rankingMethod: "EXACT_NAME_THEN_RRF";
  rrfK: number;
  semanticWeight: number;
  keywordWeight: number;
}

export interface PromotionResponse {
  maKM: number;
  tenKM: string;
  moTa?: string;
  ngayBatDau: string;
  ngayKetThuc: string;
  kichHoat: boolean;
  loaiKhuyenMai?: string;
}

export interface BenchmarkRequest {
  iterations: number;
  batchSize: number;
}

export interface BenchmarkResponse {
  testName: string;
  clusteredIndexTimeMs: number;
  nonClusteredIndexTimeMs: number;
  storedProcTimeMs: number;
  dynamicSqlTimeMs: number;
  logicalReadsWithIndex: number;
  logicalReadsWithoutIndex: number;
  summary: string;
}

export interface WalletResponse {
  maKH: number;
  hoTen: string;
  soDuVi: number;
}

export interface CheckoutResponse {
  maDH: number;
  statusCode: number;
  message: string;
  tongTien: number;
  soDuConLai: number;
}
