# Kiểm thử quản trị DailyMart trước buổi demo

Ngày thực hiện: 14/11/2026. Môi trường: giao diện Next.js tại localhost:3000, Spring Boot trong Docker tại localhost:8085, SQL Server FreshGroceryDB và Qdrant đang chạy.

Báo cáo này ghi nhận các tình huống đã chạy, không khẳng định toàn bộ hệ thống không còn lỗi. Kiểm thử thanh toán không thực hiện chuyển tiền ngân hàng hoặc giả mạo webhook thành công.

## Phạm vi và kết quả

| Mục quản trị | Tình huống đã kiểm tra | Kết quả |
|---|---|---|
| Đăng nhập | Chặn truy cập chưa đăng nhập, sai mật khẩu, đăng nhập/đăng xuất, xác thực và CSRF API | Đạt |
| Tổng quan | Tải số liệu, phân biệt doanh thu thật và đơn benchmark/AI demo | Đạt |
| Sản phẩm | Tạo nhiều quy cách, sửa giá/tồn kho giữ mã quy cách, tải ảnh và lấy ảnh | Đạt |
| Xóa mềm | Ẩn/khôi phục cả nhóm; giữ trạng thái ẩn riêng của từng quy cách; chặn mua nhóm đã ẩn hoặc nhóm có mọi quy cách bị ẩn | Đã sửa và kiểm tra hồi quy |
| Danh mục | Tạo/sửa/xóa danh mục trống; chặn xóa khi còn sản phẩm, kể cả đã xóa mềm | Đạt; sửa form giữ mã danh mục vừa xóa |
| Đơn hàng COD | Chi tiết, lọc trạng thái, giao hàng, hủy và hủy lặp; không hoàn ví COD; hoàn tồn/suất ưu đãi một lần; chặn đổi ngược trạng thái cuối | Đạt |
| SePay/MoMo | Chặn giao đơn chưa thanh toán; chặn hủy/hoàn tiền ngoài luồng đối soát; kiểm tra hàng bị ẩn ở bước tạo thanh toán | Đạt trong phạm vi kiểm thử, không xác minh giao dịch ngân hàng thật |
| Khách hàng | Danh sách, tìm kiếm không có kết quả, phân nhóm, xem lịch sử ví cũ | Đạt; không thay đổi số dư |
| Cảnh báo | Lọc sự kiện, lọc mã sản phẩm, tải lại và nhật ký đổi trạng thái đơn | Đạt |
| AI và dự báo | Trạng thái ONNX/embedding/Qdrant, hybrid search, lịch sử, suy luận giả lập, hai nguồn doanh thu và kỳ 1/7/30 ngày; đầu vào không hợp lệ | Đạt |
| Hiệu năng | Tạo 1.000 đơn BENCHMARK, so sánh hai truy vấn và đối chiếu tổng nhóm | Đạt; hai truy vấn cùng 3 nhóm kết quả |
| Khuyến mãi | Tạo, sửa giờ địa phương, thêm ưu đãi theo quy cách bán, tạm dừng/bật lại, hiển thị trang chủ và giá trong hộp mua hàng | Đạt trên giao diện desktop và 390px; giá 800đ khớp API/hộp mua hàng |
| Xả kho | Loại hàng hết hạn/hết hàng/đã ẩn, loại mã cha có quy cách, chặn phần trăm sai, giảm món 500đ còn 350đ thay vì 0đ | Đạt kiểm thử SQL |

## Các lỗi đã sửa

1. Trang chủ chưa có nơi công bố khuyến mãi. Thêm mục ưu đãi lấy giá thực từ thủ tục đang dùng, lọc thời hạn, trạng thái, tồn kho, hạn dùng và suất còn lại. Tải lại mỗi 30 giây/khi quay lại cửa sổ.
2. Trang quản trị chỉ báo “Đã bật” dù chương trình đã hết hạn hoặc chưa có sản phẩm. Hiện trạng thái thời gian rõ ràng, số sản phẩm và hướng dẫn hoàn tất ưu đãi.
3. Chọn mã sản phẩm cha cho khuyến mãi không giảm được giá quy cách mua. Thêm bộ chọn sản phẩm/quy cách và từ chối mã cha có quy cách; không tự áp giá của cha cho con.
4. Giờ khuyến mãi SQL UTC bị hiển thị/nhập không nhất quán. Chuyển đổi giờ địa phương khi nhập/sửa và hiển thị, giữ thời điểm UTC ở API.
5. Xóa mềm cả nhóm làm mất trạng thái hiển thị của quy cách khi khôi phục. Giữ trạng thái riêng, kiểm tra trạng thái cha trong các đường mua hàng.
6. Nhóm có tất cả quy cách bị ẩn bị xem như sản phẩm lẻ. Chặn việc quay về bán mã cha ở giỏ hàng, COD và các cổng thanh toán.
7. Xóa danh mục đang sửa nhưng form còn giữ mã cũ. Xóa thành công đưa form về chế độ tạo.
8. Giao diện đề nghị hủy đơn cổng thanh toán dù backend luôn từ chối. Ẩn thao tác không hỗ trợ và ghi hướng dẫn đối soát.
9. Quét xả kho lấy cả hàng hết hạn, có thể chọn lại chương trình đã hết hiệu lực và làm tròn giá món rẻ xuống 0. Sửa điều kiện, thời hạn và độ chính xác giá.
10. Ưu đãi cũ gắn vào mã cha vẫn hiện nhãn giảm giá dù các quy cách không giảm. Xóa thông tin ưu đãi khỏi DTO mã cha có quy cách ở danh sách thường và tìm kiếm thông minh, giữ nguyên giá các mã mua thực tế.
11. Mô tả Swagger benchmark khẳng định loại execution plan/I/O chưa đo. Sửa mô tả theo đúng phép đo thời gian thực hiện hiện có.

