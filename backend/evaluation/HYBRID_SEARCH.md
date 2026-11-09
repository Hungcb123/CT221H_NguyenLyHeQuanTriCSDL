# Luồng tìm kiếm đang áp dụng

Tìm kiếm sản phẩm trên giao diện mặc định là **hybrid BM25 + vector embeddings, gộp bằng RRF**. Không còn nút bật/tắt AI. Danh sách duyệt theo danh mục vẫn phân lô để tải khi cuộn.

## Hai endpoint

- `GET /api/v1/search/semantic?query=...&limit=12&minScore=0.3`: luồng chính trên giao diện, BM25 + ONNX/Qdrant + RRF. `minScore` là ngưỡng lấy ứng viên vector, không phải ngưỡng tự động công nhận kết quả chính.
- `GET /api/v1/products?keyword=...`: tương thích client cũ nhưng đã chuyển sang BM25, không gọi stored procedure tìm danh sách bằng `LIKE`. Vẫn giữ lọc danh mục, khoảng giá hiện tại, phân trang và sắp xếp giá/tồn kho/hạn dùng. Mặc định tìm kiếm ưu tiên mức liên quan.

`/products` không có keyword vẫn dùng stored procedure để duyệt danh sách. SQL scripts còn toán tử LIKE phục vụ code lịch sử/chức năng khác; việc thay thế ở đây có phạm vi luồng tìm kiếm sản phẩm của ứng dụng, không phải xóa mọi toán tử LIKE trong database.

Frontend không còn tự trả sản phẩm mock khi API danh sách/tìm kiếm sản phẩm lỗi.

## Xếp hạng và phân loại kết quả

1. Đọc catalog còn hoạt động từ SQL, tính BM25 trên tên và mô tả.
2. Sinh embedding thật bằng ONNX và lấy ứng viên Qdrant.
3. Gộp thứ hạng: `0.5/(60+rankBM25) + 0.5/(60+rankVector)`; ưu tiên tên chứa nguyên cụm truy vấn trước, sau đó điểm RRF, cuối cùng mã sản phẩm.
4. Phân kết quả chính và sản phẩm tham khảo theo chính sách dưới đây. Không coi điểm RRF hoặc cosine là phần trăm độ tin cậy.

### Mặt hàng cụ thể

`src/main/resources/search-product-types.json` là từ vựng loại hàng và tên gọi tương đương, ví dụ ba rọi/ba chỉ, cá hồi/salmon, đậu hũ/tofu. Đây là cấu hình nghiệp vụ, không chứa vector/điểm hoặc đáp án theo từng câu kiểm thử.

Nếu truy vấn chỉ rõ loại hàng mà catalog không có, kết quả chính rỗng. Nếu có loại hàng đó, các ứng viên loại khác không được chuyển thành kết quả chính chỉ vì cosine cao. Ưu tiên cụm tên dài để “thịt gà” khác “trứng gà”, “cá hồi” khác cá nói chung. Với truy vấn có dấu, giữ dấu để phân biệt “thịt để kho” và “thịt dê”, hoặc “cá” và “cà”. Truy vấn không dấu vẫn được đối chiếu tên tương đương.

### Tìm theo nhu cầu

Giữ đường chấp nhận theo bằng chứng từ tên/mô tả như trước, nhưng **không còn yêu cầu mọi kết quả chính phải khớp đủ từ**. Với truy vấn dạng nhu cầu/chế biến (“gợi ý”, “bữa”, “nguyên liệu”, “nấu”, “làm”, “món”, “để”…), ứng viên đứng đầu nhánh vector có thể vào nhóm chính khi:

- Không vi phạm loại mặt hàng được yêu cầu.
- Cosine đạt ngưỡng `search.semantic-primary-min-score`, mặc định **0.60** (đồng thời phải qua minScore lấy ứng viên).
- Điểm hơn ứng viên vector đứng thứ hai ít nhất `search.semantic-primary-min-margin`, mặc định **0.05**.

Ngưỡng và margin là quy tắc từ chối thử nghiệm, chưa được hiệu chuẩn thành độ tin cậy. Trường hợp thiếu chắc chắn chuyển xuống `relatedProducts`, không gọi là không có hàng một cách chắc chắn. Từ vựng và nhận diện nhu cầu hiện là heuristic có phạm vi giới hạn; không bảo đảm hiểu đúng mọi câu, điều kiện phủ định hoặc yêu cầu nhiều mặt hàng.

Ví dụ API thật đã thử: “gợi ý món cá hồi phi lê Nauy tươi” có sản phẩm cá hồi ở nhóm chính với cosine khoảng 0.644 dù mô tả không có đủ các từ “gợi ý món”. “Mực ống tươi” và “thịt dê” vẫn không có kết quả chính trong catalog hiện tại. Có kiểm thử riêng dùng vector mạnh nhưng không trùng từ khóa để chứng minh nhánh vector thực sự có thể tạo kết quả chính.

Cấu hình trong `application.yml`, có thể override:

```bash
SEARCH_SEMANTIC_PRIMARY_MIN_SCORE=0.60
SEARCH_SEMANTIC_PRIMARY_MIN_MARGIN=0.05
```

Nếu ONNX/Qdrant lỗi, trả `mode=KEYWORD_FALLBACK`, BM25 nhận toàn bộ trọng số và không có đường công nhận chỉ dựa vào vector. SQL lỗi thì trả lỗi, không dùng mock và không giả danh sách rỗng.

## Giao diện

`products` và `relatedProducts` không trùng nhau. Nhóm tham khảo được hiển thị riêng, ghi rõ chưa khớp đầy đủ yêu cầu. Tất cả lượt tìm từ header gọi endpoint hybrid. Banner báo khi đang fallback. Kết quả chính có thể rỗng trong khi vẫn có gợi ý, đúng lựa chọn của người dùng.

## Kiểm tra

```bash
cd backend
mvn -Dtest=HybridSearchTest,ProductQueryIntentTest,ProductKeywordSearchTest,ProductSearchEvidenceTest,ProductBm25Test,SemanticSearchTest,QdrantSearchFailureTest test
cd ../frontend
node --test tests/semantic-search.test.cjs
npx tsc --noEmit
npx eslint src/app/page.tsx src/components/layout/Header.tsx src/services/api.ts src/types/index.ts
```

`hybrid-main-report.json` là phép đo hồi quy lại 50 câu đã có, không phải tập kiểm tra mới độc lập. Các báo cáo trước trong NEXT_EXPERIMENT.md và result-contract-report.json là lịch sử của chính sách bắt khớp đủ từ. Không nên lấy số liệu lịch sử đó để mô tả chính sách hiện tại.

Mô hình production vẫn là all-MiniLM-L6-v2 do bản đa ngôn ngữ đã thử chưa đủ tốt để thay thế. Các giới hạn chất lượng tiếng Việt và gợi ý chưa liên quan vẫn còn; thay đổi này đảm bảo kiến trúc hybrid và cơ chế từ chối/fallback rõ ràng, không tuyên bố mô hình đã tối ưu.
