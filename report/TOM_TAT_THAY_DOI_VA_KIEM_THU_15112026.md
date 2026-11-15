# Tóm tắt chỉnh sửa và kiểm thử

**Ngày:** 15/11/2026 · **Repo gốc:** `main` — `915e60c`.

## Đã chỉnh sửa

- Thêm xóa vĩnh viễn, xác nhận mã và bảo vệ dữ liệu liên quan; chỉnh đều cỡ chữ nút.
- Chỉ mở form khi bấm Thêm/Sửa; hủy chỉnh sửa quay về đúng vị trí.
- Hiển thị tồn kho từng phân loại theo cột dọc.
- Khôi phục ví khách hàng, lịch sử và thanh toán bằng ví; chặn khi thiếu tiền.
- Sửa dự báo quá lớn do đưa mã sản phẩm mới vào ONNX ngoài phạm vi huấn luyện; dùng trung bình 7 ngày khi chưa phù hợp.
- Thêm nút Tải lại hybrid search, giữ truy vấn và cập nhật kết quả.
- Cố định menu quản trị khi cuộn; màn hình thấp có thể cuộn riêng menu.
- Tìm kiếm quản trị theo tên/mã: bỏ khớp mô tả, yêu cầu đủ từ, hỗ trợ không dấu và ưu tiên tên khớp sát.

## Test tay đã đạt

- Tạo/sửa sản phẩm, tải ảnh, xóa vĩnh viễn sản phẩm thử.
- COD: đặt hàng, giao hàng, hủy và hoàn tồn; chặn thiếu hàng trên giao diện.
- Phân loại và khuyến mãi: trừ đúng kho, áp giá/suất, hủy hoàn kho/suất, tạm dừng/bật lại.
- Ví: nạp, thanh toán, chặn thiếu tiền, trả vừa đủ về 0 và hoàn tiền khi hủy.
- Phân nhóm: mua thêm đơn ví 51.000đ → **3 đơn · 784.000đ, Thân thiết**; hủy → **2 đơn · 733.000đ, Đang hoạt động**, hoàn ví 450.000đ và tồn kho 9.
- Trigger: nhật ký đổi tồn, hết hàng, giá bất thường, cận hạn và xóa mềm.
- Ràng buộc SQL: giá −1 và tồn −1 đều bị CHECK chặn với lỗi **547**; #1204 vẫn giữ giá **51.000đ**, tồn **9**.
- Giá khuyến mãi 40.000đ cao hơn giá gốc 30.000đ bị trigger chặn (**50000**); giá khuyến mãi vẫn **20.000đ**.
- AI: đồng bộ 110 vector; tìm “cà rốt” và “nấu canh chua” bằng HYBRID.
- Chatbot: “Nấu canh chua” báo thiếu nguyên liệu bán được và gợi ý 4 món thay thế đủ nguyên liệu, có giá và khẩu phần.
- Chatbot → checkout: công thức 4 người, **1 khay thịt 43.500đ + 1 vỉ trứng 35.000đ = 78.500đ**; giỏ khớp, không thêm trùng.
- Đơn COD nguyên liệu **#501012**, 78.500đ: thịt 300g **24 → 23**, trứng **65 → 64**; người dùng xác nhận hủy đơn hoàn lại 1 đơn vị.
- Nút Tải lại hybrid search được người dùng xác nhận hoạt động tốt.
- Dự báo sau sửa: #1204 trả **0,14 hộp**, tồn **9**, nhập **0**; lịch sử lưu đúng phương pháp Trung bình bán 7 ngày, giữ bản cũ để đối chiếu.
- Giả lập: tồn **5**, còn hạn **10 ngày** → dự báo **31**, nhập **30**, rủi ro **THAP**; người dùng xác nhận kết quả đúng.
- Bảng bán chạy: chạy dự báo **207 sản phẩm**; nhóm hộp có **8 sản phẩm**, xếp đúng từ 0,14 xuống 0, đề xuất nhập không âm.
- Doanh thu Đơn bán: ngày mai **0đ**, kỳ 7 ngày **51.000đ**; kỳ dài **102.000đ** gồm ngày 22 và 29/11, mỗi ngày 51.000đ; 7 ngày đầu khớp nhau.
- Hiệu năng: **500.000 đơn benchmark / 500.014 đơn tổng**; hai truy vấn cùng trả **3 dòng**, lượt đo trên web **497 ms / 135 ms** (~3,7 lần).
- SSMS: hai truy vấn khớp số đơn và tổng tiền theo 3 mã khách hàng; khác thứ tự dòng do chưa có ORDER BY.
- Người dùng xác nhận bước tách đơn benchmark khỏi danh sách và doanh thu bán hàng.
- SSMS I/O: logical reads **15.977 → 1.765** (~89% ít hơn), CPU **354 → 156 ms**; elapsed **37 → 157 ms** ở lượt này. Plan xác nhận A **Clustered Index Scan** chạy song song, B **Index Seek (NonClustered)** trên chỉ mục bao phủ. Không kết luận luôn nhanh hơn.
