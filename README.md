# 🥬 HỆ THỐNG BÁCH HÓA THỰC PHẨM TƯƠI SỐNG (MÔ HÌNH BÁCH HÓA XANH)
## ĐỒ ÁN MÔN HỌC: CT221H - NGUYÊN LÝ HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU

---

### 📌 THÀNH PHẦN VÀ CẤU TRÚC THƯ MỤC DỰ ÁN
```
Project_CT221H_NguyenLyDB/
├── SRS.md                                # Tài liệu đặc tả yêu cầu phần mềm duy nhất chuẩn IEEE 830
├── fresh_grocery_demand_forecasting.ipynb # Notebook Colab huấn luyện AI LightGBM, XGBoost, PyTorch MLP
├── docker-compose.yml                    # Cấu hình Docker phối hợp MS SQL Server 2022 & Spring Boot
├── database/                             # Tập hợp các SQL Scripts chuẩn hóa
│   ├── 01_init_schema.sql                # Tạo Database FreshGroceryDB, bảng DanhMuc, SanPham, NhatKyCanhBao
│   ├── 02_stored_procedures.sql          # Stored Procedures CRUD (IN/OUT, TRY CATCH, OFFSET FETCH NEXT)
│   ├── 03_triggers.sql                   # Triggers: Bẫy giá bất thường, Cháy hàng, Cận date, Soft Delete
│   ├── 04_seed_data.sql                  # Nạp 5 danh mục và 12 sản phẩm tươi sống mẫu ban đầu
│   └── init_db.sh                        # Script tự động chạy SQL trong container Docker
└── backend/                              # Ứng dụng Backend Spring Boot 3.3 (Java 21 LTS)
    ├── pom.xml                           # Cấu hình Maven, MSSQL JDBC, SpringDoc Swagger, JPA, Lombok
    ├── Dockerfile                        # Multi-stage Docker build cho Spring Boot
    └── src/main/
        ├── resources/application.yml     # Cấu hình kết nối CSDL và Swagger UI
        └── java/com/ctu/freshgrocery/    # Mã nguồn tầng Controller, Service, Repository, DTO
```

---

### 🚀 CÁCH KHỞI CHẠY HỆ THỐNG (2 PHƯƠNG THỨC)

#### 🔹 CÁCH 1: Chạy toàn bộ bằng Docker (Khuyên dùng)
1. Đảm bảo Docker Desktop đang bật trên máy tính.
2. Tại thư mục gốc của dự án, mở terminal và chạy:
   ```bash
   docker compose up -d
   ```
3. Docker sẽ tự động:
   * Kéo và khởi động Microsoft SQL Server 2022 trên cổng `1433`.
   * Tự động chạy toàn bộ 4 file SQL khởi tạo bảng, Stored Procedures, Triggers và dữ liệu mẫu.
   * Khởi động CSDL Vector **Qdrant** trên cổng `6333` (REST API & Web UI Dashboard).
   * Khởi động backend Spring Boot trên cổng `8080`.
4. Mở trình duyệt truy cập ngay:
   👉 **Swagger UI:** http://localhost:8080/swagger-ui.html  
   👉 **Qdrant Vector Dashboard:** http://localhost:6333/dashboard

---

#### 🔹 CÁCH 2: Chạy trực tiếp trên máy cục bộ (Local Development)
Nếu bạn đã có sẵn **SQL Server / SSMS** cài trên Windows:
1. **Bước 1 (Khởi tạo CSDL):** Mở SQL Server Management Studio (SSMS), mở và chạy tuần tự 4 file trong thư mục `database/`:
   - `01_init_schema.sql`
   - `02_stored_procedures.sql`
   - `03_triggers.sql`
   - `04_seed_data.sql`
2. **Bước 2 (Chạy Backend Spring Boot):**
   Mở terminal tại thư mục `backend/` và chạy lệnh:
   ```bash
   mvn spring-boot:run
   ```
3. Truy cập Swagger UI để test:
   👉 **http://localhost:8080/swagger-ui.html**

