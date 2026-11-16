# Rà soát độ tin cậy báo cáo DailyMart — 15/11/2026

Phạm vi: đối chiếu nội dung báo cáo, sơ đồ và phụ lục với SQL, Java, notebook, metadata mô hình và kết quả kiểm thử đã lưu trong repository. Không sửa ứng dụng hoặc database; không chạy lại giao dịch thanh toán, huấn luyện mô hình hay kiểm thử ngân hàng.

## Các điểm đã sửa

| Nội dung | Phát hiện và xử lý | Nguồn đối chiếu |
| --- | --- | --- |
| Số lượng bảng | Có đúng 15 bảng trong script gốc. Phụ lục hiện trình bày 18 bảng trong phạm vi báo cáo, không gọi đó là tổng bảng của database đang chạy. Bỏ tích hợp thanh toán cũ khỏi nội dung và nguồn sinh phụ lục. | database/01_init_schema.sql; customer/CustomerAuthController.java; resources/db/sepay.sql |
| Ví | Không có bảng Vi riêng. Thêm SoDuVi vào sơ đồ KhachHang; ghi rõ MaDH của lịch sử ví cho phép NULL. | database/01_init_schema.sql |
| Quan hệ dữ liệu | Làm rõ bản ghi cha có thể chưa có bản ghi con; đổi nhãn 1–N thành 1–0..N trong các sơ đồ ERD. | FK và UNIQUE trong schema |
| Xóa mềm sản phẩm cha | Báo cáo cũ mô tả xóa cả loại con. Sửa thành xóa mềm bản ghi cha, gỡ chỉ mục các loại liên quan, giữ cờ xóa riêng của loại con. | ProductServiceImpl.deleteProduct; sp_XoaSanPham; trigger INSTEAD OF DELETE |
| Chỉ mục bao phủ | Danh sách INCLUDE bị thiếu NguonDon; đã bổ sung. | database/05_indexing_benchmark.sql |
| Cảnh báo hạn dùng | Không phải cửa sổ 24 giờ. DATEDIFF(day, GETDATE(), HanSuDung) <= 1 tính theo mốc ngày và bao gồm cả hàng đã hết hạn còn tồn. | database/03_triggers.sql |
| Tìm kiếm | Mô tả cũ giới hạn 50 thứ hạng; code hiện giới hạn 80 và ưu tiên thêm nguyên liệu công thức. Đã cập nhật văn bản/sơ đồ, bổ sung lọc hàng test, hiệu chỉnh lỗi gõ và quy tắc ẩm thực. | SemanticSearchServiceImpl; VietnameseFoodSemantics |
| Chất lượng tìm kiếm | Tỷ lệ trong JSON khớp bảng báo cáo, nhưng thuộc chính sách tại lúc đo. Đã ghi rõ không dùng chúng làm phép đánh giá mới cho tất cả thay đổi mã hiện tại. | backend/evaluation/hybrid-main-report.json |
| Kiểm thử SQL thanh toán | Báo cáo cũ ghi 8 ca bị bỏ qua. Artifact hiện lưu 10 ca đạt, không lỗi, không bỏ qua; đã cập nhật và vẫn phân biệt với ngân hàng thật. | backend/target/surefire-reports/TEST-com.ctu.freshgrocery.payment.SePayDatabaseTest.xml |
| Tổng ca kiểm thử | Bỏ tổng 35 ca của tập cũ; chỉ trích số ca của từng suite liên quan có kết quả lưu, tránh ghép phạm vi hoặc đếm trùng. | Surefire XML/txt |

## Các số liệu có nguồn lưu nhưng cần hiểu đúng

- Benchmark: CSV lưu mức đọc logic 13.942 xuống 1.401 và thời gian trung vị 195 xuống 103 ms. Đây là phép đo đã lưu, không phải lần đo mới; mức giảm 89,95% chưa đạt tiêu chí ít nhất 90%.
- Dự báo: metadata lưu MAE 3,2161, RMSE 4,3157, R² 0,9741, WAPE 5,34%, 860 mẫu kiểm tra và danh mục 12 sản phẩm. Đây là dữ liệu mô phỏng; chưa xác minh lại toàn bộ quá trình tạo artifact bằng một lượt huấn luyện mới.
- Kết quả QA 74/74 và frontend 22/22 có trong tài liệu QA nội bộ. Không nâng mức chứng cứ của tài liệu tự ghi nhận thành log thực thi độc lập hoặc lượt kiểm thử mới.
- View trong phụ lục là đề xuất chưa tích hợp khởi tạo; báo cáo vẫn ghi rõ trạng thái đó.
- Chưa có bằng chứng ngân hàng thật/webhook công khai trong phạm vi tài liệu đã kiểm tra. Không suy từ unit test, SQL integration hoặc ảnh API giả lập sang kết luận đã vận hành thanh toán thật.

## Giới hạn kết luận

Đã tìm thấy và sửa những mô tả sai hoặc lỗi thời nêu trên. Việc số liệu khớp tệp lưu chỉ xác nhận báo cáo trích đúng tệp; không chứng minh mọi tệp kết quả đều được tạo bằng một quy trình độc lập và có thể tái lập. Đợt rà soát này không đưa ra cam kết toàn bộ hệ thống đã chạy đúng trong vận hành thực.
