package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.ChatMessageRequest;
import com.ctu.freshgrocery.dto.ChatMessageResponse;
import com.ctu.freshgrocery.dto.McpToolCallRequest;
import com.ctu.freshgrocery.dto.McpToolCallResponse;
import com.ctu.freshgrocery.dto.McpToolDto;
import com.ctu.freshgrocery.dto.RecipeDetailDto;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.model.Recipe;
import com.ctu.freshgrocery.model.RecipeIngredient;
import com.ctu.freshgrocery.model.RecipeIngredientId;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.repository.RecipeRepository;
import com.ctu.freshgrocery.service.ChatbotService;
import com.ctu.freshgrocery.service.McpServerService;
import com.ctu.freshgrocery.service.RecipeService;
import com.ctu.freshgrocery.service.impl.ChatbotServiceImpl;
import com.ctu.freshgrocery.service.impl.McpServerServiceImpl;
import com.ctu.freshgrocery.service.impl.RecipeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ChatbotAndBenchmarkTest {

    private RecipeRepository recipeRepository;
    private ProductRepository productRepository;
    private com.ctu.freshgrocery.service.SemanticSearchService semanticSearchService;
    private RecipeService recipeService;
    private McpServerService mcpServerService;
    private ChatbotService chatbotService;

    @BeforeEach
    public void setup() {
        recipeRepository = Mockito.mock(RecipeRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        semanticSearchService = Mockito.mock(com.ctu.freshgrocery.service.SemanticSearchService.class);

        Product p1 = Product.builder()
                .maSP(1)
                .tenSP("Cá lóc tươi làm sạch")
                .donViTinh("con")
                .gia(new BigDecimal("75000.00"))
                .soLuongTon(20).hanSuDung(LocalDateTime.now().plusDays(2))
                .trangThaiXoa(false)
                .build();

        Product p4 = Product.builder()
                .maSP(4)
                .tenSP("Cà chua tươi Đà Lạt")
                .donViTinh("kg")
                .gia(new BigDecimal("32000.00"))
                .soLuongTon(50).hanSuDung(LocalDateTime.now().plusDays(2))
                .trangThaiXoa(false)
                .build();

        Recipe r1 = Recipe.builder()
                .maCongThuc(1)
                .tenMonAn("Canh chua cá lóc miền Tây")
                .khauPhanMacDinh(4)
                .moTa("Món canh chua truyền thống")
                .huongDanNau("Nấu cá với me và cà chua")
                .ngayTao(LocalDateTime.now())
                .build();

        RecipeIngredient ri1 = RecipeIngredient.builder()
                .id(new RecipeIngredientId(1, 1))
                .soLuongDinhLuong(new BigDecimal("1.00"))
                .donViTinh("con")
                .batBuoc(true)
                .recipe(r1)
                .product(p1)
                .build();

        RecipeIngredient ri2 = RecipeIngredient.builder()
                .id(new RecipeIngredientId(1, 4))
                .soLuongDinhLuong(new BigDecimal("1.00"))
                .donViTinh("kg")
                .batBuoc(true)
                .recipe(r1)
                .product(p4)
                .build();

        r1.setIngredients(List.of(ri1, ri2));

        when(recipeRepository.findById(1)).thenReturn(Optional.of(r1));
        when(recipeRepository.findByTenMonAnContainingIgnoreCase("canh chua")).thenReturn(List.of(r1));
        when(recipeRepository.findAll()).thenReturn(List.of(r1));
        when(productRepository.findAll()).thenReturn(List.of(p1, p4));

        recipeService = new RecipeServiceImpl(recipeRepository, null, null, productRepository, null, null);
        mcpServerService = new McpServerServiceImpl(recipeService, productRepository, semanticSearchService);
        com.ctu.freshgrocery.service.PromotionService promoService = Mockito.mock(com.ctu.freshgrocery.service.PromotionService.class);
        chatbotService = new ChatbotServiceImpl(mcpServerService, recipeService, promoService);
    }

    @Test
    public void testMcpToolsRegistry() {
        List<McpToolDto> tools = mcpServerService.getAvailableTools();
        assertNotNull(tools);
        assertEquals(4, tools.size(), "MCP Server phải cung cấp đúng 4 công cụ chuẩn hóa");

        List<String> toolNames = tools.stream().map(McpToolDto::getName).toList();
        assertTrue(toolNames.contains("search_recipe_and_groceries"));
        assertTrue(toolNames.contains("add_recipe_to_customer_cart"));
        assertTrue(toolNames.contains("semantic_product_search"));
        assertTrue(toolNames.contains("create_checkout_redirect"));
    }

    @Test
    public void testMcpSearchRecipeTool() {
        McpToolCallRequest req = McpToolCallRequest.builder()
                .name("search_recipe_and_groceries")
                .arguments(Map.of("recipeName", "canh chua", "servings", 4))
                .build();

        McpToolCallResponse resp = mcpServerService.executeTool(req);
        assertTrue(resp.getIsSuccess());
        assertNotNull(resp.getContent());

        RecipeDetailDto detail = (RecipeDetailDto) resp.getContent();
        assertEquals("Canh chua cá lóc miền Tây", detail.getTenMonAn());
        assertEquals(2, detail.getNguyenLieuList().size());
        assertEquals(new BigDecimal("107000.00"), detail.getTongTienUocTinh());
        assertTrue(detail.getDuNguyenLieuTrongKho());
    }

    @Test
    public void testChatbotRecipeConsultation() {
        ChatMessageRequest req = ChatMessageRequest.builder()
                .customerId(1)
                .message("Tôi muốn nấu món canh chua cá lóc cho 4 người ăn tối nay")
                .build();

        ChatMessageResponse resp = chatbotService.processChatMessage(req);
        assertNotNull(resp);
        assertTrue(resp.getReply().contains("Canh chua cá lóc miền Tây"));
        assertTrue(resp.getCanAddToCart(), "Phải cho phép bấm thêm vào giỏ hàng");
        assertFalse(resp.getCartAdded());
        assertNotNull(resp.getRecipeSuggested());
    }
}
