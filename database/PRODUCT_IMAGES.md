# Ảnh sản phẩm

- `SanPham.HinhAnh` lưu URL ảnh; không lưu dữ liệu nhị phân trong SQL Server.
- Admin cho phép dán URL hoặc tải ảnh JPG/PNG/WebP tối đa 5 MB. Tải ảnh cần phiên quản trị và CSRF token. Sau khi chọn ảnh, bấm **Lưu sản phẩm**.
- File tải lên nằm trong `backend/.local/product-images/`, được mount vào `/app/.local` để giữ ảnh khi tạo lại container. Sao lưu thư mục này cùng database.
- API danh sách, chi tiết, quản trị và tìm kiếm ngữ nghĩa cùng trả trường `hinhAnh`.
- `07_product_images.sql` bổ sung cột cho database cũ. Chạy tiếp `02_stored_procedures.sql` để cập nhật CRUD và kết quả truy vấn.

## Dữ liệu mẫu

Từ thư mục gốc, chạy `python database/download_catalog_images.py` để tải 60 ảnh minh họa và sinh `08_catalog_images.sql`. Sau đó chạy script SQL này trong `FreshGroceryDB`. Luồng `init_db.sh` cũng chạy các script ảnh theo đúng thứ tự.

Ảnh mẫu từ Wikimedia Commons minh họa loại nguyên liệu, không phải ảnh chụp nhãn hiệu hoặc lô hàng thật. Nguồn, tác giả và giấy phép được ghi trong `catalog-image-sources.json` và trang `/product-image-credits.html`. Một số sản phẩm cùng loại dùng chung ảnh minh họa. Script seed chỉ cập nhật ảnh trống hoặc ảnh demo; giữ nguyên ảnh quản trị đã tải lên.
