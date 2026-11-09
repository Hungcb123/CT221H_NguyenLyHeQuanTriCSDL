# Báo cáo kiểm thử và thay đổi DailyMart

**Mốc đối chiếu** Nhánh `dev_Can` tại commit gốc `843861c`. Cập nhật ngày 10/11/2026.

## Đã sửa

- **Thanh toán và khuyến mãi** — Sửa lỗi HTTP 500 khi thanh toán, số dư ví cũ trên màn hình xác nhận, số liệu tồn kho và số dư cũ trong bài thử tranh chấp. Chặn bán vượt suất khuyến mãi.
  - **Mã gốc** `02_stored_procedures.sql` thiếu thiết lập `QUOTED_IDENTIFIER ON` khi tạo thủ tục, khiến thao tác cập nhật bảng có chỉ mục lọc có thể lỗi. Bộ lọc ưu đãi chỉ kiểm tra còn suất. `OrderServiceImpl` đọc lại ví và kho qua các entity JPA đã nạp trước khi thủ tục SQL cập nhật.
- **Đăng nhập và quản trị** — Chặn điều hướng sang trang ngoài sau khi đăng nhập hoặc đăng ký. Đăng xuất admin giữ phiên khách hàng trong cùng trình duyệt. Ô **DailyMart** ở thanh bên mở cửa hàng trong cùng tab. Bỏ nút **Mở cửa hàng** trùng lặp trên thanh trên cùng và căn **Đăng xuất** về bên phải.
  - **Mã gốc** Trang đăng nhập và đăng ký chỉ kiểm tra tiền tố của `next`. `AdminSecurityConfig` dùng `invalidateHttpSession(true)` và xóa `JSESSIONID`. `AdminShell.tsx` đặt hai thao tác ở chân thanh bên.
- **Sản phẩm và API** — Chi tiết sản phẩm đã ẩn và URL API không tồn tại trả HTTP 404.
  - **Mã gốc** `ProductServiceImpl.getProductById` thiếu bộ lọc `TrangThaiXoa`. `GlobalExceptionHandler` đưa `NoResourceFoundException` vào bộ xử lý chung trả HTTP 500.
- **Khởi tạo hệ thống** — Backend đợi SQL Server khởi tạo dữ liệu thành công. Giữ định dạng LF cho script Bash và dùng cổng `1434` trên Windows để tránh trùng SQL Server Express.
  - **Mã gốc** `docker-compose.yml` chỉ đợi `sqlserver` khỏe và gắn cổng máy `1433`. `init_db.sh` thiếu `set -e` và cờ `-b` ở một số lệnh SQL. Repo chưa có quy tắc giữ LF cho script.
- **Chạy thử và kiểm thử** — Tải đủ 60 ảnh mẫu cho bản chạy cục bộ, sửa bộ test frontend và làm ổn định các ca kiểm thử nguyên liệu.
  - **Mã gốc** `download_catalog_images.py` tải ảnh vào thư mục `backend/.local` nằm ngoài Git. Các test frontend thiếu biến và module cần thiết khi chạy trong VM. `verify_recipe_cart.sql` và `verify_customer_flows.py` giả định nguyên liệu mẫu đã hết hạn hoặc thiếu hàng.
- **Dự báo nhu cầu** — Tính ngày dự báo mặc định và số ngày còn hạn theo giờ Việt Nam. Dùng cùng một ngày cho toàn bộ đợt dự báo và chặn giá trị âm ở API giả lập trước khi chạy mô hình.
  - **Mã gốc** `DemandForecastServiceImpl` lấy ngày bằng `LocalDate.now()` theo múi giờ JVM. Các trường số tùy chỉnh của `ForecastCustomRequest` thiếu ràng buộc không âm.
- **Ảnh giỏ hàng** — Thủ tục `sp_LayGioHangKhachHang` và API trả ảnh theo từng sản phẩm. Giỏ hàng và trang thanh toán dùng cùng ảnh với danh mục. Ảnh thiếu hoặc lỗi tải hiện biểu tượng trung tính.
  - **Mã gốc** `sp_LayGioHangKhachHang` và `CartItemResponse` thiếu trường ảnh. `CartDrawer.tsx` dùng cùng một ảnh rau trộn khi thiếu `hinhAnh`.
