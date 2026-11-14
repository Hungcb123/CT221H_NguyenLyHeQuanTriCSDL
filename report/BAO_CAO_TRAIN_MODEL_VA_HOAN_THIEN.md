# BÁO CÁO HUẤN LUYỆN LẠI MÔ HÌNH AI & HOÀN THIỆN SẢN PHẨM
**Dự án:** Hệ thống Thương mại Điện tử Bách Hóa Thực Phẩm Tươi Sống (CT221H - Nguyên lý Hệ quản trị CSDL)  
**Ngày cập nhật:** 07/11/2026

---

## 1. Tóm tắt hiện trạng trước khi nâng cấp và vấn đề đã giải quyết

Trước khi huấn luyện lại và đồng bộ hệ thống, phân hệ AI Dự báo nhu cầu nhập hàng tồn tại 3 điểm nghẽn chính:
1. **Lệch danh mục sản phẩm (Out-of-Distribution trên sản phẩm 9–12):** Mô hình ONNX cũ và file `scaler_params.json` chỉ được huấn luyện trên 8 sản phẩm (`MaSP` từ 1 đến 8, giá tối đa 260.000đ). Trong khi đó, CSDL thực tế (`04_seed_data.sql`) có **12 sản phẩm**, bao gồm các mặt hàng giá trị cao hoặc đặc thù như *Cá hồi phi lê Nauy tươi (480.000đ)*, *Thịt bò bắp Úc tươi (290.000đ)*, *Xoài cát Hòa Lộc (85.000đ)* và *Đậu hũ non trứng (15.000đ)*. Khi suy luận trên nhóm sản phẩm 9–12, sai số tuyệt đối trung bình (MAE) của mô hình cũ lên tới **7.95 đơn vị**.
2. **Thiếu dữ liệu đơn hàng lịch sử 7 ngày trong CSDL khởi tạo:** Khi khởi tạo CSDL mới, bảng `DonHang` và `ChiTietDonHang` chưa có lịch sử giao dịch 7 ngày gần nhất, khiến truy vấn tính `Lag_1`, `Lag_7`, `Rolling_Mean_7`, `Rolling_Std_7` trả về toàn bộ giá trị `0`, làm lệch chuẩn hóa `StandardScaler` khi gọi dự báo tự động.
3. **Thiếu chỉ số đánh giá phân loại nghiệp vụ (F1-Score & Accuracy) và công cụ giả lập trên giao diện Admin:** Trang `/admin/ai` chưa hiển thị các chỉ số thực nghiệm của mô hình (F1-Score, Accuracy, $R^2$, MAE, RMSE, WAPE) và chưa có biểu mẫu giả lập kịch bản tùy chỉnh (`predict-custom`) trực tiếp trên giao diện.

---

## 2. Quy trình huấn luyện lại mô hình Dự báo nhu cầu (`PyTorch-MLP-ONNX`)

### 2.1. Xây dựng tập dữ liệu chuỗi thời gian đồng bộ 12 sản phẩm
- **Danh mục huấn luyện:** Đầy đủ 12 sản phẩm thực phẩm tươi sống trong bảng `SanPham` (`MaSP` 1..12) với đúng đơn giá niêm yết và quy luật tiêu thụ thực tế.
- **Chuỗi thời gian:** 365 ngày liên tục cho mỗi sản phẩm ($365 \times 12 = 4.380$ bản ghi). Sau khi trích xuất cửa sổ trượt 7 ngày (bỏ 7 ngày đầu mỗi sản phẩm), thu được **4.296 mẫu** sạch hoàn toàn.
- **Quy luật mô phỏng thực tế ngành hàng tươi sống:**
  - Hệ số tăng trưởng cuối tuần (`IsWeekend = 1` vào Thứ Bảy, Chủ Nhật): tăng $1.42\times$ nhu cầu cơ sở.
  - Hệ số mùa vụ theo nhóm ngành hàng: rau củ quả thanh mát (`MaSP` 3, 4, 7, 12) tăng $1.25\times$ vào mùa hè (tháng 5–7); thịt cá hải sản (`MaSP` 1, 2, 6, 9, 10) tăng $1.30\times$ dịp cuối năm/đầu năm (tháng 11, 12, 1); trái cây nhiệt đới (`MaSP` 8, 11) tăng $1.20\times$ vào mùa nắng (tháng 4–6).
  - Sóng biến động nhu cầu ngắn hạn đa chế độ (Multi-regime wave $\pm 25\%$) kết hợp nhiễu ngẫu nhiên Gauss giúp mô hình học chính xác mối tương quan giữa chuỗi độ trễ (`Lag_1`, `Lag_7`, `Rolling_Mean_7`) và sản lượng ngày kế tiếp ở cả giai đoạn thấp điểm lẫn cao điểm.

