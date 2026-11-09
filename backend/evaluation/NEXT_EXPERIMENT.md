# Lượt thử tiếp theo: 50 truy vấn và kết quả thay thế riêng

## Quyết định sau lượt thử

Giữ mô hình đang chạy; **chưa đổi sang mô hình đa ngôn ngữ**. Đã áp dụng yêu cầu của người dùng: khi không có sản phẩm khớp đủ yêu cầu, kết quả chính phải rỗng; ứng viên gần nghĩa được hiển thị riêng ở “Sản phẩm khác có thể tham khảo”.

## Bộ dữ liệu và giới hạn

`search-queries-v2.json` có 50 câu: 20 development (15 có hàng, 5 không có), 30 holdout (24 có hàng, 6 không có). Có tên có dấu/không dấu, định dạng chữ, lỗi gõ, nhu cầu chế biến và mặt hàng ngoài catalog.

Nhãn sản phẩm phù hợp do assistant đề xuất, **chưa được người dùng/người hiểu nghiệp vụ duyệt**. Catalog chỉ có 12 mặt hàng. Một số câu holdout là biến thể tên các mặt hàng đã xuất hiện ở development; không phải phép thử tổng quát hóa sang sản phẩm mới. Không nên diễn giải Hit@3 thành độ chính xác của toàn bộ danh sách.

Snapshot SQL và kết quả cũ được lưu trước khi thêm bước tách gợi ý: `catalog-search-snapshot.json`. Giá/tồn kho trong snapshot là giá trị tại lượt thu thập, không phải dữ liệu vận hành hiện thời.

## Thử mô hình

Thử `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`, bản ONNX INT8 AVX2 chính thức, với tokenizer chính thức và mean pooling theo attention mask + L2 normalization. Revision và SHA256 nằm trong `model-manifest.json`; tải model vào thư mục `models/` đã được ignore.

Đối chứng English dùng mô hình ONNX hiện có với tokenizer chính thức. Hai mô hình thử dùng cùng tên, mô tả và danh mục gốc từ SQL, tối đa 128 tokens. Kết quả `CURRENT_HYBRID` lấy từ API cũ, còn dùng phần bổ sung từ khóa theo tên sản phẩm; vì vậy đây là đối chứng **pipeline**, không phải phép so sánh chỉ thay duy nhất mô hình.

Ngưỡng cosine được thử trên development: 0.3, 0.4, 0.5, 0.6, 0.7, 0.8. Mục tiêu chọn ngưỡng là trung bình MRR@5 của câu có hàng và tỷ lệ trả rỗng đúng của câu không có hàng. Ngưỡng chọn được là English 0.5, multilingual 0.7. Chọn xong mới tính báo cáo holdout; không chỉnh lại theo holdout.

Với mục tiêu cân bằng này, LIKE được chọn trên development vì từ chối mặt hàng không có tốt; nhưng bỏ lỡ quá nhiều câu có hàng. Đây cũng là cảnh báo rằng mục tiêu chọn cấu hình cần phản ánh đúng sản phẩm, không phải cứ lấy con số tổng cao nhất là tự triển khai.

| Pipeline trên 30 câu holdout | Có hàng: Hit@3 (24 câu) | Có hàng: MRR@5 | Không có hàng: trả rỗng đúng (6 câu) |
| --- | ---: | ---: | ---: |
| LIKE | 0.2917 | 0.2917 | 1.0000 |
| BM25 | 1.0000 | 0.9514 | 0.1667 |
| Hybrid cũ | 1.0000 | 0.9514 | 0.1667 |
| English raw vector, ngưỡng 0.5 | 0.8333 | 0.7917 | 0.3333 |
| Multilingual INT8 raw vector, ngưỡng 0.7 | 0.1250 | 0.1250 | 0.8333 |
| Multilingual INT8 raw hybrid | 1.0000 | 0.9306 | 0.1667 |

Kết quả chỉ nói rằng **cấu hình multilingual INT8 + tài liệu gốc + ngưỡng đã thử này** chưa đáng thay thế pipeline hiện tại. Không chứng minh mọi mô hình đa ngôn ngữ đều kém. Ngưỡng cao loại được nhiều kết quả sai nhưng cũng loại nhiều kết quả đúng. Xem toàn bộ sweep và top 3 trong `model-comparison.json`.

