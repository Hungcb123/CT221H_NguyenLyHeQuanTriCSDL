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
| `/admin/customers` | Tra cứu khách, lọc nhóm, xem 100 giao dịch ví gần nhất và nạp số dư demo có xác nhận |
| `/admin/alerts` | Nhật ký trigger và audit; lọc loại sự kiện hoặc mã sản phẩm |
| `/admin/ai` | Trạng thái mô hình, đồng bộ vector, thử hybrid search, dự báo nhu cầu, xếp hạng sản phẩm bán chạy và dự báo doanh thu theo kỳ |
| `/admin/benchmark` | Đo truy vấn thật, sinh dữ liệu và tạo/gỡ chỉ mục có xác nhận |

Admin không gọi nhánh mock trong `apiService`; mọi yêu cầu qua `admin-api.ts`, lỗi hiển thị để xử lý. API admin và API ghi catalog/khuyến mãi, đồng bộ vector, dự báo, benchmark được bảo vệ tại backend. Các luồng khách hàng có sẵn vẫn giữ cơ chế định danh hiện hữu; phân quyền tài khoản khách hàng không nằm trong thay đổi này.

## AI và tính trung thực của dữ liệu

- SQL Server phân loại nguồn đơn bằng `DonHang.NguonDon`. Tổng quan và lịch sử khách dùng `SALE`. Đơn huấn luyện mẫu có nguồn `DEMO_FORECAST`, còn bài đo hiệu năng dùng `BENCHMARK`. Chạy các script khởi tạo theo thứ tự để nâng cấp bản cũ.
- Mục **Khách hàng → Xem ví** cho phép admin nạp số dư demo, có xác nhận số tiền và tên người nhận. Sổ cái ghi tài khoản admin. Gửi lại cùng mã yêu cầu cho cùng khách và cùng số tiền trả kết quả mà chỉ cộng một lần.
- Phân nhóm khách hàng dựa trên lần mua gần nhất, số đơn và chi tiêu. Chỉ tính đơn đã thanh toán hoặc đã giao có chi tiết, bỏ qua dữ liệu benchmark và lịch sử giả lập huấn luyện. Bốn nhóm gồm chưa mua, cần tương tác lại (lần cuối cách hơn 30 ngày), thân thiết (có mua trong 30 ngày, ít nhất 3 đơn và chi từ 500.000 đ trong 90 ngày) và đang hoạt động (các khách mới mua còn lại).
- Bảng dự báo bán chạy dùng kết quả mô hình cho ngày kế tiếp. Bảng xếp tối đa 10 sản phẩm trong cùng đơn vị tính. Giá trị nhu cầu dự kiến lấy số lượng dự báo nhân giá niêm yết, chưa áp khuyến mãi hoặc giới hạn tồn kho. Chạy dự báo toàn bộ để cập nhật bảng.
- **Dự báo doanh thu theo kỳ** dùng trung bình sản lượng theo thứ trong tối đa 56 ngày, với kỳ 1, 7 hoặc 30 ngày. Nguồn đơn bán và nguồn mô phỏng được chọn riêng. Dự báo phân bổ tồn kho hiện tại qua các ngày trước hạn và áp suất khuyến mãi còn lại theo giá hiệu lực lúc 12 giờ.
- Kịch bản giả lập nhu cầu hiển thị kết quả trực tiếp. Chỉ các lần dự báo vận hành ghi lịch sử và cảnh báo kho.

- Embedding hiện tại: `all-MiniLM-L6-v2` ONNX, 384 chiều. Hybrid search dùng BM25 + vector/RRF; khi embedding/Qdrant không khả dụng, trả `KEYWORD_FALLBACK` trên catalog SQL. Không tìm thấy mặt hàng được yêu cầu thì kết quả chính trống, thay thế tách riêng.
- Đồng bộ vector upsert sản phẩm đang bán. Sửa/xóa sản phẩm cũng cập nhật/xóa vector qua dịch vụ sẵn có. Đổi tên danh mục nên chạy đồng bộ lại để nội dung embedding phản ánh tên mới. Qdrant có thể còn vector cũ nếu lần xóa trước bị lỗi; tìm kiếm vẫn đối chiếu sản phẩm đang bán trong SQL.
- Dự báo dùng `PyTorch-MLP-ONNX`, scaler đã cài, và doanh số PAID/DELIVERED trong 7 ngày trước ngày dự báo. Ngày không có doanh số được điền 0, không bịa lịch sử. Ngày hiện tại có thể chưa đủ doanh số khi dự báo cho ngày mai. Chưa xác nhận chất lượng dự báo bằng dữ liệu bán hàng thực tế đủ dài.
- Không có phần trăm tin cậy đã hiệu chỉnh: `doTinCay=null`. Khi khởi động, migration cho phép cột `DuBaoNhapHang.DoTinCay` nhận NULL (tài khoản DB cần quyền ALTER cho lần nâng cấp). Lịch sử cũ vẫn được giữ; API không diễn giải giá trị cũ thành độ tin cậy hợp lệ.
- Lịch sử lưu không có độ trễ inference nên API trả NULL thay vì giả 0ms. Batch forecast chạy trong transaction, lỗi sẽ rollback cả batch.
- Không có upload, huấn luyện hoặc đổi model trong giao diện đợt này.

## Benchmark

Sinh dữ liệu ghi đơn có nguồn `BENCHMARK` vào database hiện tại. Phép so sánh lọc nguồn benchmark và đo thời gian tại ứng dụng. Đối chiếu Scan/Seek và lượt đọc bằng kế hoạch thực thi và `STATISTICS IO` trên database benchmark riêng.

## Kiểm thử

Từ `backend`: `mvn test`. Kiểm thử SQL Server hoàn ví/rollback được bật riêng:

```bash
RUN_DB_INTEGRATION_TESTS=true mvn -Dtest=AdminRefundDatabaseTest test
```

Các fixture chạy trong transaction được rollback sau kiểm thử (identity có thể tăng). Từ `frontend`: `npx tsc --noEmit`, `npm run lint`, `node --test tests/*.test.cjs`.
