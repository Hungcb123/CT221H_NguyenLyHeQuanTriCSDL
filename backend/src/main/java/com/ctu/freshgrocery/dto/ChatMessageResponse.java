package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Phản hồi thông minh từ AI Chatbot ẩm thực")
public class ChatMessageResponse {
    private java.util.List<RecipeDetailDto> suggestedRecipes;

    @Schema(description = "Lời phản hồi và tư vấn tự nhiên của Chatbot", example = "Dạ bách hóa có đầy đủ nguyên liệu cho món Canh chua cá lóc miền Tây (khẩu phần 4 người)...")
    private String reply;

    @Schema(description = "Tên công cụ MCP được kích hoạt tự động (Tool Invocation)", example = "search_recipe_and_groceries")
    private String mcpToolInvoked;

    @Schema(description = "Chi tiết công thức món ăn được gợi ý (nếu có)")
    private RecipeDetailDto recipeSuggested;

    @Schema(description = "Có thể bấm thêm nhanh toàn bộ nguyên liệu vào giỏ hàng không?", example = "true")
    private Boolean canAddToCart;

    @Schema(description = "Trạng thái nguyên liệu đã được bỏ vào giỏ hàng tự động hay chưa", example = "false")
    private Boolean cartAdded;

    @Schema(description = "Số lượng món đã tự động bỏ vào giỏ")
    private Integer itemsAddedCount;

    @Schema(description = "Tổng tiền nguyên liệu đã bỏ vào giỏ (VND)")
    private BigDecimal totalCartPriceAdded;

    @Schema(description = "Đường dẫn chuyển tiếp sang trang thanh toán nhanh", example = "/checkout")
    private String checkoutUrl;
}