- **Chữ trên cửa hàng** — Dùng phông sans-serif hỗ trợ tiếng Việt cho tiêu đề và nội dung. Tăng cỡ chữ ở thông báo tìm kiếm, nút điều hướng, thẻ sản phẩm, giỏ hàng và phần giới thiệu. Đã thử tìm và xóa tìm kiếm trên Chrome ở **320 và 1.440 px**. Bản dựng frontend đạt.
  - **Mã gốc** `globals.css` trỏ `font-sans` tới biến `--font-geist-sans` chưa được khai báo. `page.tsx` dùng `font-serif` cho tiêu đề và `text-xs` cho thông báo tìm kiếm.

## Đã bổ sung

- **Phân nhóm khách hàng** — Thêm bốn nhóm và bộ lọc trên trang admin, dựa trên lần mua gần nhất, số đơn và chi tiêu. Loại đơn hủy, benchmark và lịch sử giả lập khỏi phép tính.
- **Dự báo bán chạy** — Xếp hạng tối đa 10 sản phẩm cho ngày kế tiếp trong từng đơn vị tính, kèm giá trị nhu cầu ước tính theo giá niêm yết.
- **Thủ tục lưu và trigger** — Thêm `sp_XoaSanPham` để thao tác xóa đi qua thủ tục lưu và vẫn kích hoạt xóa mềm. Nhật ký cập nhật ghi dữ liệu trước và sau, có bộ lọc trên trang admin.
- **Giao diện đăng nhập** — Dùng bố cục chung thích ứng với màn hình, cho phép chuyển giữa **Khách hàng** và **Quản trị** trong cùng một tab. Giữ nội dung biểu mẫu và trang đích sau khi đăng nhập.
- **Nút điều hướng** — Đồng bộ nút quay lại cửa hàng, tất cả đơn hàng, cập nhật trạng thái, xem chi tiết và phân trang. Nút có viền hoặc nền xanh, biểu tượng cho thao tác chính và vùng bấm tối thiểu 44 px.
- **Ô chọn quản trị** — Đồng bộ kiểu ô chọn và menu với góc bo, dấu tích cho mục đang chọn và vùng cuộn khi danh sách dài. Việt hóa nhãn lọc đơn hàng, loại cảnh báo và loại khuyến mãi. Chrome ở **360 và 1.280 px** hiển thị đúng menu. Đã kiểm tra chọn bằng chuột, bàn phím và lọc theo mã API tương ứng. Bản dựng frontend đạt.
- **Bố cục chữ quản trị** — Tăng cỡ chữ mô tả dưới tiêu đề lên 16 px, chữ hướng dẫn lên 15 px và nới khoảng cách giữa các dòng. Tách hướng dẫn dài ở Khách hàng, AI và Hiệu năng thành từng khối. Bảng Danh mục cuộn trong khung trên màn hình nhỏ. Đã kiểm tra 9 trang quản trị trên Chrome ở **360 và 1.280 px**. Bản dựng frontend đạt.
- **Trạng thái hệ thống** — Căn ba ô trạng thái Embedding, Qdrant và Dự báo cùng kích thước, thẳng cột. Trên điện thoại, phần mô tả nằm dưới từng ô. Đã kiểm tra ở **360 và 1.280 px**.
- **Giao diện phát triển** — Việt hóa các mục chính trong menu Next.js và ẩn nút nổi mặc định để bảng công cụ không che cửa hàng hay trang quản trị. Có thể bật lại bằng `DAILYMART_DEVTOOLS=1` khi khởi động frontend ở chế độ phát triển.
- **Minh chứng chỉ mục** — Đo trên database demo riêng có đúng 500.000 đơn. Kế hoạch chuyển từ `Clustered Index Scan` sang `Index Seek`. Lượt đọc trang giảm từ **13.942** xuống **1.401 (89,95%)**, còn thấp hơn ngưỡng **trên 90%** của SRS. Thời gian trung vị giảm từ **195 ms** xuống **103 ms**. [Báo cáo và tệp minh chứng benchmark](BAO_CAO_BENCHMARK_500K.md)

## Đối chiếu 9 yêu cầu của đồ án

