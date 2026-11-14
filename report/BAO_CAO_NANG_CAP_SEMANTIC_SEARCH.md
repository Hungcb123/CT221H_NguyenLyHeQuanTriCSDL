# BÁO CÁO NÂNG CẤP TOÀN DIỆN PHÂN HỆ TÌM KIẾM NGỮ NGHĨA (HYBRID SEMANTIC SEARCH)
**Dự án:** Hệ thống Thương mại Điện tử Bách Hóa Thực Phẩm Tươi Sống DailyMart (CT221H - Nguyên lý Hệ quản trị CSDL)  
**Ngày thực hiện:** 14/11/2026  

---

## 1. Chẩn đoán hiện trạng và 5 nguyên nhân gốc rễ trước khi nâng cấp

Qua khảo sát trực tiếp trên API thực tế (`GET /api/v1/search/semantic`), cơ sở dữ liệu MS SQL Server (`FreshGroceryDB`) và CSDL Vector Qdrant (`fresh_grocery_products`), hệ thống tìm kiếm trước khi nâng cấp trả về **0 sản phẩm chính (`products = []`) cho 17/19 câu truy vấn tự nhiên phổ biến** (kể cả các câu ví dụ mẫu trên giao diện như *"nguyên liệu nấu canh chua miền Tây"*, *"thực phẩm giàu protein tập gym"*, *"nước ép giảm cân detox"*).

Nguyên nhân đến từ **5 điểm nghẽn kết hợp giữa dữ liệu và thuật toán**:

1. **Thiếu hụt các mặt hàng thiết yếu và ngữ cảnh ẩm thực/dinh dưỡng trong CSDL (`SanPham`, `CongThucMonAn`):**
   - Kho hàng thiếu hoàn toàn nhóm gia vị tươi & khô cốt lõi (*tỏi, hành tím, gừng, sả, ớt hiểm, chanh, me vắt, nước tương/xì dầu, hạt nêm, tiêu đen*) và nhiều rau củ/trái cây/đồ tươi thiết yếu (*cần tây, táo xanh, bơ sáp, xà lách, cải thảo, khoai lang mật, nấm đùi gà, nấm rơm, giá đỗ, bạc hà/dọc mùng, ớt chuông, đậu hũ trắng, cua biển*) — dù chính mã nguồn và bài test từng nhắc tới *cần tây, táo xanh, gừng, cua*.
   - Mô tả (`MoTa`) của 60 sản phẩm ban đầu quá ngắn, thiếu thông tin món ăn, cách chế biến và giá trị dinh dưỡng (*giàu protein, tập gym, eat clean, giàu vitamin C, detox, bữa sáng, làm sốt, kho tộ...*).
2. **Lệch không gian ngôn ngữ trên mô hình ONNX `all-MiniLM-L6-v2` và giới hạn `MAX_SEQ_LENGTH = 64`:**
   - Mô hình nhúng `all-MiniLM-L6-v2.onnx` chạy trên JVM vốn được huấn luyện trên ngữ liệu **tiếng Anh**. Khi nhận trực tiếp chuỗi tiếng Việt bỏ dấu, bộ tách từ WordPiece cắt câu thành các mảnh ký tự rời rạc khiến điểm tương đồng Cosine bị nhiễu.
   - Hằng số `MAX_SEQ_LENGTH = 64` làm các mô tả dài bị cắt cụt trước khi kịp đi vào mạng nơ-ron.
3. **Nhiễu chỉ mục Qdrant do nạp lẫn biến thể quy cách con (`MaSPCha != NULL`) và sản phẩm `[TEST]`:**
   - Hàm `syncAllProducts()` cũ nạp cả **179 bản ghi** (gồm 92 biến thể đóng gói `300g/500g/1kg` và 3 gói `[TEST] Gói thử thanh toán`) vào Qdrant, trong khi bước tìm kiếm chỉ duyệt sản phẩm cha.
   - Hệ quả kép: các biến thể con có vector gần như trùng khít với sản phẩm cha (`bestScore - runnerUp ≈ 0.001 < 0.05`), làm điều kiện `clearSemanticWinner` gần như luôn thất bại; đồng thời sản phẩm `[TEST]` bị lọt vào danh sách gợi ý của khách hàng.
   - Ngoài ra, `SemanticSearchInitializer` cũ chỉ đồng bộ khi `pointsCount == 0`, khiến dữ liệu SQL mới thêm không được cập nhật vector khi khởi động lại.