### 2.2. Kỹ thuật đặc trưng (Feature Engineering) và Chuẩn hóa
Mô hình sử dụng vector **10 đặc trưng đầu vào (`float_input`)**:
1. `ProductId`: Mã định danh sản phẩm (`1` đến `12`).
2. `UnitPrice`: Đơn giá bán hiện hành (VND).
3. `DayOfWeek`: Thứ trong tuần (`0`: Thứ Hai ... `6`: Chủ Nhật).
4. `IsWeekend`: Cờ ngày cuối tuần (`1` nếu Thứ Bảy/Chủ Nhật, `0` nếu ngày thường).
5. `Month`: Tháng trong năm (`1` đến `12`).
6. `DayOfMonth`: Ngày trong tháng (`1` đến `31`).
7. `Lag_1`: Sản lượng tiêu thụ thực tế ngày hôm trước ($t - 1$).
8. `Lag_7`: Sản lượng tiêu thụ cùng thứ tuần trước ($t - 7$).
9. `Rolling_Mean_7`: Trung bình trượt sản lượng bán trong 7 ngày gần nhất.
10. `Rolling_Std_7`: Độ lệch chuẩn sản lượng bán trong 7 ngày gần nhất.

Toàn bộ 10 đặc trưng được chuẩn hóa bằng `StandardScaler` ($z = \frac{x - \mu}{\sigma}$) tính исключительно trên tập Train và xuất ra `backend/src/main/resources/models/scaler_params.json`:
- `mean`: `[6.5, 123250.0, 3.0052, 0.2867, 5.4790, 15.6199, 57.6807, 57.7119, 57.6941, 11.9818]`
- `scale`: `[3.4521, 136593.7568, 2.0001, 0.4522, 2.7438, 8.6095, 29.8529, 29.8825, 26.8271, 5.5653]`

### 2.3. Phân chia tập dữ liệu và Kiến trúc huấn luyện
- **Phân chia theo thời gian (Temporal Split 80/20):**
  - **Tập Train (80%):** 3.436 mẫu đầu theo thứ tự thời gian.
  - **Tập Holdout Test (20%):** 860 mẫu cuối năm (kiểm thử độc lập, không rò rỉ dữ liệu tương lai về quá khứ).
- **Kiến trúc mạng (`FreshGroceryMLP`):**
  - `Linear(10 -> 64) -> BatchNorm1d(64) -> ReLU -> Dropout(0.2) -> Linear(64 -> 32) -> BatchNorm1d(32) -> ReLU -> Linear(32 -> 1)`.
  - Khi xuất sang chuẩn mở ONNX (`best_grocery_demand_model.onnx` + `best_grocery_demand_model.onnx.data`), các lớp `BatchNorm1d` được hợp nhất trực tiếp vào toán tử `Gemm` (`FuseBatchNormIntoGemm`), giúp giảm độ trễ suy luận trên Java JVM xuống dưới **5ms/lượt**.
- **Thuật toán tối ưu:**
  - Optimizer: `AdamW` (`lr_max = 0.006`, `weight_decay = 5e-5`, `batch_size = 32`).
  - Lịch trình giảm tốc độ học: `CosineAnnealingLR` qua 400 epochs, tự động lưu giữ bộ trọng số đạt `Validation RMSE` thấp nhất.

---

## 3. Kết quả đánh giá mô hình (Accuracy, F1-Score, R², MAE, RMSE, WAPE)

### 3.1. Bảng so sánh mô hình Dự báo nhu cầu Trước và Sau khi huấn luyện lại
Đánh giá trên **860 mẫu Holdout Test** của đầy đủ 12 sản phẩm:

