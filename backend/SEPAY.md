# Thanh toán SePay (VietQR + webhook)

Checkout khách hàng dùng COD (thanh toán khi nhận hàng) hoặc SePay chuyển khoản ngân hàng.
Đơn MoMo cũ và API cũ được giữ để xem lịch sử/đối soát.

## Chuẩn bị

- Tài khoản SePay đã liên kết tài khoản ngân hàng nhận tiền.
- Mã ngân hàng (ví dụ `MBBank`), số tài khoản, tên chủ tài khoản.
- Backend có URL HTTPS công khai để SePay gọi webhook. Khi chạy local có thể dùng tunnel vào port 8085.
- Khóa webhook do bạn tạo (`openssl rand -hex 32`), tối thiểu 32 ký tự.
  Đây là khóa xác thực webhook; luồng này không cần API Token truy vấn ngân hàng hay khóa đối tác MoMo.

Copy `.env.sepay.example` thành `.env.sepay.local`, điền cấu hình và bật `SEPAY_ENABLED=true`.
Không đưa khóa vào frontend hay commit file local.

```bash
docker compose --env-file .env.sepay.local up -d --build
```

Frontend chạy riêng bằng `npm run dev` trong thư mục `frontend`.

Nếu chạy Maven trực tiếp, đưa cùng các biến môi trường vào process backend.
Schema `db/sepay.sql` tự chạy khi backend khởi động; giữ nguyên dữ liệu MoMo.
Chưa cấu hình thì checkout hiển thị SePay chưa khả dụng.

## Ngrok chạy cùng Docker

Điền thêm `NGROK_AUTHTOKEN` vào `.env.sepay.local` (lấy từ ngrok dashboard).
Đây là token tài khoản ngrok, khác với `SEPAY_WEBHOOK_API_KEY`.
Lệnh Compose phía trên khởi động cả service `ngrok`, chuyển tiếp HTTPS công khai tới `backend:8085`.
Không cần chạy thêm `ngrok http 8085` trong terminal. Dừng ngrok terminal cũ trước khi chuyển sang Docker để tránh trùng endpoint/cổng 4040.

`ngrok http` tự dùng dev domain cố định của tài khoản, nên restart container vẫn giữ URL khi dùng cùng tài khoản.
Xem URL và lỗi kết nối bằng:

```bash
docker compose --env-file .env.sepay.local logs --tail=30 ngrok
```

Giao diện kiểm tra request chỉ mở trên máy local: http://localhost:4040.
Sau khi đã tạo container, có thể khởi động lại bằng `docker compose start` (không cần nạp lại env).
Docker tự khởi động lại ngrok theo `restart: unless-stopped`; Docker Desktop và mạng vẫn phải hoạt động.
Nếu chủ động `docker compose stop` hoặc `down`, cần bật lại bằng `start` hoặc lệnh `up` phía trên.
Đóng terminal không làm dừng container khi dùng `-d`.

Tham khảo chính thức: [Docker](https://ngrok.com/docs/using-ngrok-with/docker),
[dev domain cố định](https://ngrok.com/docs/gateway/domains).

## Cấu hình trên SePay

Tạo webhook cho **Có tiền vào**, chọn đúng tài khoản nhận tiền:

- URL: `https://YOUR_PUBLIC_BACKEND/api/v1/payments/sepay/webhook`.
- Định dạng dữ liệu: JSON.
- Xác thực: API Key, giá trị giống `SEPAY_WEBHOOK_API_KEY`.
- SePay gửi header `Authorization: Apikey <khóa>`.
- Bật gửi lại khi server trả mã ngoài 200–299.
- Có thể không đặt bộ lọc mã; backend đọc mã `DM` + 32 ký tự hex trong `code`/`content`.

Endpoint trả HTTP 201 và `{"success":true}` khi đã xử lý hoặc đã ghi nhận giao dịch không khớp.
Tham khảo: [webhook SePay](https://docs.sepay.vn/tich-hop-webhooks.html),
[QR và luồng chuyển khoản](https://sepay.vn/lap-trinh-cong-thanh-toan.html).

## Quy tắc xử lý

- Giá, khuyến mãi, tồn kho được tính và khóa tại server; giữ hàng trong 30 phút.
- Khởi tạo lại cùng idempotency key hoặc khi còn đơn chờ trả về thanh toán cũ, tránh trừ kho hai lần.
- QR chứa tài khoản, số tiền và nội dung riêng cho thanh toán; thông tin nhận tiền lưu theo từng đơn.
- Chỉ giao dịch tiền vào, đúng tài khoản, đúng một mã và đúng số tiền mới xác nhận đơn.
- `SePayWebhook.TransactionId` là khóa chính để chống xử lý webhook trùng; payload và kết quả xử lý được lưu cùng transaction cập nhật đơn.
- Sai tiền/tài khoản hoặc chuyển lần hai được lưu để đối soát, không tự xác nhận hay cộng ví.
- Scheduler hết hạn giữ hàng sau 30 phút, trả tồn kho và lượt khuyến mãi một lần. Đơn hết hạn chuyển `CANCELLED`, thanh toán `FAILED`.
- Tiền đến muộn chuyển thanh toán `REVIEW`; đơn không tự giao. Cần đối soát và hoàn ngân hàng thủ công nếu phù hợp.
- Không có API hoàn tiền ngân hàng tự động. Admin không được hoàn đơn SePay vào ví nội bộ.
- Giới hạn số tiền của ứng dụng: 1.000–50.000.000 VND/đơn, nguyên VND.
- Nút kiểm tra thanh toán đọc trạng thái server đã nhận qua webhook. Khi webhook thất bại, kiểm tra nhật ký SePay và gửi lại; chưa có truy vấn API ngân hàng dự phòng.

## Kiểm tra

Chạy test unit bảo mật và đơn hàng:

```bash
mvn -f backend/pom.xml -Dtest=SePaySecurityTest,AdminOrderServiceTest,AdminSecurityTest test
```

Test SQL Server có rollback dữ liệu kiểm thử:

```bash
RUN_DB_INTEGRATION_TESTS=true mvn -f backend/pom.xml -Dtest=SePayDatabaseTest test
```

Gửi thử webhook ở SePay; kiểm tra giao dịch hợp lệ, sai tiền, sai tài khoản, gửi trùng, tiền đến sau hết hạn.
Dùng tài khoản và webhook Test Mode của SePay cho thử nghiệm; chỉ dùng cấu hình Live khi chủ động nhận tiền thật.

## Xóa mềm sản phẩm

Admin → Sản phẩm → **Xóa mềm** → xác nhận. Sản phẩm và các loại của nó được đánh dấu `TrangThaiXoa=1`,
bỏ khỏi cửa hàng/chỉ mục tìm kiếm và giữ lịch sử đơn hàng. Chọn bộ lọc **Đã xóa mềm** để **Khôi phục**.
Backend dùng thủ tục `sp_XoaSanPham` và trigger `INSTEAD OF DELETE`; không xóa bản ghi vật lý.