---

### 🧪 KỊCH BẢN KIỂM THỬ MODULE 1 TRÊN SWAGGER UI

Bạn có thể mở **http://localhost:8080/swagger-ui.html** và thực hiện trực tiếp các ca kiểm thử sau:

#### 1. Kiểm thử Stored Procedure thêm mới có kiểm soát (`sp_ThemSanPhamMoi`):
* **Ca 1 (Dữ liệu sai):** Gọi `POST /api/v1/products` với `gia = -50000` hoặc `soLuongTon = -5`.
  * **Kết quả:** Nhận mã lỗi âm từ Stored Procedure (`statusCode = -1` hoặc `-2`) kèm thông điệp báo lỗi từ CSDL, không có dòng nào được tạo.
* **Ca 2 (Dữ liệu hợp lệ):** Gọi `POST /api/v1/products` với:
  ```json
  {
    "tenSP": "Cá chẽm tươi phi lê",
    "moTa": "Cá chẽm tươi làm sạch da, đóng gói hút chân không",
    "gia": 120000,
    "donViTinh": "kg",
    "soLuongTon": 20,
    "hanSuDung": "2026-10-15T18:00:00",
    "nhietDoBaoQuan": "Ngăn mát 0 - 2°C",
    "maDanhMuc": 3
  }
  ```
  * **Kết quả:** `statusCode = 1`, trả về mã `@NewMaSP` tự sinh từ Stored Procedure.

#### 2. Kiểm thử Trigger bẫy biến động giá bất thường (`trg_SanPham_CanhBaoBienDong`):
* Gọi `PUT /api/v1/products/1` (Cá lóc tươi giá gốc 75.000 đ), gửi `gia = 15000` (giảm sâu 80%).
* **Kết quả kiểm chứng:** Mở endpoint `GET /api/v1/alerts`, bạn sẽ thấy ngay một dòng nhật ký mới thuộc loại **`CANH_BAO_GIA_BAT_THUONG`** ghi rõ giá cũ, giá mới, % biến động và người thực thi!

#### 3. Kiểm thử Trigger bẫy cháy hàng (`CANH_BAO_HET_HANG`):
* Gọi `PUT /api/v1/products/1`, gửi `soLuongTon = 0`.
* **Kết quả kiểm chứng:** Mở `GET /api/v1/alerts`, Trigger tự động sinh bản ghi **`CANH_BAO_HET_HANG`** cảnh báo Quản trị kho nhập hàng khẩn cấp.

#### 4. Kiểm thử Trigger ngăn chặn xóa vật lý Soft Delete (`trg_SanPham_NganChanXoaVatLy`):
* Gọi `DELETE /api/v1/products/1`.
* **Kết quả kiểm chứng:**
  * Sản phẩm **không bị xóa khỏi bảng** (chống lỗi mất dữ liệu và vi phạm khóa ngoại).
  * Cờ `trangThaiXoa` tự động chuyển thành `true` (Xóa mềm).
  * Mở `GET /api/v1/alerts`, xuất hiện bản ghi **`XOA_MEM_SAN_PHAM`** truy vết lệnh xóa.
  * Khi gọi lại `GET /api/v1/products`, sản phẩm đã bị xóa mềm sẽ tự động không còn xuất hiện trong danh sách hiển thị!

#### 5. Kiểm thử Stored Procedure phân trang & lọc (`sp_LayDanhSachSanPhamPhanTrang`):
* Gọi `GET /api/v1/products?categoryId=3&page=1&size=5&sortBy=GIA_ASC`.
* **Kết quả:** Trả về danh sách thủy hải sản sắp xếp giá tăng dần, kèm tổng số bản ghi và tổng số trang qua các tham số `OUTPUT`.

---

### 💳 KỊCH BẢN KIỂM THỬ MODULE 2 TRÊN SWAGGER UI (TRANSACTION ACID & VÍ ĐIỆN TỬ)

