package com.ctu.freshgrocery.service;

public interface EmbeddingService {

    /**
     * Chuyển đổi văn bản thành vector embedding 384 chiều chuẩn hóa L2
     * @param text Văn bản đầu vào
     * @return Mảng float[] vector 384 chiều
     */
    float[] generateEmbedding(String text);

    /**
     * Số chiều của vector
     */
    int getDimension();
}
