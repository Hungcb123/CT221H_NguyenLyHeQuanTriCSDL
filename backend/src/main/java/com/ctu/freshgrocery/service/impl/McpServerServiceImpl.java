package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.McpToolCallRequest;
import com.ctu.freshgrocery.dto.McpToolCallResponse;
import com.ctu.freshgrocery.dto.McpToolDto;
import com.ctu.freshgrocery.dto.RecipeDetailDto;
import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.McpServerService;
import com.ctu.freshgrocery.service.RecipeService;
import com.ctu.freshgrocery.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl implements McpServerService {

    private final RecipeService recipeService;
    private final ProductRepository productRepository;
    private final SemanticSearchService semanticSearchService;

    @Override
    public List<McpToolDto> getAvailableTools() {
        List<McpToolDto> tools = new ArrayList<>();

        // Tool 1: search_recipe_and_groceries
        Map<String, Object> schema1 = new HashMap<>();
        schema1.put("type", "object");
        Map<String, Object> props1 = new HashMap<>();
        props1.put("recipeName", Map.of("type", "string", "description", "Tên món ăn cần nấu (VD: canh chua cá lóc, thịt kho tàu)"));
        props1.put("servings", Map.of("type", "integer", "description", "Số lượng người ăn / khẩu phần (mặc định: 4)"));
        schema1.put("properties", props1);
        schema1.put("required", List.of("recipeName"));

        tools.add(McpToolDto.builder()
                .name("search_recipe_and_groceries")
                .description("Tìm kiếm công thức món ăn và kiểm tra sự sẵn có của các nguyên liệu tươi sống trong kho bách hóa.")
                .inputSchema(schema1)
                .build());

        // Tool 2: add_recipe_to_customer_cart
        Map<String, Object> schema2 = new HashMap<>();
        schema2.put("type", "object");
        Map<String, Object> props2 = new HashMap<>();
        props2.put("customerId", Map.of("type", "integer", "description", "Mã khách hàng"));
        props2.put("recipeId", Map.of("type", "integer", "description", "Mã công thức món ăn"));
        props2.put("servings", Map.of("type", "integer", "description", "Số khẩu phần cần thêm"));
        schema2.put("properties", props2);
        schema2.put("required", List.of("customerId", "recipeId"));

        tools.add(McpToolDto.builder()
                .name("add_recipe_to_customer_cart")
                .description("Thêm toàn bộ các nguyên liệu của món ăn vào giỏ hàng khách hàng thông qua Stored Procedure.")
                .inputSchema(schema2)
                .build());

        // Tool 3: semantic_product_search
        Map<String, Object> schema3 = new HashMap<>();
        schema3.put("type", "object");
        Map<String, Object> props3 = new HashMap<>();
        props3.put("query", Map.of("type", "string", "description", "Từ khóa hoặc tên mặt hàng thực phẩm cần tìm"));
        schema3.put("properties", props3);
        schema3.put("required", List.of("query"));

        tools.add(McpToolDto.builder()
                .name("semantic_product_search")
                .description("Tìm kiếm thông minh sản phẩm thực phẩm tươi sống theo tên hoặc ngữ nghĩa nhu cầu nấu ăn.")
                .inputSchema(schema3)
                .build());

        // Tool 4: create_checkout_redirect
        Map<String, Object> schema4 = new HashMap<>();
        schema4.put("type", "object");
        Map<String, Object> props4 = new HashMap<>();
        props4.put("customerId", Map.of("type", "integer", "description", "Mã khách hàng cần thanh toán"));
        schema4.put("properties", props4);
        schema4.put("required", List.of("customerId"));

        tools.add(McpToolDto.builder()
                .name("create_checkout_redirect")
                .description("Tạo phiên thanh toán đơn hàng và trả về URL chuyển tiếp đến trang checkout.")
                .inputSchema(schema4)
                .build());

        return tools;
    }

    @Override
    public McpToolCallResponse executeTool(McpToolCallRequest request) {
        String toolName = request.getName();
        Map<String, Object> args = request.getArguments() != null ? request.getArguments() : Collections.emptyMap();
        log.info("Thực thi MCP Tool call: [{}] với tham số: {}", toolName, args);

        try {
            switch (toolName) {
                case "search_recipe_and_groceries": {
                    String recipeName = (String) args.get("recipeName");
                    Integer servings = args.containsKey("servings") ? ((Number) args.get("servings")).intValue() : 4;
                    RecipeDetailDto recipe = recipeService.searchRecipeWithIngredients(recipeName, servings);
                    return McpToolCallResponse.builder()
                            .toolName(toolName)
                            .isSuccess(true)
                            .content(recipe)
                            .build();
                }

                case "add_recipe_to_customer_cart": {
                    Integer customerId = ((Number) args.get("customerId")).intValue();
                    Integer recipeId = ((Number) args.get("recipeId")).intValue();
                    Integer servings = args.containsKey("servings") ? ((Number) args.get("servings")).intValue() : 4;
                    Map<String, Object> result = recipeService.addRecipeToCart(customerId, recipeId, servings);
                    return McpToolCallResponse.builder()
                            .toolName(toolName)
                            .isSuccess(true)
                            .content(result)
                            .build();
                }

                case "semantic_product_search": {
                    String query = (String) args.get("query");
                    List<SemanticProductResponse> products = semanticSearchService.search(query, 5, 0.35f);

                    return McpToolCallResponse.builder()
                            .toolName(toolName)
                            .isSuccess(true)
                            .content(products)
                            .build();
                }

                case "create_checkout_redirect": {
                    Integer customerId = ((Number) args.get("customerId")).intValue();
                    Map<String, Object> redirect = Map.of(
                            "customerId", customerId,
                            "checkoutUrl", "/checkout?customerId=" + customerId,
                            "status", "READY_FOR_CHECKOUT",
                            "message", "Giỏ hàng đã sẵn sàng. Bạn có thể tiến hành thanh toán ví điện tử!"
                    );
                    return McpToolCallResponse.builder()
                            .toolName(toolName)
                            .isSuccess(true)
                            .content(redirect)
                            .build();
                }

                default:
                    return McpToolCallResponse.builder()
                            .toolName(toolName)
                            .isSuccess(false)
                            .errorMessage("Không tìm thấy công cụ MCP có tên: " + toolName)
                            .build();
            }
        } catch (Exception e) {
            log.error("Lỗi thực thi MCP Tool [{}]: ", toolName, e);
            return McpToolCallResponse.builder()
                    .toolName(toolName)
                    .isSuccess(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