4. **Bộ lọc ý định (`ProductQueryIntent`) và bằng chứng từ vựng (`ProductSearchEvidence`) quá cứng nhắc:**
   - `ProductQueryIntent` cũ chỉ giữ **1 nhóm thực thể dài nhất duy nhất**, làm các truy vấn nhiều nguyên liệu (*"hành tỏi ớt gừng sả"*, *"nước ép cần tây táo xanh"*, *"ngao hấp sả"*) tự loại bỏ các nguyên liệu còn lại.
   - `ProductSearchEvidence` yêu cầu một sản phẩm đơn lẻ phải chứa tất cả từ khóa của cả câu truy vấn, khiến truy vấn theo món ăn hoặc danh sách nguyên liệu trả về rỗng.
5. **Chưa có cơ chế khôi phục lỗi gõ phím tiếng Việt phổ biến:**
   - Các lỗi gõ lặp chữ hoặc thiếu ký tự cuối như `"ca hoii"`, `"ca looc"`, `"tom suu"`, `"rau muon"` không khớp từ vựng nên trả về rỗng.

---

## 2. Các hạng mục đã triển khai

### 2.1. Bổ sung dữ liệu danh mục, ảnh minh họa và công thức món ăn (`SQL Server`)
- Tạo mới script idempotent **`database/19_semantic_search_enrichment.sql`** và tích hợp vào **`database/init_db.sh`**:
  - **Bổ sung 23 sản phẩm tươi sống & gia vị thiết yếu** thuộc cả 5 danh mục (nâng tổng sản phẩm gốc từ **87** lên **110**, trong đó **107** sản phẩm thương mại hiển thị cho khách hàng):
    - *Rau củ quả & nấm & đậu hũ (10 SP):* Cần tây Đà Lạt, Xà lách lô lô xanh, Cải thảo Đà Lạt, Khoai lang mật Đà Lạt, Nấm đùi gà tươi, Nấm rơm tươi miền Tây, Giá đỗ sạch ủ mầm, Bạc hà nấu canh chua (dọc mùng), Ớt chuông Đà Lạt, Đậu hũ trắng truyền thống.
    - *Trái cây tươi (3 SP):* Táo xanh Mỹ tươi giòn, Bơ sáp Đắk Lắk, Chanh tươi không hạt.
    - *Thủy hải sản (1 SP):* Cua biển Cà Mau.
    - *Gia vị & Đồ khô (9 SP):* Tỏi ta Lý Sơn 250g, Hành tím Vĩnh Châu 250g, Gừng tươi rửa sạch 250g, Sả cây tươi 300g, Ớt hiểm đỏ tươi 100g, Me vắt nấu canh chua 200g, Nước tương đậu nành 500ml (xì dầu), Hạt nêm thịt thăn 400g, Tiêu đen xay Phú Quốc 100g.
  - **Giữ nguyên 100% tập kiểm thử phủ định ngoài danh mục:** Không thêm các mặt hàng dùng làm mốc kiểm thử âm tính (*sữa tươi, sữa chua, phô mai, bánh mì, sầu riêng, cà phê, nước ngọt, bột giặt, nước rửa chén, thịt dê, thịt vịt*).
  - **Làm giàu mô tả (`MoTa`) cho 33 sản phẩm hiện có** với ngữ cảnh món ăn, cách chế biến, dinh dưỡng và phương ngữ Bắc – Trung – Nam (*ngao, súp lơ xanh, dọc mùng, xì dầu...*).
  - **Bổ sung 6 công thức món ăn hoàn chỉnh** vào `CongThucMonAn` và `NguyenLieuMonAn` (nâng tổng lên **17 công thức**): *Lẩu hải sản chua cay, Phở bò truyền thống, Bò lúc lắc ớt chuông, Cá lóc kho tộ miền Tây, Gỏi cuốn tôm thịt thanh mát, Nước ép cần tây táo xanh detox*, đồng thời bổ sung đầy đủ nguyên liệu chuẩn cho *Canh chua cá lóc miền Tây*.