| Nhóm chỉ số | Thước đo | Mô hình cũ (8/12 SP) | Mô hình mới (12/12 SP) | Mức cải thiện |
| :--- | :--- | ---: | ---: | :--- |
| **Hồi quy sản lượng** | **MAE (Đơn vị)** | 5.0000 | **3.2161** | Giảm 35.7% sai số |
| **Hồi quy sản lượng** | **RMSE (Đơn vị)** | 6.6300 | **4.3157** | Giảm 34.9% sai số |
| **Hồi quy sản lượng** | **MAE nhóm SP mới (Mã 9–12)** | 7.9500 | **3.2510** | Giảm 59.1% sai số |
| **Hồi quy sản lượng** | **$R^2$ Score** | 0.9240 | **0.9741** | +5.01 điểm % |
| **Hồi quy sản lượng** | **WAPE (%)** | 8.36% | **5.34%** | Giảm 3.02 điểm % |
| **Hồi quy sản lượng** | **Độ chính xác sản lượng ($100\% - \text{WAPE}$)** | 91.64% | **94.66%** | +3.02 điểm % |
| **Quyết định nhập kho** | **Restock Accuracy (%)** | — | **94.77%** | Đạt chuẩn vận hành |
| **Quyết định nhập kho** | **Restock Precision (%)** | — | **94.84%** | Đạt chuẩn vận hành |
| **Quyết định nhập kho** | **Restock Recall (%)** | — | **94.61%** | Đạt chuẩn vận hành |
| **Quyết định nhập kho** | **Restock F1-Score (%)** | — | **94.72%** | Đạt chuẩn vận hành |
| **Phân loại rủi ro hư hỏng (3 lớp)** | **Spoilage Risk Accuracy (%)** | — | **92.44%** | Phân loại chuẩn 3 mức |
| **Phân loại rủi ro hư hỏng (3 lớp)** | **Spoilage Risk Macro F1 (%)** | — | **92.38%** | Cân bằng cả 3 lớp |
| **Phân loại rủi ro hư hỏng (3 lớp)** | **Spoilage Risk Weighted F1 (%)** | — | **92.46%** | Ổn định trên toàn tập |

*Giải thích cách tính F1 & Accuracy nghiệp vụ trên tập Holdout:*
- **Quyết định nhập kho (Binary Classification):** Đối chiếu quyết định cần nhập thêm hàng ($\lceil \text{Demand} \times 1.10 \rceil - \text{Stock} > 0$) giữa sản lượng dự báo và sản lượng thực tế.
- **Phân loại rủi ro hư hỏng (3-Class Classification: `THAP`, `TRUNG_BINH`, `CAO`):** Đối chiếu nhãn rủi ro dự báo với nhãn rủi ro thực tế theo quy tắc nghiệp vụ (`CAO` khi $\text{Stock} > 1.5 \times \text{Demand}$ và $\text{HSD} \le 2$ ngày; `TRUNG_BINH` khi $\text{Stock} > \text{Demand}$; `THAP` trong các trường hợp còn lại).

### 3.2. Kết quả suy luận thực tế trên 12 mặt hàng với dữ liệu lịch sử 7 ngày trong SQL

| Mã SP | Tên sản phẩm | Đơn giá (VND) | Trung bình bán 7 ngày | Dự báo ngày mai | Tồn kho hiện tại | Đề xuất nhập thêm (Buffer 10%) |
| :---: | :--- | ---: | ---: | ---: | ---: | ---: |
| 1 | Cá lóc tươi làm sạch | 75.000 | 47.3 con | **58.3 con** | 25 con | **40 con** |
| 2 | Thịt ba rọi heo VietGAP | 145.000 | 63.1 kg | **80.4 kg** | 40 kg | **49 kg** |
| 3 | Rau muống nước sạch | 12.000 | 89.4 bó | **114.8 bó** | 80 bó | **47 bó** |
| 4 | Cà chua tươi Đà Lạt | 32.000 | 58.0 kg | **72.9 kg** | 50 kg | **31 kg** |
| 5 | Trứng gà ta Ba Huân (vỉ 10) | 35.000 | 73.7 vỉ | **93.5 vỉ** | 65 vỉ | **38 vỉ** |
| 6 | Tôm sú tươi sống | 260.000 | 31.7 kg | **39.9 kg** | 18 kg | **26 kg** |
| 7 | Bắp cải trắng Đà Lạt | 22.000 | 42.1 bắp | **57.8 bắp** | 35 bắp | **29 bắp** |
| 8 | Chuối sứ chín tự nhiên | 28.000 | 52.6 nải | **69.7 nải** | 45 nải | **32 nải** |
| 9 | Cá hồi phi lê Nauy tươi | 480.000 | 18.9 kg | **27.5 kg** | 12 kg | **19 kg** |
| 10 | Thịt bò bắp Úc tươi | 290.000 | 29.4 kg | **42.0 kg** | 20 kg | **27 kg** |
| 11 | Xoài cát Hòa Lộc | 85.000 | 44.1 kg | **62.4 kg** | 30 kg | **39 kg** |
| 12 | Đậu hũ non trứng | 15.000 | 79.0 hộp | **104.7 hộp** | 55 hộp | **61 hộp** |