#### 1. Kiểm thử Nạp tiền Ví & Ghi sổ cái kiểm toán (`sp_NapTienVi` & `LichSuGiaoDichVi`):
* Gọi `POST /api/v1/wallets/top-up` với:
  ```json
  {
    "maKH": 3,
    "soTien": 150000,
    "noiDung": "Nạp tiền ví Bách Hóa Xanh qua ngân hàng"
  }
  ```
* **Kết quả kiểm chứng:**
  * Số dư ví của Khách 3 tăng từ 50.000 lên 200.000 VND.
  * Gọi `GET /api/v1/wallets/3/transactions`, hệ thống hiển thị ngay dòng nhật ký kiểm toán mới (`LoaiGiaoDich = 'NAP_TIEN'`, số dư trước: 50.000, thay đổi: +150.000, số dư sau: 200.000 VND).

#### 2. Thao tác Giỏ hàng (`sp_ThemVaoGioHang`, `sp_LayGioHangKhachHang`):
* Thêm món: Gọi `POST /api/v1/cart/items` với `{"maKH": 1, "maSP": 2, "soLuong": 1}`.
* Xem giỏ: Gọi `GET /api/v1/cart/1`.
  * **Kết quả:** Giỏ hàng tự động tính toán đơn giá, thành tiền và cờ `conDuHang = true`.

#### 3. Thanh toán đơn hàng chuẩn ACID (`sp_ThanhToanDonHang` - REQ-TX-01):
* **Ca 1 (Ví không đủ tiền):**
  * Khách 3 (số dư 200.000 VND) nếu giỏ hàng có tổng tiền > 200.000 VND -> Bị Rollback với mã lỗi `-2: Số dư ví không đủ thanh toán`. Tồn kho và số dư ví được giữ nguyên vẹn 100%.
* **Ca 2 (Thanh toán thành công):**
  * Khách 1 (Số dư 1.000.000 VND) gọi `POST /api/v1/orders/checkout` với `{"maKH": 1}`.
  * **Kết quả nguyên tử (Atomicity & Consistency):**
    * Trừ tiền ví tương ứng và trừ số lượng tồn kho của các sản phẩm đã mua.
    * Tạo bản ghi mới trong bảng `DonHang` và `ChiTietDonHang`.
    * Tự động xóa sạch các mặt hàng trong `ChiTietGioHang` sau khi mua.
    * Ghi sổ cái `LichSuGiaoDichVi` với `LoaiGiaoDich = 'THANH_TOAN'`, `SoTienThayDoi` là số âm.

#### 4. 🌟 1-Click Concurrency Demo: Kiểm thử tranh chấp đồng thời 2 luồng:
* Đây là tính năng đặc sắc nhất của Đồ án phục vụ báo cáo trực tiếp với Thầy/Cô.
* Mở Swagger UI, tìm endpoint:
  👉 **`POST /api/v1/orders/concurrency-test`** -> Bấm **Try it out** -> **Execute**.
* **Cơ chế hoạt động bên dưới:**
  1. Hệ thống tự động thiết lập sản phẩm Flash Sale: *Tôm hùm Alaska Flash Sale* với **Số lượng tồn kho đúng = 1**.
  2. Tự động đưa sản phẩm này vào giỏ hàng của cả Khách 1 (VIP) và Khách 2.
  3. Sử dụng `CountDownLatch` kích hoạt 2 luồng độc lập của Khách 1 và Khách 2 cùng nhấn nút **Thanh toán tại đúng 1 mili-giây**.
* **Kết quả đối chứng trả về ngay trên Swagger:**
  * **1 Luồng THÀNH CÔNG:** Tạo mã đơn hàng mới, trừ tiền ví 100.000 VND.
  * **1 Luồng THẤT BẠI:** Bị ngắt và Rollback với thông điệp: *`Một hoặc nhiều sản phẩm trong giỏ đã hết hàng hoặc không đủ tồn kho!`*, số dư ví của khách hàng này được bảo toàn nguyên vẹn 100%.
  * **Đánh giá ACID Isolation:** Trả về kết luận `PASS` xác nhận MS SQL Server Transaction với `SET XACT_ABORT ON` và khóa dòng `UPDLOCK, ROWLOCK` hoạt động chuẩn xác!