- Cập nhật **`database/demo-catalog/image-sources.json`** và tải đầy đủ **23 ảnh thực phẩm thực tế** (`semantic-01` đến `semantic-23`) vào `backend/.local/product-images/`, phục vụ trực tiếp qua `/api/v1/product-images/*` (100% HTTP 200 OK).

### 2.2. Nâng cấp thuật toán Hybrid Semantic Search trên Backend (`Spring Boot + ONNX + Qdrant`)
1. **Cầu nối ngữ nghĩa song ngữ Việt – Anh (`VietnameseFoodSemantics.java` + `search-culinary-knowledge.json`):**
   - Ánh xạ hai chiều các khái niệm món ăn Việt Nam (*canh chua, phở bò, lẩu hải sản, thịt kho tàu, cá kho tộ, bò lúc lắc, gỏi cuốn...*), nhu cầu sức khỏe/dinh dưỡng (*giàu protein/tập gym, eat clean, detox giảm mỡ, giàu vitamin C, omega-3, bữa sáng nhanh gọn, món chay thanh đạm...*) và hơn 100 nguyên liệu sang không gian ngữ nghĩa tiếng Anh tương ứng cho cả **tài liệu sản phẩm khi index** lẫn **câu truy vấn của khách hàng**.
   - Tăng `MAX_SEQ_LENGTH` trong `EmbeddingServiceImpl` từ **`64` lên `192` tokens**, đưa tín hiệu ngữ nghĩa cốt lõi lên đầu tài liệu để không bao giờ bị cắt cụt.
2. **Phân tích ý định đa thực thể bằng triệt tiêu khoảng con (*Longest Non-Overlapping Span Suppression*) trong `ProductQueryIntent.java`:**
   - Vừa giữ nguyên khả năng phân biệt cụm từ bao hàm (*"rau mùi tàu"* che *"rau mùi"*, *"cá hồi"* che *"cá"*, *"thăn bò"* che *"thịt bò"*), vừa cho phép nhận diện đồng thời nhiều nguyên liệu tách rời trên cùng câu tìm kiếm (*"hành tỏi ớt gừng sả"*, *"nước ép cần tây táo xanh"*, *"ngao hấp sả"*, *"gỏi cuốn tôm thịt"*).
   - Bổ sung khóa phạm vi danh mục thông minh: truy vấn *"trái cây giàu vitamin C"* chỉ trả về nhóm Trái cây (không lẫn cà chua/ớt chuông), *"rau ăn lẩu"* chỉ trả về nhóm Rau củ & nấm, *"hải sản để hấp"* chỉ trả về Thủy hải sản.
3. **Kết hợp trực tiếp đồ thị công thức món ăn từ SQL (`RecipeRepository.findAllWithIngredients`) vào xếp hạng RRF:**
   - Khi người dùng tìm theo tên món ăn (*"nguyên liệu nấu canh chua miền Tây"*, *"nấu phở bò"*, *"bò lúc lắc"*...), hệ thống đối chiếu trực tiếp bảng `CongThucMonAn` / `NguyenLieuMonAn` kết hợp điểm RRF (`BM25 + Cosine Vector`) để đưa toàn bộ nguyên liệu chính xác của món ăn lên nhóm kết quả chính.
4. **Sửa lỗi chính tả tiếng Việt theo ngữ cảnh an toàn (`resolveTypoQuery`):**
   - Tự động khôi phục các lỗi gõ lặp ký tự (`"ca hoii"` → `"ca hoi"`, `"ca looc"` → `"ca loc"`, `"tom suu"` → `"tom su"`) và lỗi khoảng cách Levenshtein = 1 có từ neo ngữ cảnh tên sản phẩm (`"rau muon"` → `"rau muong"`), tuyệt đối không biến đổi các từ đã có nghĩa trong từ điển.
