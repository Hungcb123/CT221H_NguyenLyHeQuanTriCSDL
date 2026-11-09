# TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)

## HỆ THỐNG THƯƠNG MẠI ĐIỆN TỬ BÁCH HÓA THỰC PHẨM TƯƠI SỐNG NÂNG CAO
### (ADVANCED FRESH GROCERY E-COMMERCE PLATFORM)

---

* **Tên đồ án:** Hệ thống Quản trị & Thương mại điện tử Bách Hóa Thực phẩm Tươi Sống (Mô hình Bách Hóa Xanh)
* **Mã học phần:** CT221H - Nguyên lý Hệ quản trị Cơ sở Dữ liệu
* **Mã tài liệu:** `SRS-FRESH-ECOM-2026`
* **Hệ quản trị CSDL mục tiêu:** Microsoft SQL Server (2019 / 2022 Enterprise / Developer Edition)
* **Backend Framework:** Spring Boot 3.3+ (Java 17/21 LTS, Spring Data JPA, `SimpleJdbcCall`, Spring AI, Spring AI MCP, ONNX Runtime Java)
* **Công nghệ Trí tuệ Nhân tạo:** Model Context Protocol (MCP), LLM Vector Embeddings (Semantic Search), Machine Learning (LightGBM / ONNX Demand Forecasting)

---

