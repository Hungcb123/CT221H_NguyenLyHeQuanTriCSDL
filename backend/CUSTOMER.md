# Khách hàng và lịch sử đơn hàng

Trang `/login` dùng email và mật khẩu, tạo phiên khách hàng ở server. Menu tên khách hàng có **Đơn hàng của tôi**, **Ví DailyMart** và **Đăng xuất**. `/orders` có phân trang, `/orders/{id}` có chi tiết sản phẩm, tổng tiền, giao hàng và trạng thái. Lịch sử chỉ lấy đơn có nguồn `SALE`.

Trang `/wallet` và liên kết trong bước thanh toán hiển thị số dư cùng lịch sử giao dịch của chính khách hàng. Quản trị viên bổ sung số dư demo từ mục **Khách hàng**. Giỏ hàng cho phép tăng, giảm hoặc xóa từng món. Khi đổi số lượng, backend kiểm tra hàng và trả lại giá, suất ưu đãi và tổng tiền hiện tại.

Trang `/register` tạo tài khoản mới bằng họ tên, email, số điện thoại và mật khẩu (từ 8 ký tự); địa chỉ không bắt buộc. Email và số điện thoại không được trùng. Tạo hồ sơ, mật khẩu BCrypt và giỏ hàng rỗng trong một transaction, số dư ví ban đầu bằng 0, sau đó tự đăng nhập. Trang đăng nhập có liên kết **Đăng ký ngay**.

`python backend/evaluation/verify_registration.py` kiểm tra toàn bộ luồng đăng ký bằng một tài khoản tạm và dọn riêng tài khoản kiểm tra sau khi chạy.

Tài khoản demo được tạo một lần cho ba email seed, mật khẩu `DailyMart123!`:

- `nguyenvanan@ctu.edu.vn`
- `tranthibinh@ctu.edu.vn`
- `levancuong@ctu.edu.vn`

Mật khẩu lưu BCrypt trong `KhachHangDangNhap`; khởi động lại không ghi đè mật khẩu đã có. Phiên khách hàng độc lập với phiên quản trị. Giỏ, ví, đơn hàng và thao tác mua của chatbot yêu cầu đúng chủ tài khoản; thao tác thay đổi dữ liệu yêu cầu CSRF token.

## Công thức theo tồn kho

Chatbot tra tên công thức trong `CongThucMonAn`, không định tuyến theo tên món hoặc ID viết cố định trong Java. `10_available_recipes.sql` bổ sung dữ liệu công thức mẫu cho các sản phẩm hiện có.

Mỗi lần tra cứu và thêm giỏ, dịch vụ đọc lại kho; chọn SKU còn hạn, đủ tồn, cùng nhóm nguyên liệu và cùng đơn vị bán. Nếu nguyên liệu không có lựa chọn phù hợp, trợ lý đưa ra các công thức khác hiện đủ hàng. Đây là tra cứu công thức có trong database, chưa phải mô hình sinh công thức tùy ý.

`python backend/evaluation/verify_customer_flows.py` kiểm tra đăng nhập, CSRF, phân quyền khách hàng, đăng xuất, món khả dụng và thêm cả món vào giỏ; khôi phục nội dung giỏ sau khi kiểm tra.