5. **Chuẩn hóa đồng bộ Vector Qdrant (`SemanticSearchServiceImpl`, `QdrantServiceImpl`, `SemanticSearchInitializer`):**
   - Chỉ index **107 sản phẩm gốc thương mại** (`MaSPCha IS NULL`, không xóa mềm, loại trừ sản phẩm nội bộ `[TEST]`).
   - Bổ sung `QdrantService.retainOnlyPoints(...)` tự động quét và xóa sạch các điểm vector dư thừa/biến thể con cũ khỏi Qdrant.
   - Nâng cấp `SemanticSearchInitializer` tự động kiểm tra độ lệch số lượng giữa SQL Server và Qdrant khi khởi động container (`count != expectedCount`) để tự đồng bộ lại ngay lập tức.

### 2.3. Nâng cấp giao diện Tìm kiếm thông minh & Bếp Trưởng AI trên Frontend (`Next.js`) + Xử lý chọn SKU quy cách con trong Công thức
- **`RecipeServiceImpl.java` & `RecipeAvailabilityTest.java` (Backend):**
  - Khắc phục triệt để lỗi `"Vui lòng chọn loại sản phẩm trước khi thêm vào giỏ"` khi bấm **"Bỏ vào giỏ ngay"** trên công thức có nguyên liệu đa quy cách (*Đậu bắp, Dứa, Rau ngổ, Thịt ba rọi, Thịt bò thăn, Hành lá, Ngò gai, Húng quế...*). Hệ thống tự động ánh xạ từ sản phẩm gốc (`MaSPCha IS NULL`) sang **quy cách đóng gói con còn hàng phù hợp nhất** (*VD: `Đậu bắp tươi - 250g`, `Rau ngổ tươi - 50g`, `Dứa chín gọt sẵn - Hộp 300g`, `Thịt ba rọi heo VietGAP - 300g`*), tính chuẩn giá thực tế và nạp thành công 100% vào `sp_ThemVaoGioHang`.
- **`FloatingChatbot.tsx` (Frontend):**
  - Bổ sung nút **Mở rộng / Thu nhỏ (`Maximize2` / `Minimize2`)** ngay trên thanh tiêu đề cửa sổ Bếp Trưởng AI (hỗ trợ cả phím `Esc`), mở rộng khung chat lên tới `900px` với bố cục nguyên liệu 2 cột thoáng rộng và ẩn thanh cuộn ngang thô trên dải chip gợi ý.
- **`Header.tsx`:** Bổ sung nút xóa nhanh (`X`) ngay trong thanh tìm kiếm và gợi ý placeholder trực quan hơn.
- **`page.tsx`:** Bổ sung thanh **Gợi ý tìm kiếm AI** một chạm (*Nguyên liệu nấu canh chua miền Tây, Thực phẩm giàu protein tập gym, Nước ép giảm cân detox, Nấu phở bò, Nguyên liệu nấu lẩu hải sản, Trái cây giàu Vitamin C, Rau ăn lẩu, Hành tỏi ớt gừng sả*) giúp người dùng trải nghiệm tức thì khả năng hiểu ngữ nghĩa.
- **`ProductCard.tsx`:** Tách biệt màu nhãn khớp AI (`Khớp kết hợp` / `Khớp ngữ nghĩa` màu xanh ngọc, `Khớp từ khóa` màu xanh cổ vịt, `Tham khảo` màu xám trung tính) với nhãn giảm giá `-XX%` màu đỏ ở góc phải, giúp hiển thị đồng thời cả trạng thái khớp AI lẫn khuyến mãi.

---

## 3. Kết quả thực nghiệm Trước & Sau nâng cấp

### 3.1. So sánh chỉ số tổng hợp trên môi trường Docker thực tế