## MỤC LỤC
1. [GIỚI THIỆU (INTRODUCTION)](#1-giới-thiệu-introduction)
   - 1.1. Mục đích tài liệu
   - 1.2. Phạm vi hệ thống & Bối cảnh nghiệp vụ Bách Hóa Tươi Sống
   - 1.3. Định nghĩa, thuật ngữ và chữ viết tắt
2. [KIẾN TRÚC TỔNG THỂ HỆ THỐNG (SYSTEM ARCHITECTURE)](#2-kiến-trúc-tổng-thể-hệ-thống-system-architecture)
   - 2.1. Kiến trúc phân tầng (Layered Architecture)
   - 2.2. Cơ chế tích hợp Spring Boot và Microsoft SQL Server
   - 2.3. Luồng kết nối AI, MCP Server, Chatbot ẩm thực và Mô hình ML Dự báo
3. [THIẾT KẾ CƠ SỞ DỮ LIỆU & LƯỢC ĐỒ THỰC THỂ (DATABASE SCHEMA)](#3-thiết-kế-cơ-sở-dữ-liệu--lược-đồ-thực-thể-database-schema)
   - 3.1. Danh mục 13 bảng thực thể chuẩn hóa
   - 3.2. Từ điển dữ liệu chi tiết (Data Dictionary)
   - 3.3. Lược đồ quan hệ thực thể (ERD Specification)
4. [PHÂN HỆ THỦ TỤC LƯU TRỮ (STORED PROCEDURES - CRUD)](#4-phân-hệ-thủ-tục-lưu-trữ-stored-procedures---crud)
   - 4.1. `REQ-SP-01`: Quản lý thêm mới sản phẩm có tham số kiểm soát
   - 4.2. `REQ-SP-02`: Cập nhật thông tin và điều chỉnh giá thực phẩm
   - 4.3. `REQ-SP-03`: Truy vấn danh sách phân trang kết hợp lọc đa tiêu chí
   - 4.4. `REQ-SP-04`: Quản lý giỏ hàng và công thức món ăn
   - 4.5. `REQ-SP-05`: Trích xuất dữ liệu lịch sử bán hàng và lưu kết quả AI Dự báo nhu cầu
5. [PHÂN HỆ QUẢN LÝ GIAO DỊCH (TRANSACTION MANAGEMENT & CONCURRENCY)](#5-phân-hệ-quản-lý-giao-dịch-transaction-management--concurrency)
   - 5.1. `REQ-TX-01`: Thủ tục thanh toán đơn hàng & trừ kho an toàn (`sp_ThanhToanDonHang`)
   - 5.2. Đảm bảo 4 thuộc tính ACID và kỹ thuật khóa dòng (`UPDLOCK, ROWLOCK`)
   - 5.3. Xử lý tranh chấp đồng thời và chống Deadlock
6. [PHÂN HỆ BẪY SỰ KIỆN TỰ ĐỘNG & CẢNH BÁO (TRIGGERS & ALERTING)](#6-phân-hệ-bẫy-sự-kiện-tự-động--cảnh-báo-triggers--alerting)
   - 6.1. `REQ-TRG-01`: Bẫy biến động giá bất thường, Cháy hàng và Hàng cận Date (`AFTER UPDATE`)
   - 6.2. `REQ-TRG-02`: Ngăn chặn xóa vật lý - Thực thi Soft Delete (`INSTEAD OF DELETE`)
   - 6.3. Kiến trúc Transactional Outbox gửi cảnh báo Real-time/Email qua Spring Boot
7. [PHÂN HỆ ĐÁNH GIÁ HIỆU NĂNG CHỈ MỤC (INDEXING BENCHMARK 1.000.000 RECORDS)](#7-phân-hệ-đánh-giá-hiệu-năng-chỉ-mục-indexing-benchmark-1000000-records)
   - 7.1. `REQ-IDX-01`: Kịch bản sinh tập dữ liệu lớn bằng T-SQL CTE (1M bản ghi)
   - 7.2. `REQ-IDX-02`: Thiết kế câu truy vấn phân tích doanh số và tồn kho thực phẩm
   - 7.3. `REQ-IDX-03`: Kịch bản so sánh Nonclustered Index + INCLUDE + Filtered Index
   - 7.4. Đo lường `STATISTICS IO`, `STATISTICS TIME` và Spring Boot Benchmark Runner
8. [PHÂN HỆ TÍCH HỢP TRÍ TUỆ NHÂN TẠO & CHATBOT ẨM THỰC (AI, MCP & ML FORECASTING)](#8-phân-hệ-tích-hợp-trí-tuệ-nhân-tạo--chatbot-ẩm-thực-ai-mcp--ml-forecasting)
   - 8.1. `REQ-AI-01`: Tìm kiếm ngữ nghĩa sản phẩm tươi sống (Semantic Vector Search)
   - 8.2. `REQ-AI-02`: Dự báo và tự động phân loại danh mục sản phẩm tươi
   - 8.3. `REQ-AI-03`: Chatbot tư vấn thực đơn, công thức món ăn & bỏ vào giỏ hàng tự động
   - 8.4. `REQ-AI-04`: Giao thức Model Context Protocol (MCP) Server trong Spring Boot
   - 8.5. `REQ-AI-05`: Mô hình Machine Learning Dự báo Nhu cầu Tiêu thụ & Đề xuất Nhập hàng tươi sống (ONNX Runtime Java)
9. [YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS - NFR)](#9-yêu-cầu-phi-chức-năng-non-functional-requirements---nfr)
10. [TIÊU CHÍ NGHIỆM THU ĐỒ ÁN (ACCEPTANCE CRITERIA)](#10-tiêu-chí-nghiệm-thu-đồ-án-acceptance-criteria)

---

## 1. GIỚI THIỆU (INTRODUCTION)

### 1.1. Mục đích tài liệu
Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này quy định toàn diện các yêu cầu nghiệp vụ, kiến trúc hệ thống, thiết kế cơ sở dữ liệu nâng cao, các giải thuật T-SQL (Stored Procedures, Transactions, Triggers, Indexes) và cơ chế tích hợp trí tuệ nhân tạo (AI, Model Context Protocol và Machine Learning Forecasting) cho **Hệ thống Thương mại điện tử Bách Hóa Thực phẩm Tươi Sống**.

Tài liệu là căn cứ kỹ thuật chuẩn mực phục vụ cho:
1. Phát triển cơ sở dữ liệu trên Microsoft SQL Server đáp ứng tuyệt đối chuẩn môn học **CT221H - Nguyên lý Hệ quản trị Cơ sở Dữ liệu**.
2. Triển khai tầng Backend bằng **Spring Boot 3.3+**, kết nối cơ sở dữ liệu qua **JDBC / SimpleJdbcCall**, tích hợp **Spring AI** và chạy mô hình học máy **ONNX Runtime** trực tiếp trên JVM.
3. Nghiệm thu, đánh giá hiệu năng chỉ mục trên tập dữ liệu quy mô lớn (1.000.000 dòng), kiểm thử giao dịch đồng thời (Concurrency Test) và chứng minh hiệu quả mô hình AI Dự báo lượng nhập hàng.

### 1.2. Phạm vi hệ thống & Bối cảnh nghiệp vụ Bách Hóa Tươi Sống
Hệ thống mô phỏng mô hình kinh doanh bán lẻ đa mặt hàng thực phẩm tươi sống kiểu **Bách Hóa Xanh**, tập trung giải quyết các bài toán kỹ thuật đặc thù:
* **Hàng hóa có vòng đời ngắn (Short Shelf-life):** Quản lý đơn vị tính linh hoạt (`kg`, `khay`, `bó`, `gam`), hạn sử dụng (`HanSuDung`), nhiệt độ bảo quản (`NhietDoBaoQuan`) và cảnh báo xả hàng khi cận date.
* **Biến động giá và tồn kho liên tục:** Cơ chế kiểm soát giá, bẫy sự kiện thay đổi giá bất thường do nhập sai dữ liệu, tự động thông báo khi mặt hàng tươi sống hết hàng.
* **Giao dịch thanh toán ví điện tử chuẩn ACID:** Khách hàng thanh toán đơn hàng bằng ví điện tử nội bộ (`SoDuVi`), hệ thống thực hiện kiểm tra khóa dòng, trừ kho, trừ tiền ví và tạo lịch sử giao dịch nguyên tử (Atomic).
* **Trợ lý Chatbot AI Ẩm Thực Thông Minh (Culinary AI Assistant):** Cho phép khách hàng trò chuyện ngôn ngữ tự nhiên (Ví dụ: *"Gợi ý nguyên liệu nấu canh chua cá lóc cho 4 người"*), AI tự động tra cứu công thức món ăn, kiểm tra tồn kho nguyên liệu tươi trong DB, cho phép khách hàng nhấn chọn bỏ toàn bộ nguyên liệu vào giỏ hàng và chuyển thẳng sang trang thanh toán.
* **Mô hình AI Học máy Dự báo Nhu cầu & Lượng nhập hàng (Demand & Restock Forecasting Model):** Giải quyết bài toán cốt tử của chuỗi bách hóa tươi sống: Hàng tươi sống nhập thừa thì thối rữa/tiêu hủy (lỗ vốn hao hụt), nhập thiếu thì cháy hàng (mất doanh thu). Mô hình phân tích lịch sử bán hàng theo chu kỳ ngày trong tuần và thời vụ để dự báo số lượng tiêu thụ ngày mai và đề xuất số lượng nhập hàng chính xác.

### 1.3. Định nghĩa, thuật ngữ và chữ viết tắt
| Thuật ngữ | Diễn giải |
| :--- | :--- |
| **MS SQL Server** | Hệ quản trị CSDL quan hệ của Microsoft phiên bản 2019/2022. |
| **T-SQL** | Transact-SQL, ngôn ngữ lập trình thủ tục mở rộng trên MS SQL Server. |
| **SP (Stored Procedure)** | Thủ tục lưu trữ trong CSDL có tham số đầu vào (`IN`), đầu ra (`OUTPUT`) và mã lỗi. |
| **ACID** | 4 đặc tính giao dịch: Nguyên tử (Atomicity), Nhất quán (Consistency), Độc lập (Isolation), Bền vững (Durability). |
| **UPDLOCK & ROWLOCK** | Gợi ý khóa dòng (Table Hint) trong T-SQL ngăn chặn Race Condition và Deadlock. |
| **Soft Delete** | Kỹ thuật đánh dấu cờ xóa (`TrangThaiXoa = 1`) mà không xóa vật lý bản ghi. |
| **Index Seek / Scan** | Các thao tác truy cập cây B-Tree của chỉ mục để định vị dữ liệu. |
| **MCP (Model Context Protocol)** | Giao thức tiêu chuẩn mở kết nối các mô hình AI với các công cụ/nguồn dữ liệu ngoài. |
| **ONNX (Open Neural Network Exchange)** | Định dạng chuẩn mở cho mô hình Machine Learning / Deep Learning, cho phép Spring Boot chạy suy luận trực tiếp bằng Java. |
| **Transactional Outbox** | Mô hình lưu trữ thông báo vào bảng trung gian trong cùng CSDL trước khi đẩy ra ngoài. |

---

## 2. KIẾN TRÚC TỔNG THỂ HỆ THỐNG (SYSTEM ARCHITECTURE)

### 2.1. Kiến trúc phân tầng (Layered Architecture)
Hệ thống được thiết kế theo mô hình 4 tầng phân tách trách nhiệm rõ ràng:

```
+-----------------------------------------------------------------------------------+
|                           CLIENT LAYER (Web / Mobile / Chat UI)                  |
|  - Giao diện mua sắm thực phẩm tươi sống & Giỏ hàng                               |
|  - Cửa sổ Chatbot tư vấn ẩm thực & nấu ăn thời gian thực                         |
|  - Dashboard Quản trị viên (Cảnh báo giá, hết hàng, Kế hoạch nhập hàng, Benchmark)|
+-----------------------------------------+-----------------------------------------+
                                          | REST API / Server-Sent Events (SSE)
+-----------------------------------------v-----------------------------------------+
|                    SPRING BOOT APPLICATION LAYER (Java 17/21)                     |
|  [REST Controllers]          [AI & ML Module]           [Outbox Scheduler]        |
|  - ProductController         - Spring AI ChatClient     - AlertNotificationWorker |
|  - OrderController           - MCP Server Tools         - WebSocket/Mail Service  |
|  - CartController            - Semantic Search Service  - Benchmark Runner        |
|  - ForecastController        - OnnxDemandForecastService                          |
|  -------------------------------------------------------------------------------  |
|  [Service & DAO Layer]                                                            |
|  - SimpleJdbcCall (Điều hướng Stored Procedures & Transaction)                    |
|  - Spring Data JPA (Ánh xạ Entity & các truy vấn tra cứu chuẩn)                   |
|  - OnnxRuntime Session (Nạp file model .onnx suy luận dự báo trực tiếp trên JVM)  |
+-----------------------------------------+-----------------------------------------+
                                          | JDBC Driver (com.microsoft.sqlserver)
+-----------------------------------------v-----------------------------------------+
|                  DATABASE ENGINE LAYER (Microsoft SQL Server)                     |
|  - Stored Procedures: sp_ThanhToanDonHang, sp_ThemSanPhamMoi, sp_PhanTrang...     |
|  - Triggers: trg_SanPham_CanhBaoBienDong (AFTER), trg_SanPham_SoftDelete (INSTEAD)|
|  - Indexes: Clustered, Covering Nonclustered Index + INCLUDE, Filtered Index     |
|  - Tables: SanPham, CongThucMonAn, DonHang, LichSuGiaoDichVi, DuBaoNhapHang...    |
+-----------------------------------------------------------------------------------+
```

### 2.2. Cơ chế tích hợp Spring Boot và Microsoft SQL Server
1. **Kết nối JDBC Connection Pool:** Sử dụng `HikariCP` tích hợp sẵn trong Spring Boot, kết nối an toàn với MS SQL Server qua chuỗi `jdbc:sqlserver://localhost:1433;databaseName=FreshGroceryDB;encrypt=true;trustServerCertificate=true`.
2. **Thực thi Stored Procedure có tham số kiểm soát (`SimpleJdbcCall`):**
   * Sử dụng `org.springframework.jdbc.core.simple.SimpleJdbcCall` để gọi trực tiếp các Stored Procedure của MS SQL Server.
   * Ánh xạ chính xác các tham số đầu vào (`IN`) và nhận các giá trị đầu ra (`OUTPUT`) như `@NewMaSP`, `@StatusCode`, `@Message`.
3. **Ủy quyền toàn diện Transaction cho CSDL:**
   * Toàn bộ thao tác thanh toán, trừ số dư ví, trừ số lượng tồn kho, ghi đơn hàng và ghi sổ cái giao dịch được gói trọn vẹn trong `BEGIN TRANSACTION ... COMMIT TRANSACTION` của Stored Procedure `sp_ThanhToanDonHang`.
   * Tầng Spring Boot chỉ cần kích hoạt Stored Procedure và xử lý kết quả thông qua `@StatusCode`. Nếu lỗi, Stored Procedure tự động `ROLLBACK`, ngăn chặn hoàn toàn lỗi mất đồng bộ dữ liệu giữa ứng dụng và CSDL.

### 2.3. Luồng kết nối AI, MCP Server, Chatbot ẩm thực và Mô hình ML Dự báo
1. **Spring AI & LLM:** Sử dụng Spring AI kết nối với mô hình LLM (Gemini / OpenAI / Ollama Local) để xử lý ngôn ngữ tự nhiên.
2. **Giao thức MCP Server (Model Context Protocol):**
   * Spring Boot triển khai một MCP Server đóng vai trò Tool Provider cung cấp các công cụ cho AI:
     * `tool_search_recipe`: Tìm kiếm công thức món ăn theo tên món, khẩu phần hoặc nguyên liệu.
     * `tool_check_fresh_inventory`: Kiểm tra tình trạng còn hàng và giá bán của nguyên liệu tươi trong kho.
     * `tool_add_ingredients_to_cart`: Tự động thêm danh sách các nguyên liệu cần thiết vào giỏ hàng của khách.
     * `tool_navigate_to_checkout`: Khởi tạo phiên thanh toán và cung cấp đường dẫn `/checkout`.
3. **Vector Embeddings trong MS SQL Server:** Bảng `SanPham_Vector` lưu trữ vector đặc trưng (vector 384 hoặc 1536 chiều) dưới dạng chuỗi JSON hoặc mảng nhị phân, phục vụ tính toán độ tương đồng Cosine (Cosine Similarity) cho bài toán tìm kiếm ngữ nghĩa (Semantic Search).
4. **Mô hình Học máy Dự báo Nhập hàng (ONNX Runtime Java):**
   * Huấn luyện mô hình hồi quy (LightGBM / Random Forest) dựa trên dữ liệu siêu thị bán lẻ Đông Nam Á (Kaggle) và lịch sử bán hàng trong SQL Server.
   * Xuất file mô hình `grocery_demand_forecast.onnx`.
   * Spring Boot sử dụng thư viện `com.microsoft.onnxruntime:onnxruntime` để nạp file `.onnx` và thực thi suy luận trực tiếp trên JVM, tự động tính toán nhu cầu tiêu thụ và đề xuất số lượng nhập hàng ngày mai, lưu kết quả vào bảng `DuBaoNhapHang`.

---

## 3. THIẾT KẾ CƠ SỞ DỮ LIỆU & LƯỢC ĐỒ THỰC THỂ (DATABASE SCHEMA)

### 3.1. Danh mục 13 bảng thực thể chuẩn hóa
Hệ thống gồm 13 bảng thực thể chuẩn hóa (chuẩn 3NF), hỗ trợ đầy đủ các yêu cầu nghiệp vụ bách hóa tươi sống và đồ án môn học:

1. **`KhachHang`**: Thông tin người tiêu dùng, số dư ví điện tử và hạng thành viên.
2. **`DanhMuc`**: Danh mục nhóm hàng (Rau củ quả, Thịt cá tươi sống, Thủy hải sản, Trái cây tươi, Gia vị - Đồ khô).
3. **`SanPham`**: Thông tin thực phẩm tươi sống, đơn vị tính, giá bán, tồn kho, hạn sử dụng và nhiệt độ bảo quản.
4. **`CongThucMonAn`**: Danh sách công thức nấu ăn, món ngon gia đình, khẩu phần và hướng dẫn chế biến.
5. **`NguyenLieuMonAn`**: Bảng liên kết nguyên liệu thực phẩm cụ thể trong kho cho từng món ăn.
6. **`GioHang`**: Giỏ hàng lưu trữ phiên mua sắm của khách hàng.
7. **`ChiTietGioHang`**: Chi tiết sản phẩm và số lượng đặt trong giỏ.
8. **`DonHang`**: Hóa đơn mua hàng, tổng giá trị thanh toán, trạng thái giao hàng.
9. **`ChiTietDonHang`**: Chi tiết từng dòng sản phẩm, số lượng mua, đơn giá tại thời điểm xuất bán.
10. **`LichSuGiaoDichVi`**: Sổ cái kiểm toán biến động số dư ví điện tử (Audit Trail) phục vụ tiêu chuẩn ACID.
11. **`NhatKyCanhBao`**: Nhật ký bẫy sự kiện Trigger dành cho Ban Quản trị (Hết hàng, sửa giá bất thường, xóa mềm).
12. **`ThongBaoKhachHang`**: Bảng hàng đợi thông báo Transactional Outbox gửi email/thông báo cho khách hàng.
13. **`DuBaoNhapHang`**: Lưu trữ kết quả dự báo nhu cầu tiêu thụ và khuyến nghị số lượng nhập hàng tươi sống từ mô hình Machine Learning.
14. **`SanPham_Vector`**: Lưu trữ embedding vector phục vụ tìm kiếm ngữ nghĩa và AI Recommendation.

---

### 3.2. Từ điển dữ liệu chi tiết (Data Dictionary)

#### Bảng: `KhachHang` (Quản lý khách hàng & Ví điện tử)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaKH` | `INT` | `PK, IDENTITY(1,1)` | Mã định danh khách hàng |
| `HoTen` | `NVARCHAR(100)` | `NOT NULL` | Họ và tên khách hàng |
| `Email` | `VARCHAR(100)` | `NOT NULL, UNIQUE` | Địa chỉ email liên hệ |
| `SoDienThoai` | `VARCHAR(15)` | `NOT NULL, UNIQUE` | Số điện thoại nhận hàng |
| `DiaChi` | `NVARCHAR(255)` | `NULL` | Địa chỉ giao thực phẩm |
| `SoDuVi` | `DECIMAL(18,2)` | `NOT NULL, DEFAULT 0, CHECK (SoDuVi >= 0)` | Số dư tài khoản ví điện tử |
| `NgayTao` | `DATETIME2` | `NOT NULL, DEFAULT GETDATE()` | Ngày đăng ký tài khoản |

#### Bảng: `DanhMuc` (Danh mục thực phẩm)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaDanhMuc` | `INT` | `PK, IDENTITY(1,1)` | Mã danh mục |
| `TenDanhMuc` | `NVARCHAR(100)` | `NOT NULL, UNIQUE` | Tên nhóm hàng (VD: Rau củ, Hải sản tươi) |
| `MoTa` | `NVARCHAR(255)` | `NULL` | Mô tả chi tiết nhóm hàng |

#### Bảng: `SanPham` (Hàng hóa thực phẩm tươi sống)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaSP` | `INT` | `PK, IDENTITY(1,1)` | Mã sản phẩm |
| `TenSP` | `NVARCHAR(200)` | `NOT NULL` | Tên thực phẩm (VD: Cá bớp tươi cắt lát) |
| `MoTa` | `NVARCHAR(MAX)` | `NULL` | Mô tả nguồn gốc, dinh dưỡng |
| `Gia` | `DECIMAL(18,2)` | `NOT NULL, CHECK (Gia > 0)` | Giá bán niêm yết hiện tại (VND) |
| `DonViTinh` | `NVARCHAR(20)` | `NOT NULL` | Đơn vị tính (`kg`, `khay`, `bó`, `vỉ`, `hộp`) |
| `SoLuongTon` | `INT` | `NOT NULL, CHECK (SoLuongTon >= 0)` | Số lượng còn lại trong kho |
| `HanSuDung` | `DATETIME2` | `NOT NULL` | Ngày hết hạn sử dụng của thực phẩm |
| `NhietDoBaoQuan`| `NVARCHAR(50)` | `NULL` | Gợi ý bảo quản (VD: Ngăn mát 2-4°C, Đông lạnh) |
| `MaDanhMuc` | `INT` | `NOT NULL, FK -> DanhMuc(MaDanhMuc)` | Danh mục phân loại |
| `TrangThaiXoa` | `BIT` | `NOT NULL, DEFAULT 0` | Cờ xóa mềm (0: Hoạt động, 1: Đã xóa mềm) |

#### Bảng: `CongThucMonAn` (Công thức món ăn phục vụ Chatbot)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaMon` | `INT` | `PK, IDENTITY(1,1)` | Mã món ăn |
| `TenMon` | `NVARCHAR(150)` | `NOT NULL` | Tên món (VD: Canh chua cá lóc miền Tây) |
| `MoTa` | `NVARCHAR(MAX)` | `NULL` | Tóm tắt hương vị, độ khó, thời gian nấu |
| `KhauPhan` | `INT` | `NOT NULL, DEFAULT 4` | Số người ăn chuẩn (khẩu phần) |
| `CachCheBien` | `NVARCHAR(MAX)` | `NULL` | Các bước sơ chế và nấu nướng |

#### Bảng: `NguyenLieuMonAn` (Chi tiết nguyên liệu tươi của từng món)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaMon` | `INT` | `PK, FK -> CongThucMonAn(MaMon)` | Mã món ăn |
| `MaSP` | `INT` | `PK, FK -> SanPham(MaSP)` | Mã sản phẩm tươi sống tương ứng trong kho |
| `SoLuongCan` | `INT` | `NOT NULL, CHECK (SoLuongCan > 0)` | Số lượng cần thiết theo đơn vị tính của SP |
| `GhiChu` | `NVARCHAR(100)` | `NULL` | Hướng dẫn (VD: Cắt khúc sẵn, rửa sạch) |

#### Bảng: `GioHang` & `ChiTietGioHang` (Giỏ hàng của khách)
* **`GioHang`**: `MaGioHang` (`INT, PK, IDENTITY`), `MaKH` (`INT, FK, UNIQUE`), `NgayCapNhat` (`DATETIME2`).
* **`ChiTietGioHang`**: `MaGioHang` (`INT, PK, FK`), `MaSP` (`INT, PK, FK`), `SoLuong` (`INT, CHECK > 0`).

#### Bảng: `DonHang` & `ChiTietDonHang` (Đơn đặt hàng)
* **`DonHang`**:
  * `MaDH` (`INT, PK, IDENTITY(1,1)`).
  * `MaKH` (`INT, FK -> KhachHang(MaKH)`).
  * `NgayTao` (`DATETIME2, DEFAULT GETDATE()`).
  * `TongTien` (`DECIMAL(18,2), CHECK (TongTien >= 0)`).
  * `TrangThai` (`VARCHAR(20), CHECK IN ('PENDING', 'PAID', 'DELIVERED', 'CANCELLED')`).
* **`ChiTietDonHang`**:
  * `MaDH` (`INT, PK, FK -> DonHang(MaDH)`).
  * `MaSP` (`INT, PK, FK -> SanPham(MaSP)`).
  * `SoLuong` (`INT, CHECK (SoLuong > 0)`).
  * `DonGia` (`DECIMAL(18,2), CHECK (DonGia > 0)`).

#### Bảng: `LichSuGiaoDichVi` (Sổ cái kiểm toán giao dịch ví - Audit Trail ACID)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaGiaoDich` | `BIGINT` | `PK, IDENTITY(1,1)` | Mã giao dịch tài chính duy nhất |
| `MaKH` | `INT` | `NOT NULL, FK -> KhachHang(MaKH)` | Chủ tài khoản ví |
| `MaDH` | `INT` | `NULL, FK -> DonHang(MaDH)` | Đơn hàng phát sinh giao dịch |
| `LoaiGiaoDich`| `VARCHAR(20)` | `NOT NULL, CHECK IN ('NAP_TIEN', 'THANH_TOAN', 'HOAN_TIEN')` | Bản chất giao dịch |
| `SoDuTruoc` | `DECIMAL(18,2)` | `NOT NULL` | Số dư trước thời điểm biến động |
| `SoTienThayDoi`| `DECIMAL(18,2)`| `NOT NULL` | Số tiền trừ hoặc cộng (Dương hoặc Âm) |
| `SoDuSau` | `DECIMAL(18,2)` | `NOT NULL` | Số dư chốt sau khi thực hiện giao dịch |
| `NgayGiaoDich`| `DATETIME2` | `NOT NULL, DEFAULT GETDATE()` | Thời điểm chính xác của giao dịch |
| `NoiDung` | `NVARCHAR(255)` | `NOT NULL` | Diễn giải (VD: Trừ tiền thanh toán đơn hàng #1002) |

#### Bảng: `NhatKyCanhBao` (Bẫy sự kiện dành cho Quản trị viên)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaLog` | `BIGINT` | `PK, IDENTITY(1,1)` | Mã nhật ký cảnh báo |
| `LoaiSuKien` | `VARCHAR(50)` | `NOT NULL` | `CANH_BAO_HET_HANG`, `CANH_BAO_GIA_BAT_THUONG`, `CANH_BAO_CAN_DATE`, `XOA_MEM_SAN_PHAM` |
| `NoiDung` | `NVARCHAR(MAX)` | `NOT NULL` | Chi tiết cảnh báo (giá cũ, giá mới, số tồn...) |
| `MaSP` | `INT` | `NULL` | Mã sản phẩm liên quan |
| `NguoiThucHien`| `NVARCHAR(100)` | `DEFAULT SUSER_SNAME()` | Tài khoản hệ thống/người dùng thực thi |
| `NgayGhi` | `DATETIME2` | `NOT NULL, DEFAULT GETDATE()` | Thời gian ghi nhật ký |

#### Bảng: `ThongBaoKhachHang` (Hàng đợi Transactional Outbox gửi cho User)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaThongBao` | `BIGINT` | `PK, IDENTITY(1,1)` | Mã thông báo |
| `MaKH` | `INT` | `NOT NULL, FK -> KhachHang(MaKH)` | Người nhận thông báo |
| `TieuDe` | `NVARCHAR(200)` | `NOT NULL` | Tiêu đề tin nhắn thông báo |
| `NoiDung` | `NVARCHAR(MAX)` | `NOT NULL` | Nội dung thông báo (hàng đã có lại, giá ưu đãi...) |
| `TrangThaiGui`| `VARCHAR(20)` | `DEFAULT 'PENDING'` | `PENDING`, `SENT`, `FAILED` |
| `NgayTao` | `DATETIME2` | `NOT NULL, DEFAULT GETDATE()` | Thời điểm tạo thông báo |

#### Bảng: `DuBaoNhapHang` (Lưu kết quả Machine Learning Dự báo nhu cầu)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaDuBao` | `BIGINT` | `PK, IDENTITY(1,1)` | Mã bản ghi dự báo duy nhất |
| `MaSP` | `INT` | `NOT NULL, FK -> SanPham(MaSP)` | Mã mặt hàng thực phẩm tươi |
| `NgayDuBao` | `DATE` | `NOT NULL` | Ngày cần dự báo tiêu thụ (VD: ngày mai) |
| `SoLuongDuBaoTieuThu` | `DECIMAL(10,2)` | `NOT NULL, CHECK (SoLuongDuBaoTieuThu >= 0)` | Lượng khách dự kiến tiêu thụ |
| `SoLuongTonHienTai` | `INT` | `NOT NULL` | Tồn kho thực tế tại thời điểm chạy mô hình |
| `SoLuongDeXuatNhap` | `INT` | `NOT NULL, CHECK (SoLuongDeXuatNhap >= 0)` | Lượng hàng khuyến nghị nhập kho |
| `DoTinCay` | `DECIMAL(5,2)` | `NOT NULL` | Điểm độ tin cậy của mô hình (%) |
| `ThuatToan` | `VARCHAR(50)` | `NOT NULL, DEFAULT 'LightGBM-ONNX'` | Tên thuật toán ML thực thi |
| `RuiRoHuHong` | `VARCHAR(20)` | `NOT NULL, CHECK IN ('THAP', 'TRUNG_BINH', 'CAO')` | Nguy cơ ế hàng quá hạn sử dụng |
| `NgayTao` | `DATETIME2` | `NOT NULL, DEFAULT GETDATE()` | Thời điểm chạy mô hình dự báo |

#### Bảng: `SanPham_Vector` (Lưu Vector Embedding)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Diễn giải |
| :--- | :--- | :--- | :--- |
| `MaSP` | `INT` | `PK, FK -> SanPham(MaSP)` | Mã sản phẩm liên kết |
| `VectorEmbedding`| `NVARCHAR(MAX)` | `NOT NULL` | Chuỗi JSON mảng số thực biểu diễn ngữ nghĩa |
| `ChieuVector` | `INT` | `NOT NULL, DEFAULT 384` | Số chiều vector embedding (VD: 384 hoặc 1536) |
| `NgayCapNhat`| `DATETIME2` | `NOT NULL, DEFAULT GETDATE()` | Thời điểm tính toán vector |

---

### 3.3. Lược đồ quan hệ thực thể (ERD Specification)

```
[DanhMuc] 1 -------- n [SanPham] 1 -------- 1 [SanPham_Vector]
                           |
                           +-- 1 ------ n [DuBaoNhapHang] (Kết quả AI ML)
                           |
                           +-- 1 ------ n [NguyenLieuMonAn] n ------ 1 [CongThucMonAn]
                           |
                           +-- 1 ------ n [ChiTietGioHang] n ------ 1 [GioHang]
                           |                                             |
                           |                                             | (1 - 1)
                           +-- 1 ------ n [ChiTietDonHang]               |
                                                |                        |
[KhachHang] 1 -------------------------- n [DonHang]                     |
     |      |                                   |                        |
     |      +-- 1 ------------------------------+------------------------+
     |      |
     |      +-- 1 ---------------------- n [LichSuGiaoDichVi]
     |      |
     |      +-- 1 ---------------------- n [ThongBaoKhachHang] (Outbox)
     |
[NhatKyCanhBao] (Sinh tự động từ Triggers trên SanPham)
```

---

## 4. PHÂN HỆ THỦ TỤC LƯU TRỮ (STORED PROCEDURES - CRUD)

Tất cả các nghiệp vụ dữ liệu bắt buộc đóng gói thành Stored Procedure với đầy đủ tham số đầu vào (`IN`), đầu ra (`OUTPUT`), kiểm tra tính hợp lệ dữ liệu và cấu trúc xử lý lỗi chuẩn `BEGIN TRY ... BEGIN CATCH`.

### 4.1. `REQ-SP-01`: Quản lý thêm mới sản phẩm có tham số kiểm soát
* **Tên thủ tục:** `sp_ThemSanPhamMoi`
* **Mục đích:** Thêm mới mặt hàng thực phẩm tươi sống với cơ chế kiểm duyệt chặt chẽ về giá, số lượng tồn kho và hạn sử dụng.
* **Tham số đầu vào (`IN`):**
  * `@TenSP` (`NVARCHAR(200)`): Tên sản phẩm.
  * `@MoTa` (`NVARCHAR(MAX)`): Mô tả chi tiết.
  * `@Gia` (`DECIMAL(18,2)`): Giá bán.
  * `@DonViTinh` (`NVARCHAR(20)`): Đơn vị tính (`kg`, `khay`, `bó`...).
  * `@SoLuongTon` (`INT`): Số lượng nhập kho ban đầu.
  * `@HanSuDung` (`DATETIME2`): Hạn dùng thực phẩm.
  * `@NhietDoBaoQuan` (`NVARCHAR(50)`): Nhiệt độ bảo quản khuyến nghị.
  * `@MaDanhMuc` (`INT`): Mã danh mục.
* **Tham số đầu ra (`OUTPUT`):**
  * `@NewMaSP` (`INT OUTPUT`): Khóa chính tự sinh của sản phẩm vừa thêm.
  * `@StatusCode` (`INT OUTPUT`): Mã trạng thái thực thi:
    * `1`: Thành công.
    * `-1`: Giá bán không hợp lệ (`Gia <= 0`).
    * `-2`: Số lượng tồn âm (`SoLuongTon < 0`).
    * `-3`: Hạn sử dụng không hợp lệ (nhỏ hơn ngày hiện tại).
    * `-4`: Danh mục hàng hóa không tồn tại.
    * `-99`: Lỗi hệ thống trong khối `CATCH`.
  * `@Message` (`NVARCHAR(255) OUTPUT`): Chuỗi thông điệp chi tiết mô tả kết quả.
* **Đặc tả thuật toán T-SQL:**
```sql
CREATE OR ALTER PROCEDURE sp_ThemSanPhamMoi
    @TenSP NVARCHAR(200),
    @MoTa NVARCHAR(MAX),
    @Gia DECIMAL(18,2),
    @DonViTinh NVARCHAR(20),
    @SoLuongTon INT,
    @HanSuDung DATETIME2,
    @NhietDoBaoQuan NVARCHAR(50),
    @MaDanhMuc INT,
    @NewMaSP INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        -- 1. Kiểm tra tính hợp lệ giá và số lượng
        IF (@Gia <= 0)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Giá bán sản phẩm phải lớn hơn 0 VND.';
            RETURN;
        END

        IF (@SoLuongTon < 0)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Số lượng tồn kho không được là số âm.';
            RETURN;
        END

        -- 2. Kiểm tra hạn sử dụng của thực phẩm tươi
        IF (@HanSuDung <= GETDATE())
        BEGIN
            SET @StatusCode = -3;
            SET @Message = N'Hạn sử dụng thực phẩm tươi sống phải lớn hơn thời điểm hiện tại.';
            RETURN;
        END

        -- 3. Kiểm tra danh mục
        IF NOT EXISTS (SELECT 1 FROM DanhMuc WHERE MaDanhMuc = @MaDanhMuc)
        BEGIN
            SET @StatusCode = -4;
            SET @Message = N'Mã danh mục không tồn tại trong hệ thống.';
            RETURN;
        END

        -- 4. Thực thi thêm mới
        INSERT INTO SanPham (TenSP, MoTa, Gia, DonViTinh, SoLuongTon, HanSuDung, NhietDoBaoQuan, MaDanhMuc, TrangThaiXoa)
        VALUES (@TenSP, @MoTa, @Gia, @DonViTinh, @SoLuongTon, @HanSuDung, @NhietDoBaoQuan, @MaDanhMuc, 0);

        SET @NewMaSP = SCOPE_IDENTITY();
        SET @StatusCode = 1;
        SET @Message = N'Thêm mới sản phẩm tươi sống thành công.';
    END TRY
    BEGIN CATCH
        SET @NewMaSP = 0;
        SET @StatusCode = -99;
        SET @Message = ERROR_MESSAGE();
    END CATCH
END;
```

### 4.2. `REQ-SP-02`: Cập nhật thông tin và điều chỉnh giá thực phẩm
* **Tên thủ tục:** `sp_CapNhatSanPham`
* **Tham số:** `@MaSP`, `@TenSP`, `@MoTa`, `@Gia`, `@SoLuongTon`, `@HanSuDung`, `@StatusCode OUTPUT`, `@Message OUTPUT`.
* **Nghiệp vụ:** Kiểm tra sản phẩm tồn tại và chưa bị xóa mềm (`TrangThaiXoa = 0`). Tiến hành cập nhật. Bất kỳ thay đổi giá hoặc số lượng tồn kho nào đều sẽ kích hoạt Trigger kiểm tra tính bất thường (mục 6.1).

### 4.3. `REQ-SP-03`: Truy vấn danh sách phân trang kết hợp lọc đa tiêu chí
* **Tên thủ tục:** `sp_LayDanhSachSanPhamPhanTrang`
* **Mục đích:** Hỗ trợ giao diện Web hiển thị danh sách thực phẩm mượt mà, hạn chế quá tải bộ nhớ bằng cơ chế `OFFSET ... FETCH NEXT`.
* **Tham số đầu vào:**
  * `@MaDanhMuc` (`INT`): Mã danh mục lọc (Truyền `NULL` nếu lấy tất cả).
  * `@TuKhoa` (`NVARCHAR(100)`): Từ khóa tìm kiếm gần đúng theo tên (`NULL` nếu không lọc).
  * `@GiaTu` (`DECIMAL(18,2)`), `@GiaDen` (`DECIMAL(18,2)`): Khoảng giá tìm kiếm.
  * `@SapXep` (`VARCHAR(20)`): Tiêu chí sắp xếp (`GIA_ASC`, `GIA_DESC`, `HSD_ASC`, `MOI_NHAT`).
  * `@TrangHienTai` (`INT`): Số trang (mặc định 1).
  * `@SoDongTrenTrang` (`INT`): Kích thước trang (mặc định 12).
* **Tham số đầu ra:**
  * `@TongSoBanGhi` (`INT OUTPUT`): Tổng số mặt hàng thỏa mãn bộ lọc.
  * `@TongSoTrang` (`INT OUTPUT`): Tổng số trang tương ứng.
* **Đầu ra dữ liệu:** Trả về tập kết quả (`ResultSet`) chứa thông tin chi tiết các sản phẩm đang hoạt động (`TrangThaiXoa = 0`).

### 4.4. `REQ-SP-04`: Quản lý giỏ hàng và thêm nguyên liệu món ăn hàng loạt
* **Tên thủ tục:** `sp_ThemNguyenLieuMonAnVaoGioHang`
* **Mục đích:** Được gọi bởi Chatbot AI khi khách hàng đồng ý thêm toàn bộ nguyên liệu của một công thức món ăn vào giỏ.
* **Tham số đầu vào:** `@MaKH` (`INT`), `@MaMon` (`INT`), `@SoKhauPhan` (`INT`).
* **Tham số đầu ra:** `@SoSanPhamDaThem` (`INT OUTPUT`), `@StatusCode` (`INT OUTPUT`), `@Message` (`NVARCHAR(255) OUTPUT`).
* **Xử lý:**
  1. Kiểm tra sự tồn tại của món ăn trong `CongThucMonAn`.
  2. Tính toán tỷ lệ nguyên liệu theo số khẩu phần khách yêu cầu.
  3. Duyệt danh sách trong `NguyenLieuMonAn` kiểm tra xem sản phẩm nào còn hàng trong `SanPham`.
  4. Tạo hoặc lấy `MaGioHang` của `@MaKH`. Dùng lệnh `MERGE` vào `ChiTietGioHang` để cộng dồn số lượng.

### 4.5. `REQ-SP-05`: Trích xuất lịch sử bán hàng và lưu kết quả AI Dự báo nhu cầu
* **Tên thủ tục 1:** `sp_LayLichSuBanHangHuanLuyen`
  * **Mục đích:** Tổng hợp chuỗi thời gian bán hàng theo ngày của từng sản phẩm tươi sống để làm dữ liệu huấn luyện hoặc cung cấp features đầu vào cho mô hình ONNX trong Spring Boot.
  * **Tham số:** `@SoNgayLichSu INT = 90` (Lấy dữ liệu trong 90 ngày gần nhất).
  * **Tập kết quả trả về:**
    * `MaSP`, `TenSP`, `NgayBan`, `DayOfWeek` (Thứ trong tuần 1-7), `IsWeekend` (1 nếu Thứ 7/CN, 0 nếu ngày thường).
    * `SoLuongBanNgay` (Tổng sản lượng bán), `GiaBanTrungBinh`, `DoanhThuNgay`.
* **Tên thủ tục 2:** `sp_LuuKetQuaDuBaoNhapHang`
  * **Mục đích:** Nhận kết quả suy luận từ mô hình ONNX trong Spring Boot và ghi nhận có kiểm soát vào bảng `DuBaoNhapHang`.
  * **Tham số đầu vào:** `@MaSP` (`INT`), `@NgayDuBao` (`DATE`), `@SoLuongDuBao` (`DECIMAL(10,2)`), `@SoLuongTon` (`INT`), `@SoLuongNhap` (`INT`), `@DoTinCay` (`DECIMAL(5,2)`), `@ThuatToan` (`VARCHAR(50)`), `@RuiRo` (`VARCHAR(20)`).
  * **Tham số đầu ra:** `@StatusCode` (`INT OUTPUT`), `@Message` (`NVARCHAR(255) OUTPUT`).

---

## 5. PHÂN HỆ QUẢN LÝ GIAO DỊCH (TRANSACTION MANAGEMENT & CONCURRENCY)

Đây là phân hệ cốt lõi chứng minh tính toàn vẹn giao dịch và tiêu chuẩn ACID của đồ án môn CSDL.

### 5.1. `REQ-TX-01`: Thủ tục thanh toán đơn hàng & trừ kho an toàn (`sp_ThanhToanDonHang`)
* **Tác tử thực thi:** Khách hàng tiến hành thanh toán giỏ hàng bằng số dư ví điện tử.
* **Tham số đầu vào:**
  * `@MaKH` (`INT`): Mã khách hàng thực hiện thanh toán.
  * `@MaGioHang` (`INT`): Giỏ hàng cần thanh toán.
* **Tham số đầu ra:**
  * `@MaDH_Output` (`INT OUTPUT`): Mã đơn hàng vừa tạo thành công.
  * `@StatusCode` (`INT OUTPUT`):
    * `1`: Giao dịch thành công trọn vẹn.
    * `-1`: Giỏ hàng rỗng hoặc không tồn tại.
    * `-2`: Số dư tài khoản ví không đủ thanh toán.
    * `-3`: Một hoặc nhiều mặt hàng trong kho không đủ số lượng tồn hoặc đã hết hạn.
    * `-99`: Lỗi hệ thống, giao dịch đã được tự động Rollback.
  * `@Message` (`NVARCHAR(255) OUTPUT`): Diễn giải kết quả.

### 5.2. Đảm bảo 4 thuộc tính ACID và kỹ thuật khóa dòng (`UPDLOCK, ROWLOCK`)
Thuật toán T-SQL bên trong `sp_ThanhToanDonHang` áp dụng chặt chẽ các kỹ thuật kiểm soát đồng thời:

1. **`SET XACT_ABORT ON`:** Đảm bảo nếu xảy ra bất kỳ lỗi runtime nào (ví dụ: mất kết nối đường truyền, vi phạm khóa ngoại), SQL Server sẽ tự động ngắt và hủy toàn bộ phiên giao dịch (`Auto-Rollback`).
2. **Khóa dòng độc quyền đọc-ghi (`WITH (UPDLOCK, ROWLOCK)`):**
   * Khóa tài khoản ví trong `KhachHang`: Ngăn ngừa vấn đề **Lost Update** khi khách hàng thực hiện nhiều giao dịch thanh toán đồng thời cùng 1 tích tắc.
   * Khóa từng dòng sản phẩm tương ứng trong `SanPham`: Ngăn ngừa tình trạng **Bán vượt tồn kho (Overselling / Dirty Read)** khi nhiều người cùng đặt món thực phẩm tươi sống cuối cùng trong kho.
3. **Tính nguyên tử (Atomicity):** Tất cả các hành vi: Trừ ví, Trừ kho, Sinh bản ghi `DonHang`, Sinh các dòng `ChiTietDonHang`, Sinh dòng kiểm toán `LichSuGiaoDichVi`, và Xóa giỏ hàng `ChiTietGioHang` phải cùng thành công 100% hoặc không có bất kỳ bảng nào bị thay đổi.
4. **Tính nhất quán (Consistency):** Ràng buộc `CHECK (SoDuVi >= 0)` và `CHECK (SoLuongTon >= 0)` trong Schema bảo vệ dữ liệu ở tầng thấp nhất.
5. **Tính độc lập (Isolation):** Mức cô lập mặc định `READ COMMITTED` kết hợp `UPDLOCK, ROWLOCK` đảm bảo dữ liệu luôn chính xác mà không làm khóa toàn bộ bảng (Table Lock).
6. **Tính bền vững (Durability):** Sau khi `COMMIT TRANSACTION`, dữ liệu ghi thẳng vào Transaction Log của MS SQL Server.

```sql
CREATE OR ALTER PROCEDURE sp_ThanhToanDonHang
    @MaKH INT,
    @MaGioHang INT,
    @MaDH_Output INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    -- Bảng tạm lưu trữ chi tiết đơn hàng tính toán
    DECLARE @Items TABLE (
        MaSP INT,
        TenSP NVARCHAR(200),
        SoLuongMua INT,
        GiaBan DECIMAL(18,2),
        ThanhTien DECIMAL(18,2)
    );

    DECLARE @TongTien DECIMAL(18,2) = 0;
    DECLARE @SoDuHienTai DECIMAL(18,2) = 0;
    DECLARE @SoDuSauGiaoDich DECIMAL(18,2) = 0;

    BEGIN TRY
        -- 1. BẮT ĐẦU PHIÊN GIAO DỊCH
        BEGIN TRANSACTION;

        -- 2. Kiểm tra giỏ hàng có sản phẩm không
        IF NOT EXISTS (SELECT 1 FROM ChiTietGioHang WHERE MaGioHang = @MaGioHang)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Giỏ hàng rỗng, không thể tiến hành thanh toán.';
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- 3. Khóa dòng kiểm tra số dư ví của khách hàng (UPDLOCK, ROWLOCK)
        SELECT @SoDuHienTai = SoDuVi
        FROM KhachHang WITH (UPDLOCK, ROWLOCK)
        WHERE MaKH = @MaKH;

        -- 4. Khóa dòng kiểm tra các sản phẩm trong kho và nạp vào bảng tạm
        INSERT INTO @Items (MaSP, TenSP, SoLuongMua, GiaBan, ThanhTien)
        SELECT 
            sp.MaSP,
            sp.TenSP,
            ctgh.SoLuong,
            sp.Gia,
            (ctgh.SoLuong * sp.Gia)
        FROM ChiTietGioHang ctgh
        INNER JOIN SanPham sp WITH (UPDLOCK, ROWLOCK) ON ctgh.MaSP = sp.MaSP
        WHERE ctgh.MaGioHang = @MaGioHang;

        -- 5. Kiểm tra tồn kho và hạn sử dụng từng sản phẩm
        IF EXISTS (
            SELECT 1 
            FROM @Items item
            INNER JOIN SanPham sp ON item.MaSP = sp.MaSP
            WHERE sp.SoLuongTon < item.SoLuongMua OR sp.HanSuDung <= GETDATE() OR sp.TrangThaiXoa = 1
        )
        BEGIN
            SET @StatusCode = -3;
            SET @Message = N'Một số sản phẩm tươi sống đã hết hàng, không đủ tồn kho hoặc đã hết hạn.';
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- 6. Tính tổng giá trị đơn hàng
        SELECT @TongTien = SUM(ThanhTien) FROM @Items;

        -- 7. Kiểm tra số dư ví
        IF (@SoDuHienTai < @TongTien)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Số dư ví không đủ thanh toán. Cần: ' + CAST(@TongTien AS NVARCHAR(30)) + N' VND, Hiện có: ' + CAST(@SoDuHienTai AS NVARCHAR(30)) + N' VND.';
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- 8. THỰC HIỆN TRỪ KHO SẢN PHẨM
        UPDATE sp
        SET sp.SoLuongTon = sp.SoLuongTon - item.SoLuongMua
        FROM SanPham sp
        INNER JOIN @Items item ON sp.MaSP = item.MaSP;

        -- 9. THỰC HIỆN TRỪ TIỀN VÍ KHÁCH HÀNG
        SET @SoDuSauGiaoDich = @SoDuHienTai - @TongTien;
        UPDATE KhachHang
        SET SoDuVi = @SoDuSauGiaoDich
        WHERE MaKH = @MaKH;

        -- 10. TẠO HÓA ĐƠN ĐƠN HÀNG
        INSERT INTO DonHang (MaKH, NgayTao, TongTien, TrangThai)
        VALUES (@MaKH, GETDATE(), @TongTien, 'PAID');
        SET @MaDH_Output = SCOPE_IDENTITY();

        -- 11. TẠO CHI TIẾT ĐƠN HÀNG
        INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia)
        SELECT @MaDH_Output, MaSP, SoLuongMua, GiaBan FROM @Items;

        -- 12. GHI SỔ CÁI KIỂM TOÁN GIAO DỊCH VÍ (AUDIT TRAIL ACID)
        INSERT INTO LichSuGiaoDichVi (MaKH, MaDH, LoaiGiaoDich, SoDuTruoc, SoTienThayDoi, SoDuSau, NgayGiaoDich, NoiDung)
        VALUES (
            @MaKH, 
            @MaDH_Output, 
            'THANH_TOAN', 
            @SoDuHienTai, 
            -(@TongTien), 
            @SoDuSauGiaoDich, 
            GETDATE(), 
            N'Thanh toán đơn hàng thực phẩm #' + CAST(@MaDH_Output AS NVARCHAR(20))
        );

        -- 13. XÓA CÁC MỤC TRONG GIỎ HÀNG SAU KHI MUA THÀNH CÔNG
        DELETE FROM ChiTietGioHang WHERE MaGioHang = @MaGioHang;

        -- 14. XÁC NHẬN GIAO DỊCH THÀNH CÔNG HOÀN TOÀN
        COMMIT TRANSACTION;

        SET @StatusCode = 1;
        SET @Message = N'Thanh toán đơn hàng thành công qua ví điện tử.';
    END TRY
    BEGIN CATCH
        IF (XACT_STATE() <> 0)
        BEGIN
            ROLLBACK TRANSACTION;
        END
        SET @MaDH_Output = 0;
        SET @StatusCode = -99;
        SET @Message = N'Lỗi giao dịch hệ thống: ' + ERROR_MESSAGE();
    END CATCH
END;
```

---

## 6. PHÂN HỆ BẪY SỰ KIỆN TỰ ĐỘNG & CẢNH BÁO (TRIGGERS & ALERTING)

### 6.1. `REQ-TRG-01`: Bẫy biến động giá bất thường, Cháy hàng và Hàng cận Date (`AFTER UPDATE`)
* **Loại trigger:** `AFTER UPDATE` trên bảng `SanPham`.
* **Kịch bản bẫy sự kiện 1 (Cảnh báo Cháy hàng):**
  * Điều kiện: Tồn kho cũ còn hàng (`d.SoLuongTon > 0`) nhưng sau khi cập nhật thì hết hàng (`i.SoLuongTon <= 0`).
  * Hành động:
    * Ghi ngay một bản ghi cảnh báo `CANH_BAO_HET_HANG` vào `NhatKyCanhBao` để Quản trị kho nhập hàng khẩn cấp.
    * Ghi các bản ghi vào `ThongBaoKhachHang` cho những khách hàng đang có sản phẩm này trong giỏ hàng để họ nắm thông tin.
* **Kịch bản bẫy sự kiện 2 (Biến động giá bất thường - Giá sốc/Sai sót nhập liệu):**
  * Điều kiện: Giá bán mới giảm sâu hơn 50% so với giá cũ (`i.Gia < d.Gia * 0.5`) hoặc tăng vọt hơn 200% (`i.Gia > d.Gia * 2.0`).
  * Hành động: Ghi cảnh báo `CANH_BAO_GIA_BAT_THUONG` vào `NhatKyCanhBao` kèm chi tiết giá cũ, giá mới và người thực thi (`SUSER_SNAME()`) để đối soát lỗi gian lận hoặc nhập liệu sai của nhân viên.
* **Kịch bản bẫy sự kiện 3 (Cảnh báo thực phẩm tươi cận Date):**
  * Điều kiện: `DATEDIFF(day, GETDATE(), i.HanSuDung) <= 1` và `i.SoLuongTon > 0`.
  * Hành động: Ghi cảnh báo `CANH_BAO_CAN_DATE` vào `NhatKyCanhBao` để đề xuất kích hoạt chương trình Flash Sale xả hàng tươi trong ngày.

```sql
CREATE OR ALTER TRIGGER trg_SanPham_CanhBaoBienDong
ON SanPham
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- 1. Bẫy cháy hàng (SoLuongTon giảm về 0)
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'CANH_BAO_HET_HANG',
        N'Sản phẩm [' + i.TenSP + N'] (Mã: ' + CAST(i.MaSP AS NVARCHAR(10)) + N') vừa hết hàng trong kho. Cần nhập hàng mới.',
        i.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    WHERE d.SoLuongTon > 0 AND i.SoLuongTon <= 0;

    -- Đẩy thông báo outbox cho khách hàng đang giữ sản phẩm này trong giỏ
    INSERT INTO ThongBaoKhachHang (MaKH, TieuDe, NoiDung, TrangThaiGui, NgayTao)
    SELECT DISTINCT
        gh.MaKH,
        N'Sản phẩm trong giỏ hàng đã hết hàng',
        N'Mặt hàng [' + i.TenSP + N'] trong giỏ hàng của bạn hiện đã tạm hết hàng.',
        'PENDING',
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    INNER JOIN ChiTietGioHang ctgh ON ctgh.MaSP = i.MaSP
    INNER JOIN GioHang gh ON ctgh.MaGioHang = gh.MaGioHang
    WHERE d.SoLuongTon > 0 AND i.SoLuongTon <= 0;

    -- 2. Bẫy biến động giá bất thường (Giảm > 50% hoặc Tăng > 200%)
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'CANH_BAO_GIA_BAT_THUONG',
        N'Cảnh báo giá bất thường sản phẩm [' + i.TenSP + N']: Giá cũ = ' + 
        CAST(d.Gia AS NVARCHAR(20)) + N' VND, Giá mới = ' + CAST(i.Gia AS NVARCHAR(20)) + N' VND.',
        i.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    WHERE (i.Gia < d.Gia * 0.5) OR (i.Gia > d.Gia * 2.0);

    -- 3. Bẫy hàng cận Date (HSD <= 1 ngày)
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'CANH_BAO_CAN_DATE',
        N'Thực phẩm tươi [' + i.TenSP + N'] sắp hết hạn (HSD: ' + 
        CONVERT(NVARCHAR(30), i.HanSuDung, 120) + N'). Đề xuất giảm giá xả kho trong ngày.',
        i.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    WHERE DATEDIFF(day, GETDATE(), i.HanSuDung) <= 1 AND i.SoLuongTon > 0;
END;
```

### 6.2. `REQ-TRG-02`: Ngăn chặn xóa vật lý - Thực thi Soft Delete (`INSTEAD OF DELETE`)
* **Loại trigger:** `INSTEAD OF DELETE` trên bảng `SanPham`.
* **Nghiệp vụ:**
  * Ngăn chặn hoàn toàn câu lệnh `DELETE FROM SanPham` gây mất dữ liệu vật lý và vi phạm ràng buộc toàn vẹn dữ liệu với các hóa đơn cũ (`ChiTietDonHang`).
  * Tự động chuyển đổi hành vi thành cập nhật cờ `TrangThaiXoa = 1`.
  * Ghi nhật ký truy vết hành động xóa vào bảng `NhatKyCanhBao`.

```sql
CREATE OR ALTER TRIGGER trg_SanPham_NganChanXoaVatLy
ON SanPham
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- 1. Chuyển đổi thành Soft Delete
    UPDATE sp
    SET sp.TrangThaiXoa = 1
    FROM SanPham sp
    INNER JOIN deleted d ON sp.MaSP = d.MaSP;

    -- 2. Ghi nhật ký truy vết hành động xóa mềm
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'XOA_MEM_SAN_PHAM',
        N'Sản phẩm [' + d.TenSP + N'] (Mã: ' + CAST(d.MaSP AS NVARCHAR(10)) + 
        N') đã được chuyển sang trạng thái Xóa mềm (TrangThaiXoa = 1).',
        d.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM deleted d;
END;
```

### 6.3. Kiến trúc Transactional Outbox gửi cảnh báo Real-time/Email qua Spring Boot
1. Triggers phía SQL Server chỉ thực hiện thao tác ghi nguyên tử vào 2 bảng `NhatKyCanhBao` và `ThongBaoKhachHang`.
2. Tầng Spring Boot sử dụng `@Scheduled(fixedDelay = 5000)` thông qua component `OutboxAlertDispatcher`:
   * Đọc các dòng có trạng thái `TrangThaiGui = 'PENDING'` từ bảng `ThongBaoKhachHang`.
   * Gửi thông báo tới người dùng qua WebSocket / Server-Sent Events (nếu đang online) hoặc gửi email.
   * Cập nhật trạng thái thành `SENT` hoặc `FAILED` nếu có lỗi mạng.

---

## 7. PHÂN HỆ ĐÁNH GIÁ HIỆU NĂNG CHỈ MỤC (INDEXING BENCHMARK 1.000.000 RECORDS)

### 7.1. `REQ-IDX-01`: Kịch bản sinh tập dữ liệu lớn bằng T-SQL CTE (1M bản ghi)
Hệ thống sử dụng kỹ thuật T-SQL Common Table Expression (CTE) kết hợp `CROSS JOIN` tạo cấp số nhân để sinh giả lập dữ liệu lớn với tốc độ cao mà không làm tràn bộ đệm RAM:

* **Tập dữ liệu tạo lập:**
  * Bảng `KhachHang`: 10.000 khách hàng mẫu.
  * Bảng `SanPham`: 1.000 mặt hàng thực phẩm tươi sống.
  * Bảng `DonHang`: **500.000 đơn hàng**.
  * Bảng `ChiTietDonHang`: **1.500.000 dòng chi tiết**.

```sql
-- Script sinh 500.000 dòng DonHang bằng CTE (Chạy trong vài giây)
;WITH 
  E1(N) AS (SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1), -- 10
  E2(N) AS (SELECT 1 FROM E1 a CROSS JOIN E1 b), -- 100
  E3(N) AS (SELECT 1 FROM E2 a CROSS JOIN E2 b), -- 10.000
  E4(N) AS (SELECT 1 FROM E3 a CROSS JOIN E1 b), -- 100.000
  E5(N) AS (SELECT 1 FROM E4 a CROSS JOIN E1 b), -- 1.000.000
  Numbers(N) AS (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) FROM E5)
INSERT INTO DonHang (MaKH, NgayTao, TongTien, TrangThai)
SELECT 
    ABS(CHECKSUM(NEWID())) % 10000 + 1 AS MaKH,
    DATEADD(MINUTE, - (N % 525600), GETDATE()) AS NgayTao, -- Trải dài trong 1 năm gần nhất
    ROUND(RAND(CHECKSUM(NEWID())) * (2000000 - 50000) + 50000, -3) AS TongTien,
    CASE N % 4 
        WHEN 0 THEN 'PAID'
        WHEN 1 THEN 'DELIVERED'
        WHEN 2 THEN 'PENDING'
        ELSE 'CANCELLED' 
    END AS TrangThai
FROM Numbers
WHERE N <= 500000;
```

### 7.2. `REQ-IDX-02`: Thiết kế câu truy vấn phân tích doanh số và tồn kho thực phẩm
Câu truy vấn đo kiểm (Benchmark Query) mô phỏng báo cáo tổng hợp doanh thu theo tháng và khách hàng VIP:
```sql
SELECT 
    dh.MaKH,
    kh.HoTen,
    COUNT(dh.MaDH) AS SoDonHang,
    SUM(dh.TongTien) AS TongChiTieu
FROM DonHang dh
INNER JOIN KhachHang kh ON dh.MaKH = kh.MaKH
WHERE dh.NgayTao >= '2026-01-01' 
  AND dh.NgayTao < '2026-10-01'
  AND dh.TrangThai IN ('PAID', 'DELIVERED')
GROUP BY dh.MaKH, kh.HoTen
HAVING SUM(dh.TongTien) > 5000000
ORDER BY TongChiTieu DESC;
```

### 7.3. `REQ-IDX-03`: Kịch bản so sánh Nonclustered Index + INCLUDE + Filtered Index

* **Kịch bản A (Chưa tối ưu chỉ mục):** Chỉ có khóa chính `PK_DonHang` (Clustered Index trên `MaDH`).
  * Thực thi câu truy vấn trên: CSDL bắt buộc duyệt tuần tự toàn bộ 500.000 dòng (`Clustered Index Scan`).
* **Kịch bản B (Đã tối ưu chỉ mục phủ Covering Index + INCLUDE):**
```sql
-- Tạo chỉ mục bao phủ có chứa thuộc tính truy vấn
CREATE NONCLUSTERED INDEX IX_DonHang_NgayTao_TrangThai_Covering
ON DonHang (NgayTao, TrangThai)
INCLUDE (MaKH, TongTien);
```
* **Kịch bản C (Chỉ mục lọc Filtered Index):** Chỉ lập chỉ mục cho các đơn hàng đã thanh toán thành công:
```sql
CREATE NONCLUSTERED INDEX IX_DonHang_Filtered_Paid
ON DonHang (NgayTao)
INCLUDE (MaKH, TongTien)
WHERE TrangThai IN ('PAID', 'DELIVERED');
```

### 7.4. Đo lường `STATISTICS IO`, `STATISTICS TIME` và Spring Boot Benchmark Runner
* Bật đo lường trên MS SQL Server:
```sql
SET STATISTICS IO ON;
SET STATISTICS TIME ON;
```
* Bảng đối chiếu chỉ số đo lường hiệu năng thực tế mục tiêu:
| Tiêu chí đo lường | Trước khi đánh chỉ mục (Kịch bản A) | Sau khi đánh chỉ mục (Kịch bản B/C) | Mức độ cải thiện |
| :--- | :--- | :--- | :--- |
| **Thao tác Execution Plan** | `Clustered Index Scan` | `Index Seek` | Chuyển từ quét tuần tự sang tìm kiếm nhị phân |
| **Logical Reads** (Đọc trang bộ nhớ) | ~15.800 reads | ~920 reads | **Giảm > 94%** |
| **CPU Time** | ~480 ms | ~35 ms | **Giảm > 92%** |
| **Elapsed Time** (Thời gian phản hồi) | ~720 ms | ~45 ms | **Nhanh hơn 16 lần** |

* **Spring Boot Benchmark Endpoint:**
  * Cung cấp REST Endpoint: `GET /api/v1/benchmark/orders-report?useIndex=true|false`
  * Trả về JSON:
```json
{
  "scenario": "COVERING_INDEX_ENABLED",
  "executionTimeMs": 42,
  "recordCount": 3840,
  "reductionPercentage": "94.2%"
}
```

---

## 8. PHÂN HỆ TÍCH HỢP TRÍ TUỆ NHÂN TẠO & CHATBOT ẨM THỰC (AI, MCP & ML FORECASTING)

### 8.1. `REQ-AI-01`: Tìm kiếm ngữ nghĩa sản phẩm tươi sống (Hybrid Semantic Vector Search & Qdrant)
* **Bài toán:** Khách hàng tìm kiếm theo nhu cầu sinh hoạt thay vì tên chính xác của thực phẩm:
  * Ví dụ: *"Nguyên liệu làm nước ép thanh lọc cơ thể giảm mỡ"* -> Kết quả hiển thị: Cần tây sạch, Táo xanh, Gừng tươi (mặc dù tên sản phẩm không chứa từ "nước ép thanh lọc").
* **Kiến trúc & Công nghệ triển khai:**
  * **Mô hình Embedding:** `all-MiniLM-L6-v2.onnx` (384 dimensions) chạy trực tiếp trên Java JVM bằng **Microsoft ONNX Runtime Java** (hoàn toàn Offline, độ trễ <10ms, không phụ thuộc API key bên ngoài).
  * **Cơ sở dữ liệu Vector:** **Qdrant Vector DB** chạy trên Docker (Port 6333), sử dụng chỉ mục đồ thị HNSW (Hierarchical Navigable Small World) và độ đo khoảng cách Cosine (Cosine Similarity).
  * **Chiến lược tìm kiếm:** **Hybrid Search Fusion** kết hợp giữa điểm số tương đồng ngữ nghĩa Vector và truy vấn khớp từ khóa SQL Server.
* **Quy trình xử lý:**
  1. Người dùng gửi câu hỏi tìm kiếm đến `SemanticSearchController` (`GET /api/v1/search/semantic`).
  2. `EmbeddingService` chuẩn hóa tiếng Việt, tách từ WordPiece và suy luận ra vector 384 chiều (L2 normalized).
  3. `QdrantService` gửi truy vấn tìm kiếm tới collection `fresh_grocery_products` trong Qdrant để lấy top sản phẩm có Cosine score cao nhất.
  4. Lấy thông tin chi tiết thực phẩm từ MS SQL Server qua `ProductRepository`.
  5. Kết hợp bổ sung các sản phẩm khớp từ khóa SQL Server (Hybrid Search), loại bỏ trùng lặp và sắp xếp theo điểm tương đồng giảm dần.
  6. Cơ chế tự động đồng bộ: Khi khởi động ứng dụng (`SemanticSearchInitializer`) và khi Thêm/Sửa/Xóa sản phẩm qua `ProductService`.

### 8.2. `REQ-AI-02`: Dự báo và tự động phân loại danh mục sản phẩm tươi
* Khi Quản trị viên nhập thực phẩm mới vào hệ thống nhưng chưa chọn danh mục (hoặc không chắc chắn về điều kiện lưu trữ):
* Spring AI phân tích chuỗi văn bản tên và mô tả sản phẩm:
  * Tự động dự đoán `MaDanhMuc` tương ứng (VD: Nhập "Mực ống cơm Phú Quốc làm sạch" -> Dự đoán danh mục: `Thủy hải sản tươi`).
  * Gợi ý `NhietDoBaoQuan` (VD: "Đông lạnh -18°C") và `HanSuDung` dự kiến (VD: 3 ngày).

### 8.3. `REQ-AI-03`: Chatbot tư vấn thực đơn, công thức món ăn & bỏ vào giỏ hàng tự động
Đây là tính năng trải nghiệm người dùng cao cấp, biến nền tảng bách hóa thành một chuyên gia tư vấn nội trợ thông minh.

#### Kịch bản tương tác người dùng:
1. **Khách hàng nhắn tin:** *"Tối nay nhà tôi có 4 người, tôi muốn nấu món canh chua cá lóc miền Tây. Cửa hàng có đủ nguyên liệu không?"*
2. **AI Chatbot xử lý:**
   * Kích hoạt Tool `tool_search_recipe`: Tìm món "Canh chua cá lóc" trong bảng `CongThucMonAn`.
   * Kích hoạt Tool `tool_check_fresh_inventory`: Truy vấn bảng `NguyenLieuMonAn` nối với `SanPham` để kiểm tra tồn kho:
     * Cá lóc tươi làm sạch: Còn tồn 15 con (Giá: 75.000 đ/con).
     * Bạc hà (dọc mùng): Còn tồn 30 cây.
     * Cà chua, thơm (dứa), đậu bắp, giá đỗ, me chua: Đều sẵn sàng trong kho.
   * Chatbot phản hồi: *"Dạ bách hóa có đầy đủ 6 nguyên liệu cho món Canh chua cá lóc 4 người ăn với tổng chi phí ước tính là 128.000 VND. Bạn có muốn bỏ tất cả nguyên liệu này vào giỏ hàng không?"*
3. **Khách hàng xác nhận:** *"Bỏ vào giỏ hàng và dẫn tôi đi thanh toán luôn nhé."*
4. **Chatbot thực thi:**
   * Kích hoạt Tool `tool_add_ingredients_to_cart`: Gọi Stored Procedure `sp_ThemNguyenLieuMonAnVaoGioHang` thêm toàn bộ các món vào giỏ hàng của khách.
   * Trả về thông báo thành công kèm đường dẫn liên kết điều hướng sang `/checkout` với danh sách món đã sẵn sàng.
   * Khách hàng bấm thanh toán, Stored Procedure giao dịch `sp_ThanhToanDonHang` (mục 5) trừ tiền ví, trừ kho và hoàn tất đơn hàng tức thì!

### 8.4. `REQ-AI-04`: Giao thức Model Context Protocol (MCP) Server trong Spring Boot
Spring Boot cấu hình chuẩn Model Context Protocol (MCP) Server (SSE Transport / JSON-RPC) để công khai 4 công cụ (Tools) chuẩn cho AI Model:

```java
// Khai báo công cụ chuẩn MCP trong Spring Boot
@Component
public class FreshGroceryMcpTools {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private SimpleJdbcCall spAddToCartCall;

    @Tool(name = "search_recipe_and_groceries", 
          description = "Tìm kiếm công thức món ăn và kiểm tra sự sẵn có của các nguyên liệu tươi sống trong kho bách hóa.")
    public RecipeDetailDto searchRecipeAndGroceries(
        @ToolParam(description = "Tên món ăn cần nấu") String recipeName,
        @ToolParam(description = "Số lượng người ăn") int servings) {
        return recipeService.findRecipeWithAvailableIngredients(recipeName, servings);
    }

    @Tool(name = "add_recipe_to_customer_cart", 
          description = "Thêm toàn bộ các nguyên liệu của món ăn vào giỏ hàng khách hàng.")
    public AddToCartResultDto addRecipeToCart(
        @ToolParam(description = "Mã khách hàng") Integer customerId,
        @ToolParam(description = "Mã món ăn") Integer recipeId,
        @ToolParam(description = "Số khẩu phần") Integer servings) {
        return recipeService.addRecipeIngredientsToCart(customerId, recipeId, servings);
    }

    @Tool(name = "create_checkout_redirect", 
          description = "Tạo phiên thanh toán đơn hàng và trả về URL chuyển tiếp đến trang checkout.")
    public CheckoutRedirectDto createCheckoutRedirect(
        @ToolParam(description = "Mã khách hàng") Integer customerId) {
        return new CheckoutRedirectDto(customerId, "/checkout?autoApply=true", "Sẵn sàng thanh toán");
    }
}
```

### 8.5. `REQ-AI-05`: Mô hình Machine Learning Dự báo Nhu cầu Tiêu thụ & Đề xuất Nhập hàng tươi sống (ONNX Runtime Java)
Đây là phân hệ giải quyết bài toán cốt tử của chuỗi bách hóa thực phẩm tươi sống: **Cân bằng giữa Nguy cơ Cháy hàng (Stockout) và Hao hụt Hư hỏng (Spoilage Loss)**.

#### 1. Nguồn dữ liệu & Tập huấn luyện (Data Source & Training Dataset):
* **Tập dữ liệu gốc:** Bộ dữ liệu siêu thị bán lẻ Đông Nam Á kinh điển trên Kaggle (**Supermarket Sales Dataset**) kết hợp dữ liệu giao dịch bán lẻ Việt Nam.
* **Quy trình chuẩn hóa:**
  * Ánh xạ các dòng hàng `Food and beverages` sang danh mục thực phẩm tươi sống Việt Nam (Cá lóc, Thịt heo, Rau muống, Cà chua, Trứng gà, Trái cây tươi).
  * Kết hợp với tập 1.500.000 dòng dữ liệu lịch sử bán hàng trong MS SQL Server (trích xuất qua `sp_LayLichSuBanHangHuanLuyen`).
* **Các thuộc tính đặc trưng (Features Engine):**
  1. `DayOfWeek` (1 đến 7: Phản ánh người tiêu dùng Việt Nam mua thực phẩm tươi tăng mạnh vào Thứ Bảy và Chủ Nhật).
  2. `IsWeekend` (0 hoặc 1).
  3. `Month` (1 đến 12: Tính mùa vụ rau củ, hải sản).
  4. `Lag_1`: Sản lượng bán thực tế ngày hôm qua (kg/hộp).
  5. `Lag_7`: Sản lượng bán cùng ngày này tuần trước.
  6. `Rolling_Mean_7`: Trung bình động số lượng tiêu thụ 7 ngày gần nhất.
  7. `GiaBan`: Giá niêm yết hiện tại.
  8. `MaDanhMuc`: Phân loại nhóm hàng.
  * **Biến mục tiêu (Target Label):** `SoLuongTieuThu` (Sản lượng khách sẽ mua vào ngày mai).

#### 2. Kiến trúc Mô hình & Định dạng ONNX:
* **Thuật toán huấn luyện:** **LightGBM Regressor** (hoặc Random Forest Regressor) được huấn luyện bằng Python trên tập dữ liệu chuẩn hóa, đạt chỉ số $R^2 > 0.88$ và $MAE < 3.2$ đơn vị sản phẩm.
* **Xuất mô hình chuẩn mở ONNX:** Mô hình được chuyển đổi sang định dạng `grocery_demand_forecast.onnx` bằng thư viện `onnxmltools` / `skl2onnx`.

#### 3. Triển khai suy luận trực tiếp trên Spring Boot (Zero-Python at Runtime):
Tầng Spring Boot sử dụng thư viện **Microsoft ONNX Runtime Java** (`com.microsoft.onnxruntime:onnxruntime:1.18.0`):
* Nạp file model `grocery_demand_forecast.onnx` từ thư mục `src/main/resources/models/` vào bộ nhớ JVM khi khởi động ứng dụng (`@PostConstruct`).
* Chạy suy luận (Inference) thuần Java trong vòng **dưới 5 mili-giây** cho mỗi lượt dự báo, không phụ thuộc vào bất kỳ môi trường Python hay API ngoài nào khi vận hành.

```java
// Cấu trúc Service suy luận ONNX trong Spring Boot
@Service
public class OnnxDemandForecastService {

    private OrtEnvironment env;
    private OrtSession session;

    @Autowired
    private SimpleJdbcCall spLuuKetQuaDuBao;

    @PostConstruct
    public void init() throws Exception {
        env = OrtEnvironment.getEnvironment();
        InputStream modelStream = getClass().getResourceAsStream("/models/grocery_demand_forecast.onnx");
        byte[] modelBytes = modelStream.readAllBytes();
        session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    public DemandForecastResultDto predictAndRecommendRestock(ProductForecastInput input) throws Exception {
        // 1. Tạo FloatBuffer chứa mảng Feature: [DayOfWeek, IsWeekend, Month, Lag_1, Lag_7, Rolling_Mean_7, GiaBan]
        float[] features = input.toFeatureArray();
        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), new long[]{1, features.length});

        // 2. Chạy suy luận mô hình
        OrtSession.Result result = session.run(Collections.singletonMap("float_input", tensor));
        float predictedDemand = ((float[][]) result.get(0).getValue())[0][0];

        // 3. Tính lượng nhập đề xuất (kèm buffer an toàn 10%)
        int currentStock = input.getCurrentStock();
        int recommendedRestock = Math.max(0, (int) Math.ceil(predictedDemand * 1.1) - currentStock);

        // 4. Đánh giá rủi ro hư hỏng (Spoilage Risk)
        String spoilageRisk = "THAP";
        if (currentStock > predictedDemand * 1.5 && input.getDaysUntilExpiry() <= 2) {
            spoilageRisk = "CAO";
        } else if (currentStock > predictedDemand) {
            spoilageRisk = "TRUNG_BINH";
        }

        // 5. Lưu kết quả vào MS SQL Server qua Stored Procedure
        spLuuKetQuaDuBao.execute(
            input.getProductId(),
            LocalDate.now().plusDays(1),
            predictedDemand,
            currentStock,
            recommendedRestock,
            93.5, // Độ tin cậy mô hình
            "LightGBM-ONNX",
            spoilageRisk
        );

        return new DemandForecastResultDto(input.getProductId(), predictedDemand, recommendedRestock, spoilageRisk);
    }
}
```

#### 4. REST Endpoints phục vụ Ban Quản trị Kho:
* `GET /api/v1/ai/forecast/restock-recommendations`: Trả về danh sách kế hoạch nhập hàng ngày mai cho toàn bộ các mặt hàng thực phẩm tươi sống.
* `POST /api/v1/ai/forecast/run-batch`: Kích hoạt chạy lại toàn bộ mô hình dự báo và đồng bộ bảng `DuBaoNhapHang`.

---

## 9. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS - NFR)

| Mã yêu cầu | Hạng mục | Tiêu chí kỹ thuật bắt buộc |
| :--- | :--- | :--- |
| **NFR-01** | **Hiệu năng truy vấn (Performance)** | Giảm thiểu ít nhất **90% Logical Reads** và thời gian phản hồi dưới **100ms** khi áp dụng Covering Index trên bảng 500.000 dòng. |
| **NFR-02** | **Tính toàn vẹn giao dịch (ACID)** | 100% thao tác thanh toán - trừ kho - trừ ví được bao bọc trong Stored Procedure Transaction; không bao giờ xảy ra tình trạng âm tiền ví hoặc âm kho. |
| **NFR-03** | **Khả năng kiểm toán (Auditability)** | 100% biến động số dư ví ghi nhận vào `LichSuGiaoDichVi`; 100% sự kiện sửa giá bất thường, hết hàng và xóa mềm ghi vào `NhatKyCanhBao`. |
| **NFR-04** | **Bảo mật tham số (Security)** | 100% câu lệnh T-SQL được tham số hóa thông qua Stored Procedure và JDBC Parameter; nghiêm cấm ghép chuỗi Dynamic SQL để ngăn chặn triệt để SQL Injection. |
| **NFR-05** | **Khả năng chịu lỗi (Resilience)** | Thiết lập `SET XACT_ABORT ON` để ngắt tự động toàn bộ giao dịch nếu rớt kết nối mạng giữa Spring Boot và MS SQL Server. |
| **NFR-06** | **Tương thích giao thức AI (Interoperability)**| Hỗ trợ chuẩn Model Context Protocol (MCP) giúp hệ thống kết nối linh hoạt với mọi mô hình LLM tiên tiến nhất. |
| **NFR-07** | **Tốc độ suy luận ML (Inference Latency)**| Mô hình ONNX chạy trực tiếp trên Java JVM đạt độ trễ suy luận **dưới 10ms** cho mỗi sản phẩm dự báo. |

---

## 10. TIÊU CHÍ NGHIỆM THU ĐỒ ÁN (ACCEPTANCE CRITERIA)

| Hạng mục kiểm thử | Kịch bản kiểm thử | Kết quả mong đợi khi nghiệm thu |
| :--- | :--- | :--- |
| **1. Stored Procedures (CRUD)** | Gọi `sp_ThemSanPhamMoi` với giá âm (`-50.000`) hoặc HSD trong quá khứ. Gọi với dữ liệu hợp lệ. | - Dữ liệu sai: Trả về mã lỗi âm qua `@StatusCode` và thông báo lỗi qua `@Message`, không có dòng nào được chèn.<br>- Dữ liệu đúng: Trả về `@StatusCode = 1` và `@NewMaSP > 0`. |
| **2. Concurrency & Transaction** | Chạy đồng thời 2 luồng thanh toán cùng mua sản phẩm tươi cuối cùng trong kho khi số dư ví vừa đủ. | Chỉ có đúng 1 giao dịch thành công (`StatusCode = 1`), giao dịch còn lại nhận mã lỗi `-3` (hết hàng) và tự động Rollback, không có hiện tượng âm kho hay trừ tiền 2 lần. |
| **3. Soft-Delete Trigger** | Chạy lệnh `DELETE FROM SanPham WHERE MaSP = 1;` | Dòng dữ liệu không bị xóa khỏi bảng; `TrangThaiXoa` chuyển thành 1; Bảng `NhatKyCanhBao` sinh một bản ghi truy vết loại `XOA_MEM_SAN_PHAM`. |
| **4. Alerting Trigger** | Cập nhật giá sản phẩm từ 100.000 đ xuống còn 20.000 đ (giảm 80%). | Bảng `NhatKyCanhBao` tự động sinh bản ghi `CANH_BAO_GIA_BAT_THUONG` kèm giá cũ và giá mới. |
| **5. Index Benchmark** | Chạy truy vấn báo cáo trên tập 500.000 dòng có bật `STATISTICS IO, TIME` trước và sau khi đánh Nonclustered Covering Index. | Ảnh chụp màn hình `Execution Plan` chuyển từ `Clustered Index Scan` sang `Index Seek`; `Logical Reads` giảm trên 90%. |
| **6. AI Chatbot & MCP** | Nhập câu hỏi: *"Tôi muốn nấu canh chua cá lóc cho 4 người"*, sau đó yêu cầu *"bỏ vào giỏ hàng và thanh toán"*. | Chatbot gợi ý đúng các nguyên liệu thực phẩm tươi từ CSDL, tự động gọi SP thêm vào giỏ hàng và chuyển hướng sang trang `/checkout` với đúng các món đó. |
| **7. AI ML Forecasting (ONNX)** | Gọi Endpoint `GET /api/v1/ai/forecast/restock-recommendations` trên Spring Boot. | Mô hình LightGBM-ONNX thực thi suy luận trên JVM, hiển thị số lượng tiêu thụ dự báo ngày mai, số lượng khuyến nghị nhập kho tương ứng, và lưu trữ bản ghi vào bảng `DuBaoNhapHang` trong MS SQL Server. |
| **8. Khuyến mãi & Flash Sale (3NF)** | Tạo chương trình khuyến mãi, thêm sản phẩm với giá KM < giá gốc và thử nghiệm tự động xả kho cận date. | Stored Procedure thực thi thành công; Trigger `trg_ChiTietKhuyenMai_KiemSoatGia` tự động tính % giảm và ngăn chặn giá bất hợp lý; Giá bán hiển thị trên Giỏ hàng, Đơn hàng và Chatbot được chiết khấu chính xác theo thuật toán `MIN(GiaKhuyenMai)`. |

---

## 11. PHÂN HỆ QUẢN LÝ CHƯƠNG TRÌNH KHUYẾN MÃI & FLASH SALE (3NF RDBMS)

### 11.1. Mô hình dữ liệu chuẩn 3NF (Bình thường hóa dữ liệu)
Thay vì lưu tạm bợ trường `GiaKhuyenMai` trực tiếp vào bảng `SanPham` (gây trùng lặp dữ liệu, không hỗ trợ nhiều chiến dịch khác nhau theo thời gian, vi phạm tính toàn vẹn 3NF), hệ thống chuẩn hóa theo chuẩn 3NF hoàn chỉnh:
* **Bảng `ChuongTrinhKhuyenMai` (Bảng 14):**
  * `MaKM` (`INT IDENTITY(1,1) PRIMARY KEY`): Mã chương trình.
  * `TenKM` (`NVARCHAR(150) NOT NULL`): Tên chiến dịch (VD: Flash Sale Thủy Hải Sản, Xả kho cận Date...).
  * `MoTa` (`NVARCHAR(500)`): Mô tả chi tiết chiến dịch.
  * `LoaiKM` (`VARCHAR(30) NOT NULL`): `FLASH_SALE`, `XA_KHO_CAN_DATE`, `GIAM_THEO_MUA`, `TRI_AN_KHACH_HANG`.
  * `NgayBatDau`, `NgayKetThuc` (`DATETIME2 NOT NULL`): Khoảng thời gian áp dụng.
  * `TrangThai` (`BIT DEFAULT 1`): Cờ kích hoạt chiến dịch.
* **Bảng `ChiTietKhuyenMai` (Bảng 15 - Quan hệ Many-to-Many):**
  * Khóa chính kết hợp: `PRIMARY KEY (MaKM, MaSP)`.
  * `GiaKhuyenMai` (`DECIMAL(18,2) NOT NULL`): Mức giá ưu đãi sau giảm.
  * `PhanTramGiam` (`DECIMAL(5,2)`): Tỷ lệ chiết khấu so với giá gốc.
  * `SoLuongKhuyenMai` (`INT`): Giới hạn số lượng suất ưu đãi mở bán (NULL nếu không giới hạn).
  * `SoLuongDaBan` (`INT DEFAULT 0`): Theo dõi số lượng khách đã mua để tự động dừng khuyến mãi khi hết suất.
* **Mở rộng Bảng `ChiTietDonHang`:**
  * Bổ sung các cột: `GiaGoc`, `TienGiam`, `MaKM` để lưu vết lịch sử chiết khấu minh bạch trên hóa đơn.

### 11.2. Ràng buộc toàn vẹn & Trigger bảo vệ nghiệp vụ (`trg_ChiTietKhuyenMai_KiemSoatGia`)
* Nghiệp vụ kinh doanh bắt buộc: Giá khuyến mãi phải luôn nhỏ hơn giá gốc của thực phẩm (`GiaKhuyenMai < GiaGoc`) và lớn hơn 0 VND.
* Trigger tự động tính toán chính xác tỷ lệ chiết khấu:
  $$\text{PhanTramGiam} = \text{ROUND}\left(\frac{\text{GiaGoc} - \text{GiaKhuyenMai}}{\text{GiaGoc}} \times 100, 2\right)$$
* Nếu vi phạm (ví dụ giá khuyến mãi cao hơn hoặc bằng giá gốc), trigger tự động ném lỗi và hủy giao dịch (`ROLLBACK TRANSACTION`).

### 11.3. Thuật toán giải quyết xung đột giá (`MIN(GiaKhuyenMai)`)
Khi một mặt hàng đồng thời nằm trong nhiều chương trình khuyến mãi đang có hiệu lực (ví dụ vừa nằm trong Flash Sale vừa nằm trong Lễ hội nông sản), Stored Procedure (`sp_LayDanhSachSanPhamPhanTrang`, `sp_LayChiTietSanPham`, `sp_LayGioHangKhachHang`) áp dụng kỹ thuật `OUTER APPLY` kết hợp `TOP 1 ... ORDER BY ctkm.GiaKhuyenMai ASC` để tự động chọn mức giá có lợi nhất cho người tiêu dùng.

### 11.4. Tính năng AI & Trigger Hook: Tự động gom hàng cận date xả kho (`sp_TuDongKichHoatXaKhoCanDate`)
* Khi mô hình Machine Learning ONNX hoặc Trigger `trg_SanPham_CanhBaoBienDong` phát hiện các mặt hàng thực phẩm tươi sống có nguy cơ hư hỏng cao hoặc HSD $\le 2$ ngày:
* Thủ tục `sp_TuDongKichHoatXaKhoCanDate` được tự động kích hoạt:
  1. Tự động khởi tạo chiến dịch `XA_KHO_CAN_DATE` trong ngày.
  2. Quét toàn bộ thực phẩm cận hạn sử dụng chưa hết hàng.
  3. Áp dụng mức giảm giá đề xuất (30% - 50%) và thêm vào bảng `ChiTietKhuyenMai`.
  4. Trả về số lượng mặt hàng đã được giải cứu xả kho thành công.

