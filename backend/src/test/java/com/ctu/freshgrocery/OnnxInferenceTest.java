package com.ctu.freshgrocery;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OnnxInferenceTest {

    @Test
    public void testOnnxModelInferenceOnJvm() throws Exception {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        assertNotNull(env, "OrtEnvironment phải được khởi tạo");

        // 1. Kiểm tra đọc file scaler_params.json
        ClassPathResource scalerResource = new ClassPathResource("models/scaler_params.json");
        assertTrue(scalerResource.exists(), "File scaler_params.json phải tồn tại trong classpath");

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = scalerResource.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            assertTrue(root.has("mean"));
            assertTrue(root.has("scale"));
            assertTrue(root.has("features"));
            assertTrue(root.has("evaluation"));
            assertTrue(root.get("evaluation").get("r2").asDouble() > 0.95);
            assertTrue(root.get("evaluation").get("restockF1Percent").asDouble() > 90.0);
        }

        // 2. Tìm file model
        File modelFile = new File("src/main/resources/models/best_grocery_demand_model.onnx");
        if (!modelFile.exists()) {
            modelFile = new File("backend/src/main/resources/models/best_grocery_demand_model.onnx");
        }
        if (!modelFile.exists()) {
            modelFile = new File("best_grocery_demand_model.onnx");
        }
        assertTrue(modelFile.exists(), "File best_grocery_demand_model.onnx phải tồn tại");

        // 3. Khởi tạo OrtSession
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        try (OrtSession session = env.createSession(modelFile.getAbsolutePath(), opts)) {
            assertNotNull(session, "Session ONNX phải nạp thành công");

            // 4. Chạy suy luận thử nghiệm với input có chuẩn hóa StandardScaler
            float[] rawInput = new float[]{1.0f, 75000.0f, 5.0f, 1.0f, 10.0f, 15.0f, 48.0f, 45.0f, 46.5f, 4.2f};
            float[] scaledInput = new float[10];

            try (InputStream is = scalerResource.getInputStream()) {
                JsonNode root = mapper.readTree(is);
                for (int i = 0; i < 10; i++) {
                    float mean = (float) root.get("mean").get(i).asDouble();
                    float scale = (float) root.get("scale").get(i).asDouble();
                    scaledInput[i] = (rawInput[i] - mean) / scale;
                }
            }

            float[][] inputData = new float[][]{scaledInput};
            OnnxTensor tensor = OnnxTensor.createTensor(env, inputData);

            long start = System.currentTimeMillis();
            try (OrtSession.Result result = session.run(Collections.singletonMap("float_input", tensor))) {
                long duration = System.currentTimeMillis() - start;
                Object value = result.get(0).getValue();
                assertNotNull(value, "Kết quả suy luận không được null");

                float predicted = (value instanceof float[] arr) ? arr[0] : ((float[][]) value)[0][0];
                System.out.println("✅ ONNX Java Inference có chuẩn hóa thành công! Dự báo tiêu thụ: " + predicted + " đơn vị | Độ trễ: " + duration + "ms");
                assertTrue(predicted > 0, "Kết quả dự báo sản lượng phải dương");
            }
        }
    }
}