| Hạng mục đánh giá | Trước nâng cấp | Sau nâng cấp | Ghi chú |
| :--- | :---: | :---: | :--- |
| **Số sản phẩm gốc thương mại trên SQL & Qdrant** | 84 SP (lẫn 179 điểm nhiễu) | **107 SP / 107 vectors sạch** | Loại bỏ 100% vector biến thể con & `[TEST]` |
| **Số công thức món ăn hoàn chỉnh (`CongThucMonAn`)** | 11 công thức | **17 công thức** | Đủ nguyên liệu thực tế trong kho |
| **Tỷ lệ trả về đúng trên 25 câu truy vấn thực tế đa dạng** | 2 / 19 (~10.5%) | **25 / 25 (100%)** | Món ăn, dinh dưỡng, detox, phương ngữ, lỗi gõ |
| **Độ chính xác từ chối mặt hàng không bán (Negative Queries)** | 11 / 11 (100%) | **11 / 11 (100%)** | Không sinh kết quả ảo (`products = []`) |
| **Bộ kiểm thử danh mục & phương ngữ (`verify-search.cjs`)** | 62 / 62 (100%) | **62 / 62 (100%)** | Giữ vững toàn bộ cam kết quy cách & alias |
| **Unit tests Backend tìm kiếm (`JUnit 5`)** | 45 / 45 pass | **50 / 50 pass** | Bổ sung `VietnameseFoodSemanticsTest` |
| **Unit tests & Build Frontend (`Next.js 16`)** | 11 / 11 pass | **11 / 11 pass + Build OK** | `tsc --noEmit`, `eslint`, `next build` 0 lỗi |

### 3.2. Kết quả đối chiếu tiêu biểu trên API `GET /api/v1/search/semantic`

| Truy vấn người dùng | Trước nâng cấp (`products`) | Sau nâng cấp (`products` tiêu biểu & Cosine Score) |
| :--- | :--- | :--- |
| `nguyên liệu nấu canh chua miền Tây` | `[]` *(0 kết quả)* | **Bạc hà nấu canh chua** (`0.871`), **Cá lóc tươi làm sạch** (`0.795`), **Giá đỗ** (`0.803`), **Dứa** (`0.778`), **Me vắt** (`0.753`), **Cà chua**, **Đậu bắp**, **Rau ngổ** |
| `nước ép giảm cân detox` | `[]` *(gợi ý nhầm Nước mắm)* | **Cần tây Đà Lạt** (`0.859`), **Táo xanh Mỹ tươi giòn** (`0.789`), **Bưởi da xanh** (`0.778`), **Dưa leo baby** (`0.740`), **Ổi nữ hoàng** (`0.704`) |
| `thực phẩm giàu protein tập gym` | `[]` *(lọt gói `[TEST]`)* | **Cá hồi phi lê Nauy** (`0.815`), **Khoai lang mật** (`0.802`), **Bơ sáp Đắk Lắk** (`0.791`), **Trứng gà ta** (`0.709`), **Ức gà phi lê** (`0.696`) |
| `trái cây giàu vitamin C` | `[]` *(0 kết quả)* | **Quýt đường** (`0.918`), **Cam sành miền Tây** (`0.857`), **Thanh long ruột đỏ** (`0.845`), **Xoài cát Hòa Lộc** (`0.794`), **Bưởi da xanh** (`0.776`) |
| `nấu phở bò` | `[]` *(0 kết quả)* | **Ngò gai tươi** (`0.780`), **Húng quế tươi** (`0.770`), **Giá đỗ** (`0.729`), **Gừng tươi** (`0.716`), **Thịt bò bắp Úc** (`0.670`), **Thịt bò thăn** |
| `nguyên liệu nấu lẩu hải sản` | `[]` *(0 kết quả)* | **Cải thảo Đà Lạt** (`0.792`), **Nghêu trắng** (`0.768`), **Mực ống tươi** (`0.754`), **Tôm sú tươi sống** (`0.753`), **Me vắt** (`0.710`), **Nấm kim châm** |
| `hành tỏi ớt gừng sả` | `[]` *(0 kết quả)* | **Gừng tươi** (`0.709`), **Tỏi ta Lý Sơn** (`0.693`), **Ớt hiểm đỏ** (`0.672`), **Sả cây tươi** (`0.612`), **Hành tím Vĩnh Châu** (`0.570`), **Hành lá** |
| `ca hoii` / `tom suu` / `rau muon` | `[]` *(0 kết quả)* | **Cá hồi phi lê Nauy** (`0.647`) / **Tôm sú tươi sống** (`0.608`) / **Rau muống nước sạch** (`0.626`) |
| `sữa tươi không đường` / `thịt dê` / `bột giặt` | `[]` | `[]` *(Chặn chính xác 100% mặt hàng không kinh doanh)* |