---

### 🤖 KỊCH BẢN KIỂM THỬ MODULE 3 TRÊN SWAGGER UI (AI DỰ BÁO NHU CẦU & NHẬP HÀNG ONNX)

#### 1. Kiểm thử Dự báo tự động theo mã sản phẩm (`GET /api/v1/ai/forecast/product/{productId}`):
* Mở Swagger UI, gọi:
  👉 **`GET /api/v1/ai/forecast/product/1`** (Cá lóc tươi làm sạch).
* **Cơ chế:**
  * Hệ thống tự động trích xuất các đặc trưng: `ProductId = 1`, `UnitPrice = 75000`, `DayOfWeek`, `IsWeekend`, `Month`, `DayOfMonth` và chuỗi `Lag/Rolling` từ CSDL.
  * Chuẩn hóa qua vector `StandardScaler` và suy luận bằng mô hình `PyTorch-MLP-ONNX` trực tiếp trên Java JVM.
* **Kết quả trả về:**
  * `soLuongDuBaoTieuThu`: Dự báo nhu cầu khách mua ngày mai (VD: ~45 - 55 con).
  * `soLuongDeXuatNhap`: Thuật toán tính toán lượng nhập an toàn (buffer 10% trừ tồn kho hiện tại).
  * `doTreInferenceMs`: **Dưới 10ms** (Không cần bất kỳ môi trường Python nào khi chạy!).
  * Tự động lưu bản ghi vào bảng **`DuBaoNhapHang`** thông qua Stored Procedure `sp_LuuKetQuaDuBaoNhapHang`.

#### 2. Kiểm thử Kịch bản Giả lập tùy chỉnh (`POST /api/v1/ai/forecast/predict-custom`):
* Gọi `POST /api/v1/ai/forecast/predict-custom` với:
  ```json
  {
    "maSP": 1,
    "currentStock": 80,
    "daysUntilExpiry": 1,
    "unitPrice": 75000,
    "lag1": 30.0,
    "lag7": 32.0,
    "rollingMean7": 31.0,
    "rollingStd7": 3.5
  }
  ```
* **Kết quả kiểm chứng:**
  * Do tồn kho lớn (80 con) trong khi hạn sử dụng chỉ còn 1 ngày và dự báo tiêu thụ thấp, AI đánh giá mức độ rủi ro: **`ruiRoHuHong = "CAO"`**.
  * **Chu trình khép kín:** Stored Procedure `sp_LuuKetQuaDuBaoNhapHang` tự động kích hoạt sinh một bản ghi mới trong bảng **`NhatKyCanhBao`** loại **`CANH_BAO_RUI_RO_THUC_PHAM`** để cảnh báo Quản lý kho khẩn cấp giảm giá hoặc xả hàng thanh lý!

#### 3. Kiểm thử Chạy dự báo hàng loạt toàn bộ danh mục (`POST /api/v1/ai/forecast/run-batch`):
* Bấm **Execute** tại:
  👉 **`POST /api/v1/ai/forecast/run-batch`**.
* **Kết quả:** Quét qua toàn bộ 12 mặt hàng thực phẩm tươi sống, trả về tổng số lượng nhập kho khuyến nghị cho từng mặt hàng và thống kê các món có nguy cơ ế hàng quá hạn sử dụng.

#### 4. Xem lịch sử dự báo của sản phẩm trong CSDL (`GET /api/v1/ai/forecast/history/{productId}`):
* Gọi `GET /api/v1/ai/forecast/history/1` để đối chứng tất cả các mốc dự báo đã được lưu trữ trong bảng `DuBaoNhapHang`.

---

### 💬 KỊCH BẢN KIỂM THỬ MODULE 4 TRÊN SWAGGER UI (AI CHATBOT ẨM THỰC & MCP TOOLS)