| # | Yêu cầu | Kết quả đối chiếu |
| --- | --- | --- |
| 1 | Thủ tục lưu, chỉ mục, trigger và AI trong CSDL | **Có đủ bốn phần.** DailyMart dùng SQL Server cho dữ liệu nghiệp vụ, Qdrant cho tìm kiếm vector và ONNX cho dự báo. |
| 2 | Hệ thống thương mại điện tử | **Có.** Có cửa hàng, giỏ hàng, thanh toán bằng ví, theo dõi đơn và trang quản trị. |
| 3 | CRUD bằng thủ tục lưu | **Có cho sản phẩm.** Thêm, đọc, sửa và xóa đều gọi thủ tục lưu qua Spring Boot. SQL Server tạo các thủ tục khi khởi tạo hệ thống. |
| 4 | Thanh toán ACID | **Có.** `sp_ThanhToanDonHang` gộp trừ ví, trừ kho, tạo đơn, ghi lịch sử ví và dọn giỏ vào cùng một giao dịch. Thủ tục dùng khóa để xử lý thanh toán đồng thời và rollback khi lỗi. |
| 5 | Trigger khi cập nhật và xóa | **Có.** Cập nhật sản phẩm tự ghi dữ liệu trước và sau. Xóa kích hoạt xóa mềm và ghi nhật ký. Admin có thể xem và lọc hai loại sự kiện. |
| 6 | Ràng buộc nghiệp vụ | **Có.** SQL Server chặn âm kho, âm ví, giá và số lượng không hợp lệ. Luồng thanh toán kiểm tra thêm hạn dùng và suất khuyến mãi. |
| 7 | Đồng bộ dữ liệu | **Có giữa SQL Server và Qdrant.** Thêm, sửa, xóa sản phẩm qua ứng dụng cập nhật chỉ mục vector. Admin có thể chạy đồng bộ lại khi cần. |
| 8 | Chỉ mục và dữ liệu lớn | **Có minh chứng trên 500.000 đơn, chưa đạt ngưỡng SRS.** Kế hoạch chuyển từ `Clustered Index Scan` sang `Index Seek`. Lượt đọc logic giảm **89,95%**, trung vị giảm từ **195 ms** xuống **103 ms**. SRS yêu cầu giảm ít nhất **90%** và thời gian dưới **100 ms**. [Minh chứng benchmark](BAO_CAO_BENCHMARK_500K.md) · [SRS](SRS.md) |
| 9 | AI, tìm kiếm ngữ nghĩa, dự báo và phân nhóm | **Có.** Có tìm kiếm ngữ nghĩa, dự báo nhu cầu bằng ONNX, xếp hạng sản phẩm bán chạy ngày mai và ước tính doanh thu cho kỳ 1, 7 hoặc 30 ngày. SQL chia khách hàng thành bốn nhóm theo lần mua gần nhất, số đơn và chi tiêu. |

Giá trị nhu cầu của bảng bán chạy lấy sản lượng ONNX nhân giá niêm yết. Dự báo doanh thu theo kỳ dùng trung bình bán theo thứ, tồn kho hiện tại, hạn dùng và suất khuyến mãi còn lại. Dashboard bán hàng và dự báo dùng nguồn riêng. Sai số của mô hình ONNX hiện được đo trên dữ liệu mô phỏng.

## Các cải tiến sau khi rà toàn bộ web

| Mục | Thay đổi và kiểm thử |
| --- | --- |
| Doanh thu và nguồn đơn | Gắn `SALE`, `DEMO_FORECAST` hoặc `BENCHMARK` cho đơn mới và phân loại lại các đơn mẫu cũ. Tổng quan, lịch sử khách và phân nhóm dùng đơn bán. Trang Hiệu năng đo trên đơn benchmark. Kiểm tra bản chạy cục bộ ghi nhận **683.000 đ** từ **1 đơn bán** và tách **7 đơn mô phỏng AI** khỏi doanh thu. |
| Ví và thanh toán | Thêm trang **Ví DailyMart** và đường dẫn từ thanh toán. Admin nạp số dư demo qua màn hình có xác nhận, CSRF và sổ cái ghi người nạp. Hai yêu cầu đồng thời dùng cùng mã và số tiền chỉ cộng một lần. Khách không được phép gọi API nạp. |
| Tìm kiếm | Kết quả xuất hiện ngay dưới thanh tìm kiếm trên điện thoại. Bấm **Xóa tìm kiếm** xóa cả từ khóa trong ô nhập. Đã thử trên Chrome ở **360 px**. |
| Giỏ hàng | Thêm nút tăng, giảm ngay trên từng món. SQL Server kiểm tra hàng và API trả lại giá, thành tiền, tổng giỏ. QA xác nhận 2 món được giá ưu đãi, 3 món vượt suất trở về giá niêm yết và lượng vượt kho bị từ chối. Đã kiểm tra thanh toán sau khi chỉnh giỏ. |
| Nhật ký quản trị | Tổng quan và trang Cảnh báo hiển thị tên sản phẩm và trường đã đổi, chẳng hạn **Tồn kho 4 → 2**. Có thể mở **Xem dữ liệu gốc** để đối chiếu JSON trước và sau. |
| Dự báo | Giả lập rủi ro cao trả kết quả và giữ nguyên số bản ghi dự báo, cảnh báo. Dự báo doanh thu theo kỳ 1, 7 hoặc 30 ngày cho phép chọn đơn bán hay dữ liệu mô phỏng. QA xác nhận ước tính thay đổi đúng khi đổi suất ưu đãi và tồn kho. Dữ liệu bán ở bản cục bộ hiện chỉ có doanh số trong **1 ngày**, nên kết quả còn dùng để minh họa. |