---

## 4. Danh sách tệp tin đã thêm mới và cập nhật

1. **Cơ sở dữ liệu & Dữ liệu hình ảnh:**
   - `database/19_semantic_search_enrichment.sql` *(Thêm mới — 23 sản phẩm, làm giàu 33 mô tả, 6 công thức & 38 liên kết nguyên liệu)*
   - `database/init_db.sh` *(Cập nhật — tự động chạy `19_semantic_search_enrichment.sql` khi khởi tạo CSDL)*
   - `database/demo-catalog/image-sources.json` & `backend/.local/product-images/semantic-01..23.*` *(Bổ sung 23 ảnh minh họa thực phẩm)*
2. **Backend Spring Boot & Qdrant Hybrid Search:**
   - `backend/src/main/resources/search-culinary-knowledge.json` *(Thêm mới — tri thức ngữ nghĩa song ngữ món ăn, dinh dưỡng & nguyên liệu)*
   - `backend/src/main/resources/search-product-types.json` *(Cập nhật — mở rộng nhóm thực thể & phương ngữ cho toàn bộ danh mục mới)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/VietnameseFoodSemantics.java` *(Thêm mới — cầu nối ngữ nghĩa song ngữ, nhận diện khái niệm ẩm thực & sửa lỗi chính tả an toàn)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/EmbeddingServiceImpl.java` *(Cập nhật — nâng `MAX_SEQ_LENGTH` từ 64 lên 192)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/ProductQueryIntent.java` *(Cập nhật — triệt tiêu khoảng con không giao nhau, hỗ trợ giỏ nhiều nguyên liệu & phạm vi danh mục)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/ProductSearchEvidence.java` *(Cập nhật — lọc từ đệm hội thoại tiếng Việt)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/ProductBm25.java` *(Cập nhật — đồng nghĩa vùng miền an toàn)*
   - `backend/src/main/java/com/ctu/freshgrocery/repository/RecipeRepository.java` *(Cập nhật — bổ sung `findAllWithIngredients()`)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/QdrantService.java` & `QdrantServiceImpl.java` *(Cập nhật — bổ sung `retainOnlyPoints` dọn sạch vector thừa)*
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/SemanticSearchServiceImpl.java`, `ProductServiceImpl.java` & `RecipeServiceImpl.java` *(Cập nhật — tích hợp đầy đủ pipeline Hybrid Search mới và tự động chọn SKU quy cách con khi bỏ công thức vào giỏ)*
   - `backend/src/main/java/com/ctu/freshgrocery/config/SemanticSearchInitializer.java` *(Cập nhật — tự động đối chiếu và đồng bộ Qdrant theo SQL khi khởi động)*
   - `backend/src/test/java/com/ctu/freshgrocery/VietnameseFoodSemanticsTest.java` & `RecipeAvailabilityTest.java` *(Thêm mới & cập nhật bộ kiểm thử đơn vị)*
3. **Frontend Next.js:**
   - `frontend/src/components/chatbot/FloatingChatbot.tsx` *(Cập nhật — nút Mở rộng/Thu nhỏ khung chat lên 900px, lưới nguyên liệu 2 cột, bỏ thanh cuộn ngang thô)*
   - `frontend/src/components/layout/Header.tsx` *(Cập nhật — nút xóa nhanh và gợi ý tìm kiếm)*
   - `frontend/src/app/page.tsx` *(Cập nhật — thanh chip Gợi ý tìm kiếm AI một chạm)*
   - `frontend/src/components/product/ProductCard.tsx` *(Cập nhật — phân tách màu nhãn khớp AI và nhãn giảm giá)*