## Dữ liệu demo và giới hạn

- Thêm đúng 1.000 đơn có nguồn BENCHMARK. Sau tạo, doanh thu và bản ghi đơn bán thật, bảy đơn AI demo và tồn kho đã đối chiếu giữ nguyên. Một lần đo: truy vấn quét 46 ms, truy vấn có điều kiện chỉ mục 7 ms; đây là một lần đo, không phải kết quả hiệu năng tổng quát hoặc bằng chứng execution plan.
- Flash Sale hải sản cũ có thời gian kết thúc ngày 23/10/2026 và không có sản phẩm, nên không được tự gia hạn hoặc tự gắn giảm giá.
- Các đơn kiểm thử mang tên QA được giữ lịch sử. Sản phẩm kiểm thử quản trị được xóa mềm sau kiểm tra. Ba gói thử thanh toán 500đ/1.000đ của người dùng vẫn giữ lại.
- Không khôi phục hàng loạt các quy cách đã bị ẩn từ trước, vì không thể biết chúng từng bị ẩn riêng hay bị ẩn theo nhóm. Bản sửa bảo toàn trạng thái cho các thao tác tiếp theo.
- Chưa kiểm tra chuyển khoản thật và webhook ngân hàng trong đợt này. Đơn cổng thanh toán chỉ chuyển thành công khi nhận xác nhận hợp lệ.

## Kịch bản demo khuyến mãi đã chuẩn bị

Chương trình `[DEMO] Flash Sale thử hệ thống` áp dụng cho `[TEST] Gói thử thanh toán 1.000đ`, giá ưu đãi 800đ, 20 suất, hiệu lực bảy ngày kể từ lúc tạo. Đã kiểm tra tạo/sửa/hiển thị, tạm dừng làm biến mất khỏi API công khai và bật lại làm xuất hiện. Không thay đổi giá sản phẩm thông thường. Với SePay, tổng đơn phải đạt tối thiểu 1.000đ; có thể chọn hai gói ưu đãi thành 1.600đ.

Thứ tự demo: vào Khuyến mãi xem trạng thái/sản phẩm → mở trang chủ xem Flash Sale → mở hộp mua kiểm tra giá → về quản trị tạm dừng rồi mở lại trang chủ → bật lại. Chương trình hải sản cũ đã hết hạn, cần sửa thời gian và gắn quy cách cụ thể nếu muốn dùng nó.

## Kiểm chứng trên bản đang chạy

- Build production Next.js thành công; Docker backend đã build và khởi động với các bản sửa.
- 27 kiểm thử frontend đạt. 122 kiểm thử backend được chọn đạt, không bỏ qua trường hợp nào, bao gồm quyền admin, đổi trạng thái, hoàn tiền/tồn kho, COD, cổng thanh toán, quy cách, khuyến mãi, xả kho, tìm kiếm và dự báo.
- Lượt smoke sau triển khai đã mở đủ chín trang: Tổng quan, Sản phẩm, Danh mục, Đơn hàng, Khuyến mãi, Khách hàng, Cảnh báo, AI và dự báo, Hiệu năng. Không ghi nhận lỗi JavaScript. Thao tác Quét và áp dụng xả kho hiển thị số lượng thực.
- Đã dọn các sản phẩm và danh mục QA thuộc đợt kiểm tra khỏi phần khách hàng nhìn thấy.

Ảnh chụp và log kiểm tra được lưu tại `/tmp/admin-*`, báo cáo JUnit tại `backend/target/surefire-reports/`. Các script trình duyệt là tệp kiểm tra tạm trong `/tmp`; báo cáo này lưu lại kết quả và phạm vi để dùng khi demo.

- Kiểm tra cuối dữ liệu tìm kiếm: 62/62 trường hợp keyword/hybrid, 38 nhóm và 92 quy cách đạt. Tìm “Dứa chín gọt sẵn” hiển thị đúng giá gốc 22.000đ, không còn ưu đãi cha không áp dụng được.
- Kiểm tra kết nối công khai sau cập nhật Docker: cấu hình SePay qua ngrok HTTP 200; gọi webhook không xác thực HTTP 401, không cập nhật giao dịch.
