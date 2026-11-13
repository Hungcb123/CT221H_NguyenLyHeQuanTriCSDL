package com.ctu.freshgrocery.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.ctu.freshgrocery.dto.BatchForecastResponse;
import com.ctu.freshgrocery.dto.ForecastCustomRequest;
import com.ctu.freshgrocery.dto.ForecastResponse;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.DemandForecast;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.DemandForecastRepository;
import com.ctu.freshgrocery.repository.ForecastSpRepository;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.DemandForecastService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandForecastServiceImpl implements DemandForecastService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ProductRepository productRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final ForecastSpRepository forecastSpRepository;
    private final JdbcTemplate jdbcTemplate;

    private OrtEnvironment env;
    private OrtSession session;

    private float[] scalerMean;
    private float[] scalerScale;
    private List<String> featureNames;
    private Map<String, Object> evaluationMetrics = Collections.emptyMap();

    @PostConstruct
    public void init() {
        try {
            log.info("Khởi tạo môi trường Microsoft ONNX Runtime Java cho phân hệ AI Demand Forecasting...");
            this.env = OrtEnvironment.getEnvironment();

            // 1. Nạp file tham số chuẩn hóa scaler_params.json
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root;
            try (InputStream scalerStream = new ClassPathResource("models/scaler_params.json").getInputStream()) {
                root = mapper.readTree(scalerStream);
            }

            JsonNode meanNode = root.get("mean");
            JsonNode scaleNode = root.get("scale");
            JsonNode featuresNode = root.get("features");
            JsonNode evalNode = root.get("evaluation");

            int numFeatures = meanNode.size();
            this.scalerMean = new float[numFeatures];
            this.scalerScale = new float[numFeatures];
            this.featureNames = new ArrayList<>();

            for (int i = 0; i < numFeatures; i++) {
                this.scalerMean[i] = (float) meanNode.get(i).asDouble();
                this.scalerScale[i] = (float) scaleNode.get(i).asDouble();
                this.featureNames.add(featuresNode.get(i).asText());
            }

            if (evalNode != null && evalNode.isObject()) {
                this.evaluationMetrics = mapper.convertValue(evalNode, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }

            // 2. Định vị file mô hình best_grocery_demand_model.onnx và weights .onnx.data
            File modelFile = resolveModelFile();
            log.info("Nạp mô hình ONNX từ đường dẫn: {}", modelFile.getAbsolutePath());

            try (OrtSession.SessionOptions opts = new OrtSession.SessionOptions()) {
                this.session = env.createSession(modelFile.getAbsolutePath(), opts);
            }

            log.info("✅ Nạp thành công mô hình ONNX [PyTorch-MLP-ONNX] vào JVM memory. Sẵn sàng suy luận (<5ms/lượt)!");
        } catch (Exception e) {
            log.error("❌ Không thể khởi tạo mô hình ONNX Runtime: ", e);
            this.session = null;
        }
    }

    private File resolveModelFile() throws Exception {
        // Kiểm tra các đường dẫn trực tiếp trên filesystem
        String[] candidatePaths = {
                "backend/src/main/resources/models/best_grocery_demand_model.onnx",
                "src/main/resources/models/best_grocery_demand_model.onnx",
                "best_grocery_demand_model.onnx"
        };

        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }

        // Nếu chạy trong JAR/Docker container, trích xuất ra thư mục tạm
        Path tempDir = Files.createTempDirectory("onnx_model_");
        Path tempModel = tempDir.resolve("best_grocery_demand_model.onnx");
        Path tempData = tempDir.resolve("best_grocery_demand_model.onnx.data");

        try (InputStream inModel = new ClassPathResource("models/best_grocery_demand_model.onnx").getInputStream()) {
            Files.copy(inModel, tempModel, StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream inData = new ClassPathResource("models/best_grocery_demand_model.onnx.data").getInputStream()) {
            Files.copy(inData, tempData, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempModel.toFile();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
            log.info("Đã giải phóng tài nguyên ONNX Runtime Session.");
        } catch (Exception e) {
            log.warn("Lỗi đóng phiên ONNX Session: ", e);
        }
    }

    @Override
    public ForecastResponse predictForProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + productId));

        return predictForProduct(product, LocalDate.now(STORE_ZONE));
    }

    private ForecastResponse predictForProduct(Product product, LocalDate today) {
        LocalDate targetDate = today.plusDays(1);
        int currentStock = product.getSoLuongTon();
        int daysUntilExpiry = (int) Math.max(0, ChronoUnit.DAYS.between(today, product.getHanSuDung().toLocalDate()));

        // Real sales for the seven dates before the prediction date.
        double[] histFeatures = estimateHistoryFeatures(product.getMaSP(), targetDate);
        double lag1 = histFeatures[0];
        double lag7 = histFeatures[1];
        double rollingMean7 = histFeatures[2];
        double rollingStd7 = histFeatures[3];

        return executeInference(
                product.getMaSP(),
                product.getTenSP(),
                product.getDonViTinh(),
                product.getGia(),
                currentStock,
                daysUntilExpiry,
                targetDate,
                lag1,
                lag7,
                rollingMean7,
                rollingStd7,
                true
        );
    }

    @Override
    public ForecastResponse predictCustom(ForecastCustomRequest request) {
        Product product = productRepository.findById(request.getMaSP())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + request.getMaSP()));

        LocalDate today = LocalDate.now(STORE_ZONE);
        LocalDate targetDate = request.getTargetDate() != null ? request.getTargetDate() : today.plusDays(1);
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : product.getGia();
        int currentStock = request.getCurrentStock() != null ? request.getCurrentStock() : product.getSoLuongTon();
        int daysUntilExpiry = request.getDaysUntilExpiry() != null ? request.getDaysUntilExpiry() :
                (int) Math.max(0, ChronoUnit.DAYS.between(today, product.getHanSuDung().toLocalDate()));

        double[] hist = estimateHistoryFeatures(product.getMaSP(), targetDate);
        double lag1 = request.getLag1() != null ? request.getLag1() : hist[0];
        double lag7 = request.getLag7() != null ? request.getLag7() : hist[1];
        double rollingMean7 = request.getRollingMean7() != null ? request.getRollingMean7() : hist[2];
        double rollingStd7 = request.getRollingStd7() != null ? request.getRollingStd7() : hist[3];

        return executeInference(
                product.getMaSP(),
                product.getTenSP(),
                product.getDonViTinh(),
                unitPrice,
                currentStock,
                daysUntilExpiry,
                targetDate,
                lag1,
                lag7,
                rollingMean7,
                rollingStd7,
                false
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public BatchForecastResponse runBatchForecast() {
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> Boolean.FALSE.equals(p.getTrangThaiXoa()))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now(STORE_ZONE);
        List<ForecastResponse> results = new ArrayList<>();
        int totalRestock = 0;
        int highRiskCount = 0;
        BigDecimal expectedDemandValue = BigDecimal.ZERO;

        for (Product prod : products) {
            ForecastResponse res = predictForProduct(prod, today);
            results.add(res);
            totalRestock += res.getSoLuongDeXuatNhap();
            expectedDemandValue = expectedDemandValue.add(prod.getGia().multiply(BigDecimal.valueOf(res.getSoLuongDuBaoTieuThu())));
            if ("CAO".equalsIgnoreCase(res.getRuiRoHuHong())) {
                highRiskCount++;
            }
        }

        return BatchForecastResponse.builder()
                .ngayDuBao(today.plusDays(1))
                .tongSoMatHang(results.size())
                .tongSoLuongDeXuatNhap(totalRestock)
                .soMatHangRuiRoCao(highRiskCount)
                .giaTriNhuCauDuKien(expectedDemandValue)
                .danhSachDuBao(results)
                .build();
    }

    @Override
    public List<ForecastResponse> getForecastHistory(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + productId);
        }

        Product product = productRepository.findById(productId).orElseThrow();
        List<DemandForecast> list = demandForecastRepository.findByMaSPOrderByNgayTaoDesc(productId);

        return list.stream().map(f -> ForecastResponse.builder()
                .maDuBao(f.getMaDuBao())
                .maSP(f.getMaSP())
                .tenSP(product.getTenSP())
                .donViTinh(product.getDonViTinh())
                .ngayDuBao(f.getNgayDuBao())
                .soLuongDuBaoTieuThu(f.getSoLuongDuBaoTieuThu().doubleValue())
                .soLuongTonHienTai(f.getSoLuongTonHienTai())
                .soLuongDeXuatNhap(f.getSoLuongDeXuatNhap())
                .doTinCay(null)
                .thuatToan(f.getThuatToan())
                .ruiRoHuHong(f.getRuiRoHuHong())
                .doTreInferenceMs(null)
                .canhBaoLienKet(null)
                .featuresUsed(Collections.emptyMap())
                .build()
        ).collect(Collectors.toList());
    }

    private ForecastResponse executeInference(
            Integer productId,
            String productName,
            String donViTinh,
            BigDecimal unitPrice,
            int currentStock,
            int daysUntilExpiry,
            LocalDate targetDate,
            double lag1,
            double lag7,
            double rollingMean7,
            double rollingStd7,
            boolean save
    ) {
        if (session == null) throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Mô hình dự báo chưa sẵn sàng.");
        long startTime = System.currentTimeMillis();

        // 1. Xây dựng vector 10 đặc trưng chuỗi thời gian:
        // [ProductId, UnitPrice, DayOfWeek, IsWeekend, Month, DayOfMonth, Lag_1, Lag_7, Rolling_Mean_7, Rolling_Std_7]
        float dayOfWeek = (float) (targetDate.getDayOfWeek().getValue() - 1); // 0: Thứ 2 ... 6: Chủ nhật (theo Python)
        float isWeekend = (dayOfWeek == 5.0f || dayOfWeek == 6.0f) ? 1.0f : 0.0f;
        float month = (float) targetDate.getMonthValue();
        float dayOfMonth = (float) targetDate.getDayOfMonth();

        float[] rawFeatures = new float[]{
                productId.floatValue(),
                unitPrice.floatValue(),
                dayOfWeek,
                isWeekend,
                month,
                dayOfMonth,
                (float) lag1,
                (float) lag7,
                (float) rollingMean7,
                (float) rollingStd7
        };

        // 2. Chuẩn hóa đặc trưng bằng vector StandardScaler
        float[] scaledFeatures = new float[rawFeatures.length];
        for (int i = 0; i < rawFeatures.length; i++) {
            scaledFeatures[i] = (rawFeatures[i] - scalerMean[i]) / scalerScale[i];
        }

        // 3. Thực thi suy luận bằng ONNX Runtime
        float predictedDemandFloat = 0.0f;
        try {
            float[][] inputData = new float[][]{scaledFeatures};
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
                 OrtSession.Result result = session.run(Collections.singletonMap("float_input", inputTensor))) {
                Object outObj = result.get(0).getValue();
                if (outObj instanceof float[] arr) {
                    predictedDemandFloat = arr[0];
                } else if (outObj instanceof float[][] mat) {
                    predictedDemandFloat = mat[0][0];
                } else {
                    throw new IllegalStateException("Unsupported forecast model output");
                }
                if (!Float.isFinite(predictedDemandFloat)) throw new IllegalStateException("Non-finite forecast model output");
            }
        } catch (Exception e) {
            log.error("Lỗi suy luận ONNX: ", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi suy luận mô hình ONNX: " + e.getMessage());
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        // 4. Nghiệp vụ thực phẩm tươi sống:
        // Demand cannot be negative.
        double predictedDemand = Math.max(0.0, Math.round(predictedDemandFloat * 100.0) / 100.0);

        // Lượng nhập = max(0, ceil(predicted * 1.10) - currentStock) (Buffer 10% an toàn chống đứt gãy nguồn cung)
        int recommendedRestock = Math.max(0, (int) Math.ceil(predictedDemand * 1.10) - currentStock);

        // Đánh giá nguy cơ hư hỏng quá date
        String ruiRo;
        if (currentStock > predictedDemand * 1.5 && daysUntilExpiry <= 2) {
            ruiRo = "CAO";
        } else if (currentStock > predictedDemand) {
            ruiRo = "TRUNG_BINH";
        } else {
            ruiRo = "THAP";
        }

        Double doTinCay = null; // Regression output is not a calibrated confidence probability.
        String thuatToan = "PyTorch-MLP-ONNX";

        // 5. Lưu kết quả vào CSDL thông qua Stored Procedure sp_LuuKetQuaDuBaoNhapHang
        Long maDuBao = null;
        if (save) {
            Map<String, Object> result = forecastSpRepository.executeLuuKetQuaDuBao(
                    productId, targetDate, BigDecimal.valueOf(predictedDemand).setScale(2, RoundingMode.HALF_UP),
                    currentStock, recommendedRestock, null, thuatToan, ruiRo
            );
            Number code = (Number) result.get("StatusCode");
            if (code == null || code.intValue() != 1) {
                throw new AppException(HttpStatus.CONFLICT, "Không lưu được dự báo: " + result.get("Message"));
            }
            maDuBao = ((Number) result.get("MaDuBao_Output")).longValue();
        }

        String canhBaoLienKet = save && "CAO".equals(ruiRo)
                ? "Đã tự động tạo bản ghi [CANH_BAO_RUI_RO_THUC_PHAM] trong bảng NhatKyCanhBao!"
                : null;

        // Tổng hợp map 10 features phục vụ đối chứng minh bạch
        Map<String, Object> featuresUsed = new LinkedHashMap<>();
        featuresUsed.put("ProductId", productId);
        featuresUsed.put("UnitPrice", unitPrice);
        featuresUsed.put("DayOfWeek", (int) dayOfWeek);
        featuresUsed.put("IsWeekend", (int) isWeekend);
        featuresUsed.put("Month", (int) month);
        featuresUsed.put("DayOfMonth", (int) dayOfMonth);
        featuresUsed.put("Lag_1", lag1);
        featuresUsed.put("Lag_7", lag7);
        featuresUsed.put("Rolling_Mean_7", rollingMean7);
        featuresUsed.put("Rolling_Std_7", rollingStd7);

        return ForecastResponse.builder()
                .maDuBao(maDuBao)
                .maSP(productId)
                .tenSP(productName)
                .donViTinh(donViTinh)
                .ngayDuBao(targetDate)
                .soLuongDuBaoTieuThu(predictedDemand)
                .soLuongTonHienTai(currentStock)
                .soLuongDeXuatNhap(recommendedRestock)
                .doTinCay(doTinCay)
                .thuatToan(thuatToan)
                .ruiRoHuHong(ruiRo)
                .doTreInferenceMs(latencyMs)
                .canhBaoLienKet(canhBaoLienKet)
                .featuresUsed(featuresUsed)
                .build();
    }

    public Map<String, Object> getModelStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ready", session != null);
        status.put("name", "PyTorch-MLP-ONNX");
        status.put("features", featureNames == null ? List.of() : List.copyOf(featureNames));
        status.put("historySource", "SQL_SALES_7_DAYS");
        status.put("confidenceCalibrated", false);
        status.put("evaluation", evaluationMetrics == null ? Collections.emptyMap() : Collections.unmodifiableMap(evaluationMetrics));
        return status;
    }

    private double[] estimateHistoryFeatures(Integer productId, LocalDate targetDate) {
        var days = jdbcTemplate.queryForList("""
                SELECT CAST(d.NgayTao AS DATE) AS SaleDate, SUM(c.SoLuong) AS Quantity
                FROM ChiTietDonHang c JOIN DonHang d ON c.MaDH=d.MaDH
                WHERE c.MaSP=? AND d.NguonDon = 'SALE' AND d.TrangThai IN ('PAID','DELIVERED')
                  AND d.NgayTao >= ? AND d.NgayTao < ?
                GROUP BY CAST(d.NgayTao AS DATE)
                """, productId, java.sql.Date.valueOf(targetDate.minusDays(7)), java.sql.Date.valueOf(targetDate));
        double[] values = new double[7];
        for (var day : days) {
            LocalDate date = LocalDate.parse(day.get("SaleDate").toString());
            int index = (int) ChronoUnit.DAYS.between(targetDate.minusDays(7), date);
            if (index >= 0 && index < 7) values[index] = ((Number) day.get("Quantity")).doubleValue();
        }
        double mean = java.util.Arrays.stream(values).average().orElse(0);
        double variance = java.util.Arrays.stream(values).map(v -> (v - mean) * (v - mean)).sum() / 6;
        return new double[]{values[6], values[0], mean, Math.sqrt(variance)};
    }
}