Đã tạo collection thử riêng `fresh_grocery_multilingual_trial_e8f8c211_raw_v1`, 12 points. Điểm Qdrant được đối chiếu với cosine tính trực tiếp; sai số lớn nhất khoảng 3.3e-8. Không ghi đè collection vận hành `fresh_grocery_products`. Xem `trial-collection-report.json`.

## Chính sách kết quả chính và gợi ý đã áp dụng

API trả hai mảng độc lập:

- `products`: các ứng viên có đầy đủ từ chính trong yêu cầu xuất hiện trong tên/mô tả/danh mục SQL.
- `relatedProducts`: ứng viên BM25/vector còn lại, tối đa 6 sản phẩm, không được coi là khớp yêu cầu và không trùng `products`.

Loại bỏ các từ yêu cầu thông dụng như “để”, “nấu”, “nguyên liệu” trước khi kiểm tra. Làm trước bước bỏ dấu để không nhầm “để” với “dê”. Không tự bỏ từ phủ định như “không”. Tên/mô tả và danh mục phải có đủ các từ còn lại; điểm cosine cao một mình không đủ xác nhận.

Ví dụ “mực ống tươi” không được coi tôm là kết quả khớp; “thịt gà nguyên con” không được coi trứng gà hoặc thịt heo là kết quả khớp. Nếu ONNX/Qdrant lỗi, vẫn có BM25 fallback, nhưng cả hai nhóm vẫn được tách theo cùng chính sách.

Đây là quy tắc bằng chứng từ khóa thận trọng, **không phải bộ hiểu ý định hoặc kiểm chứng ngữ nghĩa hoàn chỉnh**. Lỗi gõ “ca hoii” có thể chỉ được gợi ý cá hồi. “Cà chua làm sốt” có thể chỉ được gợi ý vì mô tả hiện chưa nói “sốt”. Không tự bổ sung công dụng vào SQL chỉ để làm đẹp điểm thử. `description-audit.json` ghi lại mô tả gốc và phần cần rà.

### Kiểm tra hồi quy sau thay đổi chính sách

Đây là phép đo lại trên các câu đã được xem sau lượt so sánh mô hình và sau khi nhận lựa chọn của người dùng. **Không phải một holdout mới độc lập.**

| Nhóm | Trước: không có hàng, trả rỗng đúng | Sau: không có hàng, trả rỗng đúng | Trước: có hàng, Hit@3 | Sau: có hàng, Hit@3 |
| --- | ---: | ---: | ---: | ---: |
| Development | 1/5 | 5/5 | 15/15 | 11/15 |
| Holdout cũ | 1/6 | 6/6 | 24/24 | 20/24 |

Không có sản phẩm đúng trong nhóm chính không đồng nghĩa mất hoàn toàn ứng viên: nhiều trường hợp được chuyển xuống nhóm gợi ý. Xem từng câu trong `result-contract-report.json`. Việc tách nhóm có đánh đổi giảm recall của nhóm chính, phù hợp với lựa chọn không trộn sản phẩm thay thế của người dùng.

## Chạy lại

```bash
python3 -m venv backend/evaluation/.venv
backend/evaluation/.venv/bin/pip install -r backend/evaluation/requirements.txt
backend/evaluation/.venv/bin/python backend/evaluation/download_models.py
backend/evaluation/.venv/bin/python backend/evaluation/compare_models.py
backend/evaluation/.venv/bin/python backend/evaluation/verify_trial_collection.py
backend/evaluation/.venv/bin/python backend/evaluation/evaluate_result_contract.py
```

`compare_models.py` mặc định dùng snapshot đối chứng đã đóng băng, có thể chạy offline sau khi tải model. Không refresh snapshot này từ API mới vì hai API có chính sách khác nhau. `evaluate_result_contract.py` gọi backend đang chạy để kiểm tra hợp đồng hai nhóm. `verify_trial_collection.py` cần Qdrant và chỉ ghi collection thử riêng. Thư viện Python của thử nghiệm độc lập với runtime Java của ứng dụng.

Để chạy lại báo cáo 10 câu lịch sử:

```bash
python3 backend/evaluation/evaluate_search.py --snapshot backend/evaluation/catalog-search-snapshot.json
```

Bước cần tiếp theo: người dùng duyệt nhãn và mô tả/công dụng sản phẩm; sau đó tạo thêm một nhóm truy vấn mới chưa dùng ở lượt này trước khi quyết định nâng mô hình hoặc nới chính sách xác nhận kết quả.
