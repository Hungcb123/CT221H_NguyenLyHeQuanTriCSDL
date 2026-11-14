# Minh chứng lập chỉ mục trên 500.000 đơn hàng

**Thời điểm đo** 00:04 ngày 10/11/2026, giờ Việt Nam.

**Môi trường** SQL Server 2022 Developer `16.0.4305.5` trong container riêng `dailymart-benchmark-500k`, giới hạn bộ nhớ container 2.500 MiB.

**Dữ liệu** Database `FreshGroceryBenchmark500K` chứa đúng 500.000 dòng `DonHang`, do `sp_SinhDuLieuLonBenchmark` tạo cho ba khách hàng mẫu. Database cửa hàng nằm trong container khác.

## Phép đo

Hai truy vấn tổng hợp số đơn và tổng chi tiêu theo mã khách hàng. Cả hai dùng cùng mốc sáu tháng và chỉ tính đơn `PAID` hoặc `DELIVERED`. Một truy vấn dùng `INDEX(0)`, truy vấn còn lại dùng `IX_DonHang_NgayTao_TrangThai_Covering` với khóa `(NgayTao, TrangThai)` và `INCLUDE (MaKH, TongTien)`.

Đã cập nhật thống kê bằng `FULLSCAN`, chạy làm nóng mỗi truy vấn một lần, sau đó đo năm lượt mỗi loại và luân phiên thứ tự. Hai truy vấn đều chạy với `MAXDOP 1` và `RECOMPILE`. SQL Server xuất `STATISTICS IO`, `STATISTICS TIME` và kế hoạch thực thi thực tế.

## Kết quả đo

| Chỉ số | Ép quét bảng | Chỉ mục bao phủ |
| --- | ---: | ---: |
| Toán tử đọc `DonHang` | `Clustered Index Scan` | `Index Seek` |
| Số lần đọc trang logic mỗi lượt | 13.942 | 1.401 |
| Số lần đọc vật lý mỗi lượt | 0 | 0 |
| Trung vị thời gian CPU | 179 ms | 100 ms |
| Trung vị thời gian thực thi | 195 ms | 103 ms |

Chỉ mục giảm **89,95% số lần đọc trang logic** và cho thời gian thực thi trung vị nhanh hơn **1,89 lần** trong phép đo này. Kết quả đọc trang thấp hơn ngưỡng **trên 90%** nêu trong SRS một khoảng nhỏ. Các lượt đo trả cùng kết quả cho cả ba khách hàng và cùng tổng **132.478 đơn** phù hợp bộ lọc. Kết quả thời gian phản ánh dữ liệu mẫu và bộ đệm đã làm nóng trên máy này.

## Tệp minh chứng

- [Kế hoạch thực thi khi quét bảng](benchmark-evidence/scan.sqlplan)
- [Kế hoạch thực thi khi dùng chỉ mục bao phủ](benchmark-evidence/covering.sqlplan)
- [Số liệu từng lượt](benchmark-evidence/measurements.csv)
- [Script thực hiện phép đo](database/benchmark_500k.sql)

Để tái tạo dữ liệu, dùng cấu trúc trong `database/01_init_schema.sql` và thủ tục cùng chỉ mục trong `database/05_indexing_benchmark.sql` trên database riêng mang tên `FreshGroceryBenchmark500K`. Tạo ba khách mẫu có mã từ 1 đến 3 rồi gọi `sp_SinhDuLieuLonBenchmark` với tham số `500000`. Script đo xác minh tên database và số dòng trước khi chạy.

Container demo đã dừng và volume giữ bộ dữ liệu. Để đo lại trên máy này, chạy các lệnh sau từ thư mục dự án. Chờ SQL Server khởi động xong sau lệnh đầu tiên.

```powershell
docker start dailymart-benchmark-500k
Get-Content -Raw database/benchmark_500k.sql | docker exec -i dailymart-benchmark-500k bash -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -b -I -f 65001 -y 0 -w 65535'
```