#### 1. Hỏi công thức nấu ăn & kiểm tra tồn kho nguyên liệu (`POST /api/v1/ai/chatbot/chat`):
* Mở Swagger UI, gọi:
  👉 **`POST /api/v1/ai/chatbot/chat`** với:
  ```json
  {
    "customerId": 1,
    "message": "Tối nay tôi muốn nấu món canh chua cá lóc cho 4 người ăn, bách hóa có đủ đồ không?"
  }
  ```
* **Cơ chế:**
  * Chatbot tự động kích hoạt MCP Tool **`search_recipe_and_groceries`**.
  * Truy vấn bảng `CongThucMonAn` và liên kết với `SanPham` để kiểm tra tồn kho thực tế của từng món: Cá lóc tươi làm sạch (1 con), Cà chua tươi Đà Lạt (1 kg), Rau muống nước sạch (2 bó).
* **Kết quả trả về:**
  * Lời tư vấn tự nhiên thân thiện, liệt kê chi tiết từng nguyên liệu và tổng chi phí ước tính (~128.000 - 143.000 VND).
  * Cờ `canAddToCart = true` sẵn sàng cho phép bỏ vào giỏ hàng.

#### 2. Kích hoạt Bỏ giỏ hàng tự động & Thanh toán (`POST /api/v1/ai/chatbot/chat`):
* Khách hàng xác nhận bằng tin nhắn:
  ```json
  {
    "customerId": 1,
    "message": "Bỏ vào giỏ hàng và dẫn tôi đi thanh toán luôn nhé"
  }
  ```
* **Cơ chế & Kết quả:**
  * Chatbot tự động kích hoạt Stored Procedure: **`sp_ThemNguyenLieuMonAnVaoGioHang`**.
  * Tự động cộng dồn toàn bộ 3 nguyên liệu tươi sống vào giỏ hàng của Khách 1.
  * Phản hồi: *"Trợ lý AI đã tự động thêm đầy đủ 3 nguyên liệu tươi sống vào giỏ hàng của bạn..."* kèm đường dẫn chuyển tiếp `checkoutUrl: "/checkout?customerId=1"`.

#### 3. Kiểm thử Model Context Protocol (MCP Server Registry & Tool Execution):
* **Xem danh sách công cụ:** Gọi `GET /api/v1/ai/mcp/tools`.
  * Trả về đúng 4 công cụ chuẩn hóa JSON Schema (`search_recipe_and_groceries`, `add_recipe_to_customer_cart`, `semantic_product_search`, `create_checkout_redirect`) sẵn sàng cho các LLM Agent (Claude, Cursor, Gemini).
* **Thực thi công cụ trực tiếp:** Gọi `POST /api/v1/ai/mcp/tools/call` với:
  ```json
  {
    "name": "semantic_product_search",
    "arguments": { "query": "tôm" }
  }
  ```
  * Trả về ngay thông tin sản phẩm *Tôm sú tươi sống* kèm giá bán và tồn kho.

---

### ⚡ KỊCH BẢN KIỂM THỬ MODULE 5 TRÊN SWAGGER UI (INDEXING BENCHMARK 1M DÒNG)

#### 1. 1-Click Benchmark So sánh Trực tiếp (`GET /api/v1/benchmark/compare`):
* Mở Swagger UI, bấm:
  👉 **`GET /api/v1/benchmark/compare`** -> **Execute**.
* **Hệ thống thực thi đo kiểm đối chứng:**
  * **Kịch bản 1 (Không chỉ mục):** Ép quét toàn bộ bảng tuần tự (`Clustered Index Scan / Table Scan`).
  * **Kịch bản 2 (Có Covering Index):** Ép tìm kiếm nhị phân B-Tree (`Index Seek` trên `IX_DonHang_NgayTao_TrangThai_Covering`).
