> **Luồng hiện tại:** [HYBRID_SEARCH.md](HYBRID_SEARCH.md). Hybrid đã thành mặc định; endpoint keyword chuyển sang BM25 và kết quả chính có thể nhận ứng viên ngữ nghĩa, thay cho quy tắc khớp đủ từ cũ.

> **Đã có lượt thử tiếp theo:** đọc [NEXT_EXPERIMENT.md](NEXT_EXPERIMENT.md) để xem 50 câu, thử mô hình đa ngôn ngữ và chính sách tách gợi ý. Nội dung bên dưới là báo cáo lịch sử trước bước tách nhóm. API hiện có thêm `relatedProducts`; báo cáo 10 câu nên chạy bằng snapshot như hướng dẫn trong tài liệu mới.

# Thử nghiệm tìm kiếm sản phẩm

Luồng tìm kiếm thông minh hiện tại:

1. Đọc các sản phẩm chưa bị xóa từ SQL Server. SQL lỗi thì trả lỗi, không giả danh sách rỗng.
2. Tính BM25 trên tên + mô tả sản phẩm. Bỏ dấu, chuyển chữ thường, tách theo từ; chưa có bộ tách từ tiếng Việt hoặc sửa lỗi chính tả.
3. Sinh embedding ONNX và truy vấn Qdrant; lọc theo ngưỡng cosine `minScore`.
4. Giữ tối đa 50 ứng viên mỗi nhánh, gộp sản phẩm theo mã và xếp bằng RRF.
5. Ưu tiên tên chứa nguyên cụm truy vấn (ranh giới từ) trước, sau đó điểm RRF, cuối cùng mã sản phẩm để thứ tự ổn định.

BM25 sử dụng `k1=1.2`, `b=0.75`, IDF dương `log(1 + (N-df+0.5)/(df+0.5))`.
Cách tính tham khảo [Lucene BM25Similarity](https://lucene.apache.org/core/7_6_0/core/org/apache/lucene/search/similarities/BM25Similarity.html).
Catalog hiện nhỏ nên tính trực tiếp trong backend; chưa thêm Elasticsearch hoặc chỉ mục BM25 lưu bền.

RRF dùng `0.5/(60+rankBM25) + 0.5/(60+rankVector)`. Không xuất hiện trong một nhánh thì nhánh đó đóng góp 0. Hai hệ số là trọng số thứ hạng bằng nhau, không cộng điểm BM25 trực tiếp với cosine, không chia đôi số kết quả và không biểu thị xác suất. Tham khảo [RRF](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/reciprocal-rank-fusion).

Nếu ONNX hoặc Qdrant lỗi, trả `mode=KEYWORD_FALLBACK` và chỉ xếp bằng BM25 (`1/(60+rankBM25)`). Frontend báo đang dùng từ khóa, kể cả khi không có sản phẩm phù hợp. Không dùng dữ liệu mock. Nếu vector chạy bình thường nhưng không có kết quả qua ngưỡng, vẫn trả các kết quả BM25 trong chế độ hybrid.

Endpoint `GET /api/v1/search/semantic` trả `data.products`, `mode`, `rankingMethod`, `rrfK`, `semanticWeight`, `keywordWeight`. Mỗi sản phẩm có `keywordScore` (BM25 thô), `similarityScore` (cosine, 0 nếu không có ứng viên vector), `rankingScore` (RRF). Không nên hiển thị các điểm này thành phần trăm độ tin cậy.

## Chạy so sánh

Khi backend, SQL Server, ONNX và Qdrant đã sẵn sàng:

```bash
cd backend
python3 evaluation/evaluate_search.py --output evaluation/search-report.json
```

Ở lượt lịch sử, script gọi API thật để so sánh:

- `LIKE`: endpoint danh sách sản phẩm với `keyword`, giữ nguyên cách sắp xếp mặc định của endpoint.
- `BM25`: xếp riêng theo điểm từ khóa.
- `VECTOR`: xếp riêng theo cosine, ngưỡng 0.3.
- `HYBRID`: kết quả cuối, bao gồm ưu tiên cụm tên và RRF.

`search-queries.json` có 6 truy vấn tên và 4 truy vấn nhu cầu, cùng các mặt hàng kỳ vọng ban đầu. Đây là nhãn đánh giá thủ công để bắt đầu thử nghiệm; cần người hiểu nghiệp vụ rà lại và mở rộng trước khi kết luận về chất lượng thực tế. Catalog thử phải nhỏ hơn cửa sổ ứng viên 50 mỗi nhánh để phép so sánh riêng BM25/vector không bị thiếu ứng viên.

Báo cáo tách hai nhóm và ghi top 3 từng truy vấn:

- Hit@3: có ít nhất một sản phẩm kỳ vọng trong 3 vị trí đầu.
- MRR@5: nghịch đảo thứ hạng sản phẩm kỳ vọng đầu tiên, 0 nếu không có trong top 5.
- Precision@3: số sản phẩm kỳ vọng trong top 3 chia cho 3.

Không tự chỉnh trọng số theo đúng 10 câu rồi coi kết quả đó là kiểm chứng độc lập. Mô hình embedding hiện tại vẫn có hạn chế với tiếng Việt; hybrid không bảo đảm sửa hết sai lệch ngữ nghĩa.

## Kiểm thử hồi quy

```bash
mvn -Dtest=ProductBm25Test,HybridSearchTest,SemanticSearchTest,QdrantSearchFailureTest test
cd ../frontend
node --test tests/semantic-search.test.cjs
```

## Kết quả lượt thử ban đầu

12 sản phẩm, 10 truy vấn; số liệu chi tiết trong [search-report.json](search-report.json).

| Cách tìm | Hit@3 toàn bộ | MRR@5 toàn bộ | MRR@5 nhóm tên | MRR@5 nhóm nhu cầu |
| --- | ---: | ---: | ---: | ---: |
| SQL LIKE | 0.50 | 0.5000 | 0.8333 | 0.0000 |
| BM25 | 0.90 | 0.8333 | 1.0000 | 0.5833 |
| Vector | 1.00 | 0.8833 | 0.8889 | 0.8750 |
| Tên chính xác + RRF | 1.00 | 0.9333 | 1.0000 | 0.8333 |

Hybrid có MRR toàn bộ cao hơn trong mẫu này, nhưng vector riêng tốt hơn ở nhóm nhu cầu. Một số kết quả vẫn lệch: “hải sản để hấp” đưa cá lóc và thịt bò lên trước tôm. BM25 hiện tách theo khoảng trắng và chưa loại từ dừng; những từ chung như “để” có thể đóng góp điểm không mong muốn. Không nên coi Hit@3=1 là mọi kết quả đều đúng hoặc mô hình đã hiểu tốt tiếng Việt. Cần bộ truy vấn độc lập, nhiều nhãn hơn và xem xét tokenizer/mô hình trước khi chọn cấu hình cho thực tế.
