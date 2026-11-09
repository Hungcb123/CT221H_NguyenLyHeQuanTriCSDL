package com.ctu.freshgrocery.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.ctu.freshgrocery.service.EmbeddingService;
import com.ctu.freshgrocery.exception.AppException;
import org.springframework.http.HttpStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final int DIMENSION = 384;
    private static final int MAX_SEQ_LENGTH = 64;

    private OrtEnvironment env;
    private OrtSession session;
    private BertTokenizer tokenizer;
    private boolean isInitialized = false;

    @PostConstruct
    public void init() {
        try {
            log.info("Khởi tạo môi trường Microsoft ONNX Runtime cho Text Embedding (all-MiniLM-L6-v2)...");
            this.env = OrtEnvironment.getEnvironment();

            // Nạp file từ điển WordPiece
            this.tokenizer = new BertTokenizer("models/vocab.txt");

            // Nạp file mô hình all-MiniLM-L6-v2.onnx ra file tạm
            ClassPathResource modelResource = new ClassPathResource("models/all-MiniLM-L6-v2.onnx");
            if (!modelResource.exists()) {
                log.warn("Không tìm thấy file all-MiniLM-L6-v2.onnx trong models/. Tìm kiếm ngữ nghĩa sẽ trả lỗi cho đến khi mô hình sẵn sàng.");
                return;
            }

            Path tempModelPath = Files.createTempFile("all-MiniLM-L6-v2", ".onnx");
            File tempModelFile = tempModelPath.toFile();
            tempModelFile.deleteOnExit();

            try (InputStream is = modelResource.getInputStream()) {
                Files.copy(is, tempModelPath, StandardCopyOption.REPLACE_EXISTING);
            }

            try (OrtSession.SessionOptions opts = new OrtSession.SessionOptions()) {
                this.session = env.createSession(tempModelFile.getAbsolutePath(), opts);
            }
            this.isInitialized = true;
            log.info("✅ Nạp thành công mô hình ONNX Text Embedding all-MiniLM-L6-v2 trên JVM! (Dimension: {})", DIMENSION);
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo ONNX Embedding Session: {}", e.getMessage(), e);
            this.isInitialized = false;
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (!isInitialized || session == null || tokenizer == null) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Mô hình tìm kiếm ngữ nghĩa chưa sẵn sàng. Vui lòng thử lại sau.");
        }

        try {
            BertTokenizer.TokenizedInput tokenized = tokenizer.tokenize(text, MAX_SEQ_LENGTH);

            try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, tokenized.getInputIds());
                 OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, tokenized.getAttentionMask());
                 OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, tokenized.getTokenTypeIds())) {

                Map<String, OnnxTensor> inputs = Map.of(
                        "input_ids", inputIdsTensor,
                        "attention_mask", attentionMaskTensor,
                        "token_type_ids", tokenTypeIdsTensor
                );

                try (OrtSession.Result result = session.run(inputs)) {
                    // last_hidden_state: shape [1, seq_length, 384]
                    float[][][] hiddenStates = (float[][][]) result.get(0).getValue();
                    return meanPoolingAndNormalize(hiddenStates[0], tokenized.getAttentionMask()[0]);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi suy luận ONNX embedding", e);
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể xử lý tìm kiếm ngữ nghĩa. Vui lòng thử lại sau.");
        }
    }

    public boolean isReady() { return isInitialized && session != null && tokenizer != null; }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    private float[] meanPoolingAndNormalize(float[][] hiddenState, long[] attentionMask) {
        float[] pooled = new float[DIMENSION];
        float validCount = 0f;

        for (int i = 0; i < hiddenState.length; i++) {
            if (attentionMask[i] == 1L) {
                validCount += 1f;
                for (int d = 0; d < DIMENSION; d++) {
                    pooled[d] += hiddenState[i][d];
                }
            }
        }

        if (validCount > 0f) {
            for (int d = 0; d < DIMENSION; d++) {
                pooled[d] /= validCount;
            }
        }

        // Chuẩn hóa Vector L2 (để Cosine Similarity = Dot Product)
        float sumSquare = 0f;
        for (float val : pooled) {
            sumSquare += val * val;
        }
        float norm = (float) Math.sqrt(Math.max(sumSquare, 1e-12));
        for (int d = 0; d < DIMENSION; d++) {
            pooled[d] /= norm;
        }

        return pooled;
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
            log.info("Giải phóng tài nguyên ONNX Embedding Runtime.");
        } catch (Exception e) {
            log.warn("Lỗi khi đóng ONNX session: {}", e.getMessage());
        }
    }
}