* **Kết quả đối chứng trả về trực quan:**
  * `speedupFactor`: **Tăng tốc từ 10x đến 20x lần**.
  * `timeReductionPercentage`: **Giảm trên 90% thời gian phản hồi**.
  * `academicEvaluation`: Đánh giá chuẩn kiến trúc môn học CT221H (Loại bỏ Key Lookup, tối ưu B-Tree Seek).

#### 2. Sinh dữ liệu lớn phục vụ báo cáo đồ án (`POST /api/v1/benchmark/generate-data`):
* Gọi `POST /api/v1/benchmark/generate-data?count=50000` (hoặc `100000`, `500000`).
* T-SQL CTE đệ quy siêu tốc sinh hàng chục ngàn đơn hàng chỉ trong 1-3 giây!

#### 3. Bật/Tắt Chỉ mục để chứng minh với Thầy/Cô (`POST /api/v1/benchmark/indexes/toggle`):
* Gọi `POST /api/v1/benchmark/indexes/toggle?enable=false` (Gỡ chỉ mục) -> Chạy lại Compare để thấy thời gian tăng vọt.
* Gọi `POST /api/v1/benchmark/indexes/toggle?enable=true` (Bật chỉ mục) -> Chạy lại Compare để thấy thời gian giảm về tức thì!

---

### 🧠 KỊCH BẢN KIỂM THỬ MODULE 6 TRÊN SWAGGER UI (TÌM KIẾM NGỮ NGHĨA AI & QDRANT VECTOR DB)

#### 1. Kiểm tra trạng thái Qdrant & Mở Web Dashboard (`GET /api/v1/search/qdrant-status`):
* Mở Swagger UI, bấm:
  👉 **`GET /api/v1/search/qdrant-status`** -> **Execute**.
* **Kết quả:** Trả về `status = "ONLINE"`, `pointsCount = 12`, `vectorDimension = 384` kèm đường dẫn trực tiếp:
  👉 **`http://localhost:6333/dashboard`**
* Mở trình duyệt vào link trên, Thầy/Cô có thể xem trực quan toàn bộ các vector 384 chiều, khoảng cách Cosine và metadata payload đã được đánh chỉ mục HNSW trong Qdrant!

#### 2. Kiểm thử Tìm kiếm Ngữ nghĩa theo Nhu cầu Nấu ăn (`GET /api/v1/search/semantic`):
* **Ca 1 (Nhu cầu nước ép detox):** 
  * Gọi `GET /api/v1/search/semantic?query=nguyên liệu làm nước ép thanh lọc giảm mỡ`
  * **Kết quả vượt trội:** Hệ thống tự động gợi ý:
    * **Cần tây sạch Đà Lạt** (Điểm tương đồng Cosine: `~0.85`, `matchType = "SEMANTIC_VECTOR"`).
    * **Táo xanh Ninh Thuận** (Điểm tương đồng Cosine: `~0.78`).
    * **Gừng tươi cay** (Điểm tương đồng Cosine: `~0.65`).
    *(Mặc dù tên sản phẩm không hề chứa cụm từ "nước ép thanh lọc giảm mỡ"!)*
* **Ca 2 (Nhu cầu món mặn gia đình):**
  * Gọi `GET /api/v1/search/semantic?query=thịt nấu món kho cho bữa cơm`
  * **Kết quả:** Trả về **Thịt ba chỉ heo tươi sạch**, **Trứng gà tươi** kèm điểm tương đồng cao.

#### 3. Kiểm thử Tái đồng bộ dữ liệu Vector (`POST /api/v1/search/sync`):
* Gọi **`POST /api/v1/search/sync`**.
* **Kết quả:** Tự động quét toàn bộ sản phẩm từ MS SQL Server, chuyển đổi thành vector qua mô hình ONNX nội bộ và nạp hàng loạt vào Qdrant trong **dưới 50ms**.

#### 4. Kiểm thử Đồng bộ Thời gian thực (Real-time Hook):
* Thêm sản phẩm mới qua `POST /api/v1/products` -> Vector tự động được tạo và nạp ngay vào Qdrant mà không cần can thiệp thủ công!
* Xóa mềm sản phẩm qua `DELETE /api/v1/products/{id}` -> Vector tương ứng tự động được gỡ bỏ khỏi Qdrant để đảm bảo tính nhất quán dữ liệu 100%.

