package com.ctu.freshgrocery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI freshGroceryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hệ thống Bách Hóa Thực Phẩm Tươi Sống (Fresh Grocery API)")
                        .description("""
                                **Đồ án môn học:** CT221H - Nguyên lý Hệ quản trị Cơ sở Dữ liệu
                                
                                **Tính năng Module 1:**
                                - Quản lý Sản phẩm tươi sống & Danh mục qua Stored Procedures (`sp_ThemSanPhamMoi`, `sp_CapNhatSanPham`, `sp_LayDanhSachSanPhamPhanTrang`)
                                - Tham số kiểm soát đầu vào, đầu ra `OUTPUT` (`@StatusCode`, `@Message`, `@NewMaSP`)
                                - Bẫy sự kiện Trigger: Cảnh báo giá bất thường, Cảnh báo hết hàng, Bẫy xóa mềm (Soft-Delete)
                                
                                **Tính năng Module 2 (Giao dịch ACID & Ví điện tử):**
                                - Quản lý Giỏ hàng qua Stored Procedures (`sp_ThemVaoGioHang`, `sp_XoaKhoiGioHang`, `sp_LayGioHangKhachHang`)
                                - Nạp tiền ví & Sổ cái kiểm toán tài chính Audit Trail (`sp_NapTienVi`, `LichSuGiaoDichVi`)
                                - Giao dịch thanh toán đơn hàng ACID (`sp_ThanhToanDonHang` với `SET XACT_ABORT ON`, `UPDLOCK`, `ROWLOCK`)
                                - **1-Click Concurrency Demo:** Thử nghiệm tranh chấp đồng thời 2 luồng đối chứng ngăn chặn Overselling và Lost Update
                                
                                **Tính năng Module 3 (AI Học Máy Dự báo Nhu cầu & Nhập kho):**
                                - Nạp mô hình PyTorch-MLP-ONNX trực tiếp vào JVM bộ nhớ thông qua **Microsoft ONNX Runtime Java** (`<5ms/lượt`)
                                - Dự báo nhu cầu khách mua ngày mai theo đặc trưng chuỗi thời gian (Ngày trong tuần, cuối tuần, mùa vụ, Lag_1, Lag_7, Rolling statistics)
                                - Thuật toán đề xuất lượng nhập kho an toàn (Buffer 10%) và đánh giá nguy cơ hư hỏng / ế hàng quá date (`THAP`, `TRUNG_BINH`, `CAO`)
                                - Tự động đồng bộ kết quả vào CSDL (`DuBaoNhapHang`) qua Stored Procedure và kích hoạt cảnh báo Quản trị viên (`NhatKyCanhBao`)
                                
                                **Tính năng Module 4 (Chatbot AI Ẩm Thực & Model Context Protocol - MCP):**
                                - Tư vấn công thức món ăn đặc sản gia đình (Canh chua cá lóc, Thịt kho tàu trứng gà...) và kiểm tra tồn kho nguyên liệu
                                - Tự động thêm trọn bộ nguyên liệu vào giỏ hàng qua Stored Procedure `sp_ThemNguyenLieuMonAnVaoGioHang`
                                - Cung cấp 4 công cụ chuẩn hóa **Model Context Protocol (MCP Server)** sẵn sàng kết nối các LLM Agent (Claude, Cursor, Gemini, DeepSeek)
                                
                                **Tính năng Module 5 (Đo Kiểm Hiệu Năng Chỉ Mục 1M Dòng & Index Optimization):**
                                - Sinh dữ liệu lớn mẫu (50.000 đến 1.000.000 dòng) bằng đệ quy T-SQL CTE siêu tốc
                                - So sánh trực quan **Table Scan (Clustered Index Scan)** vs **B-Tree Index Seek (Covering Index + INCLUDE)**
                                - Báo cáo tỷ lệ cải thiện thời gian phản hồi I/O và hệ số tăng tốc độ truy vấn
                                
                                **Tính năng Module 6 (Tìm kiếm Ngữ nghĩa AI - Semantic Vector Search & Qdrant):**
                                - Mô hình nhúng văn bản **all-MiniLM-L6-v2 ONNX** chạy trực tiếp trên Java JVM (<10ms, 100% Offline, độc lập không cần API key)
                                - Cơ sở dữ liệu Vector chuyên dụng **Qdrant Vector DB** (Chỉ mục HNSW, Cosine Similarity, Web UI Dashboard tại `http://localhost:6333/dashboard`)
                                - Tìm kiếm kết hợp **Hybrid Search Fusion** (Ưu tiên Vector Ngữ nghĩa + Bổ sung Từ khóa SQL Server)
                                - Tự động đồng bộ thời gian thực khi Thêm/Sửa/Xóa sản phẩm và tích hợp trực tiếp vào **MCP Server Tool & Chatbot Ẩm Thực**
                                
                                **Tính năng Module 7 (Chương trình Khuyến mãi, Flash Sale & Tự động Xả kho 3NF RDBMS):**
                                - Thiết kế CSDL Chuẩn 3NF hoàn chỉnh: `ChuongTrinhKhuyenMai` và `ChiTietKhuyenMai` liên kết sản phẩm đa - đa.
                                - Lưu vết tài chính & chiết khấu minh bạch trên hóa đơn: Bổ sung `GiaGoc`, `TienGiam`, `MaKM` vào `ChiTietDonHang`.
                                - Thuật toán giải quyết xung đột ưu đãi: Tự động chọn `MIN(GiaKhuyenMai)` khi sản phẩm thuộc nhiều chiến dịch còn hiệu lực.
                                - Ràng buộc & Trigger bảo vệ kinh doanh: `trg_ChiTietKhuyenMai_KiemSoatGia` bắt buộc `GiaKhuyenMai < GiaGoc`, tự động tính `% Giảm`.
                                - Tự động hóa thông minh (AI & Trigger hook): `sp_TuDongKichHoatXaKhoCanDate` tự động gom thực phẩm cận date (<= 2 ngày) đưa vào chiến dịch xả kho giảm 30-50%.
                                - Tích hợp toàn diện: Tự động phản ánh giá khuyến mãi trên API Phân trang sản phẩm, Giỏ hàng, Đơn hàng, Semantic Search và Chatbot AI.
                                """)
                        .version("7.0.0")
                        .contact(new Contact()
                                .name("Nhóm Đồ Án CT221H - ĐH Cần Thơ")
                                .email("ct221h@ctu.edu.vn"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Local Development Server")
                ));
    }
}
