# Thanh toán MoMo Sandbox

Đã tích hợp API MoMo `captureWallet` v2 ở chế độ **sandbox duy nhất**. Không gọi cổng production và không sử dụng tiền thật. Chưa có khóa đối tác nên mặc định nút MoMo chưa được kích hoạt; giao diện vẫn dùng được ví DailyMart.

Số `0366643213` được lưu trong cấu hình `MOMO_RECEIVER_PHONE` làm thông tin liên hệ dự kiến của cửa hàng. **API MoMo xác định tài khoản nhận bằng Partner Code và bộ khóa MoMo Business, không bằng số điện thoại này.** Cấu hình số điện thoại không làm tiền chuyển về số đó.

## Bật sandbox

1. Lấy Partner Code, Access Key và Secret Key sandbox từ tài khoản MoMo Business của bạn, theo [hướng dẫn tích hợp](https://developers.momo.vn/v3/docs/payment/onboarding/integration-process/).
2. Sao chép `.env.momo.example` thành `.env.momo.local` ở thư mục gốc. File local này được Git bỏ qua. Điền bộ khóa sandbox và đổi `MOMO_ENABLED=true`.
3. Cấp URL HTTPS công khai cho backend qua hosting hoặc tunnel. `MOMO_IPN_URL` phải trỏ tới `https://<backend>/api/v1/payments/momo/ipn`; URL này không cần đăng nhập vì xác thực bằng HMAC. `localhost` không nhận được IPN từ máy chủ MoMo.
4. Đặt `MOMO_REDIRECT_URL` tới trang `/payments/momo/return` của frontend. Cho thử nghiệm ngay trên máy, có thể dùng `http://localhost:3000/payments/momo/return`.
5. Chạy `docker compose --env-file .env.momo.local up -d --build backend`.
6. Dùng **ứng dụng/tài khoản MoMo Test**, không dùng ví tiền thật. Xem [API thanh toán](https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/).

Không ghi khóa vào frontend, commit, log hoặc chat. Không bật production bằng cách sửa hostname trong cấu hình: bản này cố định cổng `https://test-payment.momo.vn`.

## Luồng xử lý

- Checkout gửi địa chỉ và idempotency key; giá, tổng tiền và số lượng luôn được tính lại ở server từ giỏ hàng/khuyến mãi, không nhận số tiền từ trình duyệt.
- Một transaction SQL khóa dữ liệu, tạo đơn `PENDING`/`MOMO_SANDBOX`, giữ kho và hạn mức khuyến mãi, lưu ảnh chụp chi tiết đơn rồi dọn giỏ hàng. Không trừ ví DailyMart.
- HTTP gọi MoMo chạy sau khi transaction tạo đơn đã commit. Đơn lưu mã thanh toán và request ID bền vững, giữ nguyên khi thử lại; một khách chỉ có một thanh toán MoMo đang chờ/đối soát.
- Tạo được `payUrl` chỉ có nghĩa là đã tạo liên kết, **chưa phải đã thanh toán**. Người dùng sang trang MoMo Test.
- IPN phải khớp chữ ký HMAC-SHA256, đối tác, mã đơn, request ID, nội dung và số tiền đã lưu. Chỉ kết quả `0` với transaction ID hợp lệ mới chuyển đơn thành `PAID`.
- Trang return bỏ qua `resultCode`, `amount` và chữ ký trong URL trình duyệt; chỉ dùng order ID để hỏi backend. Khi IPN chậm, người dùng có thể yêu cầu backend truy vấn MoMo. Tác vụ nền cũng đối soát các giao dịch đang chờ.
- Callback lặp không trừ kho hay thanh toán thêm. Mã lỗi kết thúc đã biết trả kho/hạn mức đúng một lần và đưa sản phẩm về giỏ. Mã đang xử lý/không rõ, mất mạng hoặc timeout giữ đơn chờ đối soát, không tự kết luận thất bại.
- Nếu nhận thành công muộn sau khi đơn đã kết thúc, chuyển thanh toán sang `REVIEW`, không tự giao hàng hoặc trừ kho lần nữa.

Mọi API khách hàng yêu cầu phiên đúng chủ tài khoản; API thay đổi dữ liệu yêu cầu CSRF, riêng IPN dùng chữ ký MoMo. Bảng `ThanhToanMoMo` và cột phương thức đơn hàng được tạo idempotent từ `src/main/resources/db/momo.sql` khi backend khởi động.

## Giới hạn hiện tại

- Chưa chạy end-to-end trên hệ thống MoMo thật vì chưa có khóa sandbox. Các kiểm tra tự động dùng dữ liệu test và gateway giả lập/mocked; không giả nhận tiền MoMo.
- Hoàn tiền MoMo chưa được triển khai. Admin không được hủy đơn MoMo bằng cơ chế hoàn vào ví DailyMart. Đơn chưa thanh toán sẽ được giải phóng khi MoMo trả trạng thái thất bại/hết hạn đã xác minh.
- Nếu provider không có giao dịch nhưng kết nối tạo đơn đã timeout, trạng thái có thể tiếp tục chờ đối soát. Cần kiểm tra MoMo Business; không giải phóng kho chỉ dựa trên đồng hồ phía ứng dụng hoặc cho trả tiền lại.
- Nhãn **MoMo Sandbox** xuất hiện trên checkout, trang kết quả và đơn hàng để phân biệt với giao dịch thật.

## Kiểm tra

`MoMoSecurityTest` kiểm tra chữ ký chuẩn, callback giả/sai thông tin, cấu hình thiếu, giới hạn link sandbox và chủ sở hữu.

`MoMoDatabaseTest` chạy trên SQL Server với `RUN_DB_INTEGRATION_TESTS=true`; fixtures nằm trong transaction rollback. Kiểm tra giữ kho/idempotency, callback trùng, trả kho/giỏ, sai số tiền, xác nhận muộn và không động đến ví.

Tài liệu chính thức: [chữ ký](https://developers.momo.vn/v3/docs/payment/api/other/signature/), [IPN](https://developers.momo.vn/v3/docs/payment/api/result-handling/notification/), [truy vấn trạng thái](https://developers.momo.vn/v3/docs/payment/api/payment-api/query/), [mã kết quả](https://developers.momo.vn/v3/docs/payment/api/result-handling/resultcode/).