---

### 🔥 KỊCH BẢN KIỂM THỬ MODULE 7 TRÊN SWAGGER UI (KHUYẾN MÃI, FLASH SALE & TỰ ĐỘNG XẢ KHO 3NF)

#### 1. Lấy danh sách chương trình khuyến mãi (`GET /api/v1/promotions`):
* Mở Swagger UI, bấm **`GET /api/v1/promotions?activeOnly=true`** -> **Execute**.
* **Kết quả:** Trả về các chương trình đang còn hiệu lực:
  * **Flash Sale Giờ Vàng Thủy Hải Sản** (`FLASH_SALE`, giảm 15 - 20%).
  * **Lễ Hội Nông Sản Đà Lạt Tươi Sạch** (`GIAM_THEO_MUA`, giảm 10 - 25%).

#### 2. Xem chi tiết chương trình & danh sách sản phẩm giảm giá (`GET /api/v1/promotions/{id}`):
* Bấm **`GET /api/v1/promotions/1`** -> **Execute**.
* **Kết quả:** Trả về chi tiết chiến dịch, danh sách sản phẩm kèm:
  * `giaGoc`: Giá niêm yết ban đầu (VD: Tôm sú 220.000 đ).
  * `giaKhuyenMai`: Giá ưu đãi (VD: 176.000 đ).
  * `phanTramGiam`: Tỷ lệ giảm giá tự động tính (VD: `20.00%`).
  * `tienTietKiem`: Số tiền khách hàng tiết kiệm được (VD: `44.000 đ/kg`).

#### 3. Kiểm thử Ràng buộc Toàn vẹn & Trigger kiểm soát giá (`trg_ChiTietKhuyenMai_KiemSoatGia`):
* Bấm **`POST /api/v1/promotions/1/products`**:
  * **Ca 1 (Giá KM sai quy tắc):** Gửi `giaKhuyenMai = 300000` (cao hơn giá gốc 220.000 đ).
    * **Kết quả:** Stored Procedure và Trigger chặn đứng giao dịch, trả về thông báo lỗi: *"Giá khuyến mãi phải nhỏ hơn giá gốc!"*.
  * **Ca 2 (Giá KM hợp lệ):** Gửi `giaKhuyenMai = 160000`.
    * **Kết quả:** Thành công, trigger tự động tính toán lại tỷ lệ giảm giá `27.27%`.

#### 4. Kiểm thử Tự động Xả kho Thực phẩm Cận Date (`POST /api/v1/promotions/auto-clearance`):
* Bấm **`POST /api/v1/promotions/auto-clearance?soNgayCanDate=2&phanTramGiam=35.00`** -> **Execute**.
* **Cơ chế:** Stored Procedure `sp_TuDongKichHoatXaKhoCanDate` tự động quét toàn bộ thực phẩm có HSD $\le 2$ ngày, tự động sinh chiến dịch `XA_KHO_CAN_DATE` và áp dụng mức giảm 35%.
* **Kết quả:** Trả về thông báo thành công và số lượng mặt hàng cận date đã được giải cứu xả kho!

#### 5. Kiểm thử Tích hợp Trợ lý AI Chatbot Nhận diện Khuyến mãi:
* Gọi **`POST /api/v1/ai/chatbot/chat`** với câu hỏi:
  ```json
  {
    "customerId": 1,
    "message": "Hôm nay bách hóa có chương trình khuyến mãi hay giảm giá gì không?"
  }
  ```
* **Kết quả:** Chatbot AI tự động truy vấn danh sách ưu đãi, liệt kê chi tiết các chương trình Flash Sale, giá gốc, giá ưu đãi và phần trăm giảm giá theo phong cách nhiệt tình, thân thiện!



# CT221H_NguyenLyHeQuanTriCSDL
# CT221H_NguyenLyHeQuanTriCSDL
