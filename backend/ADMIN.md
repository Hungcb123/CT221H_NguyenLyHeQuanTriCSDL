# Quản trị cửa hàng

Mở `http://localhost:3000/admin`. Frontend chuyển tiếp API sang backend tại cổng 8085; SQL Server và Qdrant phải đang chạy.

## Tài khoản

Backend dùng phiên đăng nhập Spring Security, role `ADMIN`, BCrypt và CSRF. Lần khởi động đầu, nếu chưa đặt `ADMIN_PASSWORD`, backend tạo tài khoản local:

- Tên mặc định: `admin`.
- Mật khẩu khởi tạo: `backend/.local/admin-initial-password.txt` khi chạy Maven từ thư mục `backend`.
- Hash lưu trong `backend/.local/admin.properties`. Thư mục này bị loại khỏi Git. Đừng commit hoặc chia sẻ các file này.

Có thể đặt `ADMIN_USERNAME` và `ADMIN_PASSWORD` (ít nhất 12 ký tự) trước khi chạy backend để dùng tài khoản do mình quản lý. `ADMIN_CREDENTIALS_DIR` đổi thư mục lưu tài khoản local. Khi triển khai HTTPS, đặt `ADMIN_COOKIE_SECURE=true`. Phiên hết hạn sau 30 phút không hoạt động; đăng xuất hủy phiên. Đây là một tài khoản quản trị vận hành; chưa có giao diện quản lý nhiều tài khoản/đổi mật khẩu.

## Các chức năng

| Trang | Chức năng |
|---|---|
| `/admin` | Tổng quan tồn kho, hạn sử dụng, doanh thu và cảnh báo thật |
| `/admin/products` | Tạo, sửa, ẩn, khôi phục, tìm và lọc sản phẩm |
| `/admin/categories` | Tạo, sửa, xóa danh mục trống; không xóa danh mục còn sản phẩm ẩn |
| `/admin/orders` | Hóa đơn, lọc trạng thái, giao hàng, hủy đơn; hủy PAID hoàn ví, kho và suất KM trong cùng transaction |
| `/admin/promotions` | Tạo/sửa, bật/tạm dừng, quản lý mặt hàng/giá/suất, xả kho cận hạn |
| `/admin/customers` | Tra cứu thông tin khách, số dư và 100 giao dịch ví gần nhất |
| `/admin/alerts` | Nhật ký trigger và audit; lọc loại sự kiện hoặc mã sản phẩm |
| `/admin/ai` | Trạng thái embedding/Qdrant/forecast, đồng bộ vector, thử hybrid search, chạy và xem lịch sử dự báo |
| `/admin/benchmark` | Đo truy vấn thật, sinh dữ liệu và tạo/gỡ chỉ mục có xác nhận |

Admin không gọi nhánh mock trong `apiService`; mọi yêu cầu qua `admin-api.ts`, lỗi hiển thị để xử lý. API admin và API ghi catalog/khuyến mãi, đồng bộ vector, dự báo, benchmark được bảo vệ tại backend. Các luồng khách hàng có sẵn vẫn giữ cơ chế định danh hiện hữu; phân quyền tài khoản khách hàng không nằm trong thay đổi này.

## AI và tính trung thực của dữ liệu

- Embedding hiện tại: `all-MiniLM-L6-v2` ONNX, 384 chiều. Hybrid search dùng BM25 + vector/RRF; khi embedding/Qdrant không khả dụng, trả `KEYWORD_FALLBACK` trên catalog SQL. Không tìm thấy mặt hàng được yêu cầu thì kết quả chính trống, thay thế tách riêng.
- Đồng bộ vector upsert sản phẩm đang bán. Sửa/xóa sản phẩm cũng cập nhật/xóa vector qua dịch vụ sẵn có. Đổi tên danh mục nên chạy đồng bộ lại để nội dung embedding phản ánh tên mới. Qdrant có thể còn vector cũ nếu lần xóa trước bị lỗi; tìm kiếm vẫn đối chiếu sản phẩm đang bán trong SQL.
- Dự báo dùng `PyTorch-MLP-ONNX`, scaler đã cài, và doanh số PAID/DELIVERED trong 7 ngày trước ngày dự báo. Ngày không có doanh số được điền 0, không bịa lịch sử. Ngày hiện tại có thể chưa đủ doanh số khi dự báo cho ngày mai. Chưa xác nhận chất lượng dự báo bằng dữ liệu bán hàng thực tế đủ dài.
- Không có phần trăm tin cậy đã hiệu chỉnh: `doTinCay=null`. Khi khởi động, migration cho phép cột `DuBaoNhapHang.DoTinCay` nhận NULL (tài khoản DB cần quyền ALTER cho lần nâng cấp). Lịch sử cũ vẫn được giữ; API không diễn giải giá trị cũ thành độ tin cậy hợp lệ.
- Lịch sử lưu không có độ trễ inference nên API trả NULL thay vì giả 0ms. Batch forecast chạy trong transaction, lỗi sẽ rollback cả batch.
- Không có upload, huấn luyện hoặc đổi model trong giao diện đợt này.

## Benchmark

Sinh dữ liệu ghi thêm vào bảng `DonHang` thật; không tự hoàn tác. Dashboard tách đơn không có chi tiết khỏi thống kê bán hàng. Đây là quy ước phân biệt dataset hiện hữu, chưa phải một cờ dataset độc lập. Phép so sánh đảm bảo chỉ mục tồn tại, đo một lượt wall-clock; kết quả có thể cho chỉ mục chậm hơn. Không suy ra Scan/Seek hoặc I/O chỉ từ thời gian: kiểm tra execution plan và `STATISTICS IO` riêng.

## Kiểm thử

Từ `backend`: `mvn test`. Kiểm thử SQL Server hoàn ví/rollback được bật riêng:

```bash
RUN_DB_INTEGRATION_TESTS=true mvn -Dtest=AdminRefundDatabaseTest test
```

Các fixture chạy trong transaction được rollback sau kiểm thử (identity có thể tăng). Từ `frontend`: `npx tsc --noEmit`, `npm run lint`, `node --test tests/*.test.cjs`.
