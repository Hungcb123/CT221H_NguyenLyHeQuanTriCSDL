#!/bin/bash
# =========================================================================
# Script tự động khởi tạo Database và chạy các file SQL trên MS SQL Server
# =========================================================================

set -e

sqlcmd() {
    /opt/mssql-tools18/bin/sqlcmd -S "${SQL_SERVER_HOST:-sqlserver}" -U sa -P "StrongPassword123!" -C -b -I -f 65001 "$@"
}

echo "⏳ Đang chờ Microsoft SQL Server sẵn sàng..."
for i in {1..50}; do
    if sqlcmd -Q "SELECT 1" > /dev/null 2>&1; then
        echo "✅ Microsoft SQL Server đã sẵn sàng kết nối!"
        break
    fi
    sleep 2
done

echo "🚀 Bắt đầu thực thi các file SQL khởi tạo CSDL..."

echo "1/5: Đang tạo bảng và Schema (01_init_schema.sql)..."
sqlcmd -i /database/01_init_schema.sql

sqlcmd -i /database/07_product_images.sql

echo "2/5: Đang tạo Stored Procedures CRUD (02_stored_procedures.sql)..."
sqlcmd -i /database/02_stored_procedures.sql

echo "3/5: Đang tạo Triggers kiểm soát (03_triggers.sql)..."
sqlcmd -i /database/03_triggers.sql

echo "4/5: Đang nạp dữ liệu mẫu ban đầu (04_seed_data.sql)..."
sqlcmd -i /database/04_seed_data.sql

echo "5/5: Đang nạp kịch bản Benchmark & Chỉ mục Covering Index (05_indexing_benchmark.sql)..."
sqlcmd -i /database/05_indexing_benchmark.sql

echo "Đang bổ sung danh mục 48 sản phẩm (06_extended_catalog.sql)..."
sqlcmd -i /database/06_extended_catalog.sql

echo "Đang gắn ảnh minh họa sản phẩm (08_catalog_images.sql)..."
sqlcmd -i /database/08_catalog_images.sql

sqlcmd -i /database/10_available_recipes.sql

echo "🎉 HOÀN TẤT KHỞI TẠO CƠ SỞ DỮ LIỆU FreshGroceryDB THÀNH CÔNG!"