Số liệu benchmark 500.000 đơn ở trên là minh chứng của lần đo đã lưu. Phép so sánh trên trang Hiệu năng hiện lọc theo nguồn benchmark. Nhật ký dự báo cũ không có trường phân biệt giả lập với vận hành.

## Kết quả kiểm thử

**74/74 ca Java và SQL Server**, **22/22 ca frontend** và bản dựng frontend đạt. Đã kiểm tra qua Chrome các luồng mua hàng, quản trị và các tính năng mới. Các thử nghiệm ghi dữ liệu chạy trên database QA riêng.

Kiểm tra lại giao diện đăng nhập trên Chrome ở độ rộng **320, 390 và 1.440 px**. Chuyển vai trò trong cùng tab và đăng nhập đều hoạt động. Kiểm tra mở cửa hàng từ ô **DailyMart** và vị trí nút **Đăng xuất** ở **320 và 1.280 px**, xác nhận đăng xuất kết thúc phiên admin. Xác nhận nút Next.js mặc định đã ẩn ở cửa hàng và trang admin tại **390 và 1.440 px**.

Kiểm tra trên Chrome ở **320 và 1.440 px**. Ba sản phẩm trong giỏ tải đúng ảnh từ danh mục ở cả giỏ và trang thanh toán. Đã thử trường hợp thiếu ảnh và ảnh lỗi, chuyển giữa danh sách và chi tiết đơn hàng bằng chuột và bàn phím. Chạy lại **22/22 ca frontend** và kiểm tra TypeScript đều đạt.

Đã bỏ thẻ `<img` còn sót khi thay thành phần ảnh giỏ hàng, gây lỗi `Expression expected` trong lúc chỉnh sửa. `npm run build` đạt và Chrome mở được cửa hàng, giỏ hàng tại `localhost:3000`.

### Kiểm thử mô hình dự báo

- **Chức năng** — Đạt **5/5 ca Java**. API và tệp ONNX cùng cho **63,88 đơn vị** ở kịch bản đối chiếu. Với tồn kho 10, 70 và 150, hệ thống lần lượt đề xuất nhập 61, 1 và 0, phân đúng ba mức rủi ro, lưu lịch sử và ghi cảnh báo mức cao vào SQL Server. Chrome hiển thị đúng bảng xếp hạng **65 sản phẩm theo 12 đơn vị tính** trên database QA riêng.
- **Sai số** — Chạy trực tiếp tệp ONNX với scaler đi kèm trên **860 mẫu mới** tạo bằng cùng bộ sinh dữ liệu mô phỏng của notebook, đổi seed thành `20261110`. Kết quả **MAE 3,20 đơn vị**, **RMSE 4,25 đơn vị**, **R² 0,9751**, **WAPE 5,31%**, so với **18,12%** nếu dùng trung bình 7 ngày. Lần chạy với seed `42` cho WAPE **5,33%**, gần mức **5,34%** lưu trong repo. Chưa đo sai số trên doanh số thực.
- **Kiểm tra lại hai lỗi đã sửa** — Sau 0 giờ ngày 10/11/2026 theo giờ Việt Nam, dự báo lẻ, giả lập mặc định và toàn bộ **65 mặt hàng** trong đợt dự báo cùng trả ngày **11/11**, dù ngày mai theo UTC vẫn là **10/11**. Cả **7 trường số** giả lập trả HTTP 400 khi nhập âm và không tạo bản ghi. Giá trị 0, trường để trống và ngày chỉ định vẫn được chấp nhận. Chạy lại **5/5 ca Java** đạt.
- **Phạm vi mô hình** — Mô hình huấn luyện trên **12 mã sản phẩm**. Trong 65 mặt hàng QA, 53 mã nằm ngoài tập huấn luyện, 49 trong số đó có cả bốn đặc trưng lịch sử bằng 0. Ví dụ mã 13 vẫn được dự báo **16,41 đơn vị**. Chưa có dữ liệu để xác nhận độ chính xác của các dự báo ngoài tập huấn luyện.
