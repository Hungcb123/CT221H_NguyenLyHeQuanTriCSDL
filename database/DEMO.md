# Làm mới hạn dùng dữ liệu demo

Dữ liệu mẫu có hạn sử dụng thật tính từ ngày tạo. Khởi động lại Docker không làm mới hạn dùng.

Để mô phỏng lô hàng mới cho các sản phẩm mẫu đã hết hạn, chạy trong WSL:

```bash
docker exec freshgrocery-sqlserver bash -lc 'SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /database/refresh_demo_expiry.sql'
```

Script chỉ cập nhật sản phẩm khớp tên và mô tả của 60 sản phẩm mẫu gốc, chưa xóa và đã hết hạn. Mỗi sản phẩm dùng số ngày bảo quản như seed gốc; không đổi tồn kho, giá, đơn hàng hay ví. Chạy lại ngay không gia hạn tiếp các sản phẩm còn hạn. Các loại con và sản phẩm tự tạo trong admin không bị sửa.

Chỉ chạy trên database demo. Với hàng thật, cập nhật lô hàng mới và tồn kho đúng thực tế qua quản trị. Không bỏ điều kiện kiểm tra hạn dùng hoặc tự động gia hạn khi ứng dụng khởi động.
