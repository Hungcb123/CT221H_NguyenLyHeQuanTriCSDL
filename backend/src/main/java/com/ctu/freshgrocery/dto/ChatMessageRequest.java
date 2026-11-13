package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tin nhắn của khách hàng gửi tới AI Chatbot ẩm thực")
public class ChatMessageRequest {

    @Schema(description = "Mã khách hàng tương tác", example = "1")
    private Integer customerId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Schema(description = "Câu hỏi hoặc yêu cầu của khách hàng", example = "Tôi muốn nấu canh chua cá lóc cho 4 người ăn tối nay, bách hóa có đủ đồ không?")
    private String message;

    @Schema(description = "Mã phiên hội thoại (tùy chọn)", example = "conv-101")
    private String conversationId;
    private Integer recipeId;
}
