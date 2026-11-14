# Dữ liệu demo quy cách sản phẩm và tìm kiếm

Bộ dữ liệu gồm **38 nhóm sản phẩm và 92 quy cách**, trong đó có **24 sản phẩm cha mới** và 14 sản phẩm cha đã tồn tại được bổ sung loại/mô tả. Các phần thịt là sản phẩm riêng; các khối lượng 300g, 500g, 1kg là loại thuộc sản phẩm đó.

## Nội dung

- Heo: ba rọi/ba chỉ, nạc vai, sườn non, chân giò, nạc thăn, thịt xay/băm/bằm.
- Bò: thăn, bắp, nạm, gầu, ức, sườn rút xương.
- Gà: ức/lườn, đùi góc tư, cánh, má đùi rút xương, chân gà, gà ta và gà công nghiệp nguyên con.
- Rau thơm: ngò rí/rau mùi, ngò gai/mùi tàu, rau ngổ/ngò om, hành lá/hành hoa, húng quế/rau quế, rau răm, tía tô, thì là, tần ô/cải cúc.
- Rau củ và thực phẩm khác: khổ qua/mướp đắng, bí đao/bí xanh, dưa leo/dưa chuột, đậu bắp/mướp tây, đậu cô ve/đậu que, củ đậu/củ sắn, khoai mì/sắn, bắp/ngô ngọt, dứa/thơm/khóm, đậu phộng/lạc.

**Phân biệt khi demo:** ngò rí khác ngò gai; rau răm khác ngò rí; củ sắn (củ đậu) khác khoai mì (sắn); ức gà khác ức bò. Các nhóm trong `search-product-types.json` hỗ trợ phân biệt ý định, còn tên gọi khác trong mô tả cung cấp bằng chứng từ khóa. Không thay đổi thuật toán tìm kiếm. Từ “thơm” vẫn được ghi trong mô tả dứa nhưng không dùng một mình làm điều kiện ép kiểu sản phẩm, vì còn có “rau thơm” và “gạo thơm”.

## Dữ liệu nghiệp vụ

`products.json` là nguồn để chỉnh sửa. Giá là giá minh họa do dự án đặt ra, tính cho một khay/túi/gói/con theo quy cách; không phải báo giá thị trường. Tồn kho của từng loại là số đơn vị đóng gói. Gà nguyên con ghi khối lượng **khoảng**, giá cố định theo lựa chọn; hệ thống chưa tính lại tiền theo cân thực tế.

Loại mới có hạn dùng tương đối theo lúc nạp (thịt 3 ngày, rau 5 ngày, đậu phộng khô 180 ngày) để chạy demo. Đây là ngày giả lập cho dữ liệu, không phải hạn dùng do nhà sản xuất chứng nhận.

Seed chạy trong một transaction. Chạy lại không thêm trùng, không đặt lại giá/tồn kho/hạn dùng hoặc khôi phục sản phẩm đã xóa mềm. Với sản phẩm cũ, seed chỉ nối mô tả một lần và thêm loại chưa có; giá, tồn kho và lịch sử đơn hiện có được giữ lại. Không thêm khuyến mãi tự động.

## Khởi tạo và đồng bộ

Compose đã gọi `17_demo_search_catalog.sql` trong `database/init_db.sh`. Với cơ sở dữ liệu đang chạy, có thể nạp riêng:

```bash
node database/demo-catalog/build-seed.cjs
node database/demo-catalog/build-image-seed.cjs

docker exec -i freshgrocery-sqlserver bash -c \
  '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -b -I -f 65001' \
  < database/17_demo_search_catalog.sql

docker exec -i freshgrocery-sqlserver bash -c \
  '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -b -I -f 65001' \
  < database/18_demo_catalog_images.sql

node database/demo-catalog/fetch-images.cjs
node database/demo-catalog/sync-search.cjs
```

Sau khi cập nhật từ vựng backend phải được build lại. SQL có thể tìm thấy dữ liệu mới ngay; Qdrant cần sync để cập nhật vector. Script sync dùng tài khoản admin local, hoặc `ADMIN_USERNAME`/`ADMIN_PASSWORD` từ môi trường, và không in thông tin đăng nhập.

Ảnh được lưu ở `backend/.local/product-images`. Nguồn, tác giả và giấy phép ảnh bổ sung nằm trong `image-sources.json`; ảnh có sẵn xem `database/catalog-image-sources.json`. Các ảnh là ảnh minh họa, không phải ảnh của hàng thật. Toàn bộ 38 nhóm và 92 quy cách trong bộ demo đã có ảnh. `18_demo_catalog_images.sql` bổ sung ảnh còn trống cho dữ liệu đã nạp; giữ nguyên ảnh tùy chỉnh đang có. Một số ảnh rau là ảnh cây/lá thực vật dùng minh họa nhận dạng.

## Kịch bản demo gợi ý

| Nhập tìm kiếm | Sản phẩm mong đợi / thao tác |
|---|---|
| `thịt lợn 300g` hoặc `thịt heo 300 g` | Các phần thịt heo có quy cách 300g; mở sản phẩm và chọn loại |
| `thăn bò 500g` | Thịt bò thăn; chọn 500g |
| `nạm bò`, `gầu bò`, `ức bò 500g` | Đúng phần thịt, không thay bằng thăn bò hoặc ức gà |
| `lườn gà 300g` | Ức gà phi lê; chọn 300g |
| `rau mùi`, `ngo ri`, `rau mùi 50g` | Ngò rí, không phải ngò gai |
| `mùi tàu` | Ngò gai, không phải ngò rí |
| `hành hoa`, `ngò om`, `cải cúc` | Hành lá, rau ngổ, tần ô |
| `mướp đắng`, `đậu que`, `dưa chuột` | Khổ qua, đậu cô ve, dưa leo |
| `củ sắn` / `khoai mì` | Củ đậu / khoai mì là hai kết quả khác nhau |
| `khóm`, `ngô ngọt`, `hạt lạc` | Dứa, bắp ngọt, đậu phộng |

Danh sách kết quả trả sản phẩm cha; người mua chọn khối lượng trong hộp chọn loại. Từ khóa khối lượng chưa tự chọn hoặc lọc một loại trong hộp này.

```bash
mvn -o -f backend/pom.xml -Dtest=CatalogAliasTest,SemanticSearchTest test
DEMO_RESULTS_PATH=/tmp/catalog-search-results.json node database/demo-catalog/verify-search.cjs
```

`search-cases.json` gồm 31 câu demo, mỗi câu kiểm tra tìm kiếm từ khóa và hybrid, cộng kiểm tra giá/đơn vị/tồn kho của 92 quy cách. Đây là kiểm tra chức năng với đáp án do dự án soạn, không phải tập đánh giá độc lập cho mô hình AI. Kết quả “liên quan” được tách riêng; các điều kiện loại trừ kiểm tra danh sách kết quả chính.
