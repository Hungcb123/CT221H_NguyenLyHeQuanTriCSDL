package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.ChatMessageRequest;
import com.ctu.freshgrocery.dto.ChatMessageResponse;
import com.ctu.freshgrocery.dto.RecipeDetailDto;
import com.ctu.freshgrocery.service.ChatbotService;
import com.ctu.freshgrocery.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/chatbot")
@RequiredArgsConstructor
@Tag(name = "6. AI Chatbot Ẩm Thực & Gợi Ý Món Ăn (Smart Recipe Assistant)", description = "Các API trợ lý Chatbot AI tư vấn món ăn gia đình, kiểm tra nguyên liệu và bỏ giỏ hàng tự động")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final RecipeService recipeService;

    @PostMapping("/chat")
    @Operation(
            summary = "Nhắn tin tương tác với AI Chatbot (REQ-AI-03)",
            description = "Trợ lý nội trợ thông minh hỗ trợ 2 kịch bản chính: " +
                    "1. Tư vấn công thức món ăn (VD: 'Tôi muốn nấu canh chua cá lóc cho 4 người') -> Tự động kích hoạt MCP Tool kiểm tra tồn kho và báo giá. " +
                    "2. Yêu cầu mua hàng (VD: 'Bỏ vào giỏ hàng và dẫn tôi đi thanh toán') -> Kích hoạt Stored Procedure sp_ThemNguyenLieuMonAnVaoGioHang và chuyển hướng sang trang thanh toán."
    )
    public ResponseEntity<ApiResponse<ChatMessageResponse>> chat(@Valid @RequestBody ChatMessageRequest request) {
        ChatMessageResponse response = chatbotService.processChatMessage(request);
        return ResponseEntity.ok(ApiResponse.<ChatMessageResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Trợ lý AI đã phản hồi thành công")
                .data(response)
                .build());
    }

    @GetMapping("/recipes")
    @Operation(summary = "Xem danh sách công thức món ăn đặc sản gia đình", description = "Lấy toàn bộ thực đơn công thức nấu ăn được bách hóa hỗ trợ sẵn nguyên liệu tươi sống")
    public ResponseEntity<ApiResponse<List<RecipeDetailDto>>> getAllRecipes() {
        List<RecipeDetailDto> recipes = recipeService.getAllRecipes();
        return ResponseEntity.ok(ApiResponse.<List<RecipeDetailDto>>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy danh sách công thức món ăn thành công")
                .data(recipes)
                .build());
    }

    @GetMapping("/recipes/{recipeId}")
    @Operation(summary = "Xem chi tiết công thức món ăn và sự sẵn có của nguyên liệu", description = "Tính toán định lượng và kiểm tra tồn kho thực tế theo số lượng người ăn")
    public ResponseEntity<ApiResponse<RecipeDetailDto>> getRecipeDetail(
            @Parameter(description = "Mã công thức món ăn", example = "1")
            @PathVariable Integer recipeId,
            @Parameter(description = "Số khẩu phần người ăn (mặc định: 4)", example = "4")
            @RequestParam(required = false, defaultValue = "4") Integer servings) {
        RecipeDetailDto recipe = recipeService.getRecipeDetail(recipeId, servings);
        return ResponseEntity.ok(ApiResponse.<RecipeDetailDto>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy chi tiết công thức món ăn thành công")
                .data(recipe)
                .build());
    }
}