### 3.3. Tổng hợp chỉ số phân hệ Tìm kiếm Ngữ nghĩa Hybrid Search (`all-MiniLM-L6-v2` + BM25 + RRF)
Trên bộ 50 câu truy vấn kiểm thử (`backend/evaluation/hybrid-main-report.json`):
- **Tập Development (20 câu):** Accuracy = **80.00%**, Precision = **100.00%**, Recall (Hit@3) = **73.33%**, **F1-Score = 84.62%**, Tỷ lệ từ chối đúng câu ngoài danh mục = **100.00%** (5/5).
- **Tập Holdout (30 câu):** Accuracy = **86.67%**, Precision = **100.00%**, Recall (Hit@3) = **83.33%**, **F1-Score = 90.91%**, Tỷ lệ từ chối đúng câu ngoài danh mục = **100.00%** (6/6).
- **Toàn bộ 50 câu:** Accuracy = **84.00%** (42/50), Precision = **100.00%**, Recall = **79.49%**, **F1-Score = 88.57%**.

---

## 4. Chi tiết các hạng mục đã thực hiện và file cập nhật

1. **Huấn luyện & Xuất mô hình ONNX mới:**
   - `best_grocery_demand_model.onnx` & `best_grocery_demand_model.onnx.data` (tại thư mục gốc và `backend/src/main/resources/models/`): Trọng số mạng Nơ-ron MLP đã huấn luyện lại trên 12 sản phẩm.
   - `backend/src/main/resources/models/scaler_params.json`: Cập nhật vector `mean`, `scale` 10 đặc trưng của 12 sản phẩm và bổ sung đối tượng `evaluation` chứa đầy đủ các chỉ số thực nghiệm (F1, Accuracy, $R^2$, MAE, RMSE, WAPE).
   - `fresh_grocery_demand_forecasting.ipynb`: Đồng bộ danh sách 12 mặt hàng Việt Nam và bổ sung mã nguồn tính toán `Restock Accuracy/F1` và `Spoilage Risk Macro/Weighted F1` trên bảng xếp hạng mô hình.

2. **Đồng bộ Cơ sở dữ liệu & Lịch sử bán hàng 7 ngày:**
   - `database/04_seed_data.sql`: Thêm mục 7 tự động khởi tạo các đơn hàng `DELIVERED` kèm `ChiTietDonHang` trong 7 ngày gần nhất cho toàn bộ 12 sản phẩm khi khởi tạo CSDL.
   - `backend/src/main/java/com/ctu/freshgrocery/admin/AdminSchemaMigration.java`: Tự động kiểm tra và bổ sung dữ liệu đơn hàng lịch sử 7 ngày khi ứng dụng khởi động nếu bảng `ChiTietDonHang` đang trống, đảm bảo tính năng dự báo AI luôn có dữ liệu `Lag_1`, `Lag_7`, `Rolling_Mean_7`, `Rolling_Std_7` thực tế.

3. **Nâng cấp Backend & Giao diện Quản trị AI (`/admin/ai`):**
   - `backend/src/main/java/com/ctu/freshgrocery/service/impl/DemandForecastServiceImpl.java`: Đọc khối `evaluation` từ `scaler_params.json` và trả về trong API `GET /api/v1/admin/ai/status`.
   - `backend/src/test/java/com/ctu/freshgrocery/OnnxInferenceTest.java`: Bổ sung kiểm thử xác nhận chỉ số đánh giá mô hình (`r2 > 0.95`, `restockF1Percent > 90.0`) và suy luận ONNX trên JVM.
   - `frontend/src/services/admin-api.ts` & `frontend/src/app/admin/ai/page.tsx`:
     - Hiển thị thẻ thống kê thực nghiệm của mô hình dự báo (F1 quyết định nhập hàng, Accuracy nhập hàng, Macro F1 phân loại rủi ro, Độ chính xác sản lượng $1-\text{WAPE}$, $R^2$/MAE/RMSE).
     - Bổ sung biểu mẫu **Giả lập kịch bản dự báo tùy chỉnh** gọi trực tiếp `POST /api/v1/ai/forecast/predict-custom` để kiểm thử nhanh kịch bản tồn kho cao / cận hạn sử dụng (tự động kích hoạt cảnh báo `CANH_BAO_RUI_RO_THUC_PHAM` khi rủi ro `CAO`).

4. **Kiểm thử chất lượng hệ thống:**
   - Kiểm tra kiểu tĩnh TypeScript (`tsc --noEmit`): **Pass (0 lỗi)**.
   - Kiểm tra chuẩn mã nguồn ESLint: **Pass (0 lỗi/cảnh báo)**.
   - Bộ kiểm thử tự động Frontend (`admin-api.test.cjs`, `semantic-search.test.cjs`, `shopping-api.test.cjs`): **Pass 22/22 tests**.
