package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.ChatMessageRequest;
import com.ctu.freshgrocery.dto.ChatMessageResponse;
import com.ctu.freshgrocery.dto.McpToolCallRequest;
import com.ctu.freshgrocery.dto.McpToolCallResponse;
import com.ctu.freshgrocery.dto.RecipeDetailDto;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.RecipeRepository;
import com.ctu.freshgrocery.dto.PromotionResponse;
import com.ctu.freshgrocery.service.ChatbotService;
import com.ctu.freshgrocery.service.McpServerService;
import com.ctu.freshgrocery.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final McpServerService mcpServerService;
    private final com.ctu.freshgrocery.service.RecipeService recipeService;
    private final PromotionService promotionService;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public ChatMessageResponse processChatMessage(ChatMessageRequest request) {
        String msg = request.getMessage().trim().toLowerCase();
        Integer customerId = request.getCustomerId();

        log.info("Xử lý tin nhắn từ khách hàng [{}]: \"{}\"", customerId, request.getMessage());

        // Kịch bản 1: Yêu cầu bỏ nguyên liệu vào giỏ hàng & thanh toán
        if (msg.contains("bỏ vào giỏ") || msg.contains("thêm vào giỏ") || msg.contains("mua hết") || msg.contains("thanh toán")) {
            // Xác định món ăn đang nói tới (mặc định Canh chua cá lóc nếu không nói rõ)
            if (customerId == null) return ChatMessageResponse.builder().reply("Vui lòng đăng nhập trước khi thêm vào giỏ.").cartAdded(false).build();
            if (request.getRecipeId() == null) return ChatMessageResponse.builder().reply("Bạn muốn thêm nguyên liệu món nào? Hãy chọn một món từ gợi ý trước.").cartAdded(false).build();
            int recipeId = request.getRecipeId();
            int servings = detectServings(msg);

            // 1. Gọi MCP Tool: add_recipe_to_customer_cart
            Map<String, Object> addArgs = new HashMap<>();
            addArgs.put("customerId", customerId);
            addArgs.put("recipeId", recipeId);
            addArgs.put("servings", servings);

            McpToolCallResponse addRes = mcpServerService.executeTool(
                    McpToolCallRequest.builder()
                            .name("add_recipe_to_customer_cart")
                            .arguments(addArgs)
                            .build()
            );

            if (!Boolean.TRUE.equals(addRes.getIsSuccess())) {
                return ChatMessageResponse.builder().reply(addRes.getErrorMessage() != null
                        ? addRes.getErrorMessage() : "Không thể thêm nguyên liệu vào giỏ. Vui lòng kiểm tra lại nguyên liệu.")
                        .canAddToCart(false).cartAdded(false).itemsAddedCount(0).build();
            }

            // 2. Gọi MCP Tool: create_checkout_redirect
            McpToolCallResponse redirectRes = mcpServerService.executeTool(
                    McpToolCallRequest.builder()
                            .name("create_checkout_redirect")
                            .arguments(Map.of("customerId", customerId))
                            .build()
            );

            int itemsAdded = 0;
            BigDecimal totalPrice = BigDecimal.ZERO;
            if (addRes.getIsSuccess() && addRes.getContent() instanceof Map<?, ?> map) {
                if (map.containsKey("SoLuongMonDaThem")) {
                    itemsAdded = ((Number) map.get("SoLuongMonDaThem")).intValue();
                }
                if (map.containsKey("TongTienNguyenLieu")) {
                    totalPrice = (BigDecimal) map.get("TongTienNguyenLieu");
                }
            }

            String reply = String.format(
                    "🛒 Dạ vâng! Trợ lý AI đã tự động thêm đầy đủ %d nguyên liệu tươi sống vào giỏ hàng của bạn (Tổng tiền tạm tính: %s VND).\n\n" +
                            "👉 Đơn hàng đã sẵn sàng! Bạn có thể nhấn vào liên kết bên dưới để tiến hành thanh toán ví điện tử nhanh chóng:",
                    itemsAdded, CURRENCY_FORMAT.format(totalPrice)
            );

            return ChatMessageResponse.builder()
                    .reply(reply)
                    .mcpToolInvoked("add_recipe_to_customer_cart + create_checkout_redirect")
                    .canAddToCart(false)
                    .cartAdded(true)
                    .itemsAddedCount(itemsAdded)
                    .totalCartPriceAdded(totalPrice)
                    .checkoutUrl("/checkout?customerId=" + customerId)
                    .build();
        }

        // Kịch bản 2: Hỏi công thức món ăn và tư vấn nguyên liệu
        if (msg.contains("nấu") || msg.contains("công thức") || msg.contains("canh chua") || msg.contains("kho tàu") || msg.contains("món gì")) {
            String recipeName = request.getMessage();
            int servings = detectServings(msg);

            // Gọi MCP Tool: search_recipe_and_groceries
            McpToolCallResponse toolRes = mcpServerService.executeTool(
                    McpToolCallRequest.builder()
                            .name("search_recipe_and_groceries")
                            .arguments(Map.of("recipeName", recipeName, "servings", servings))
                            .build()
            );

            if (toolRes.getIsSuccess() && toolRes.getContent() instanceof RecipeDetailDto recipe) {
                if (!Boolean.TRUE.equals(recipe.getDuNguyenLieuTrongKho())) {
                    var alternatives = recipeService.getAllRecipes().stream()
                            .filter(r -> Boolean.TRUE.equals(r.getDuNguyenLieuTrongKho()))
                            .filter(r -> !r.getMaCongThuc().equals(recipe.getMaCongThuc())).limit(4).toList();
                    String missing = recipe.getNguyenLieuList().stream().filter(i -> !Boolean.TRUE.equals(i.getConHang()))
                            .map(RecipeDetailDto.IngredientItemDto::getTenSP).collect(java.util.stream.Collectors.joining(", "));
                    return ChatMessageResponse.builder().reply("Món **" + recipe.getTenMonAn()
                            + "** hiện thiếu nguyên liệu còn bán được: " + missing
                            + (alternatives.isEmpty() ? ". Kho chưa đủ nguyên liệu cho các công thức hiện có."
                            : ". Bạn có thể chọn các món dưới đây, đã kiểm tra đủ nguyên liệu trong kho."))
                            .suggestedRecipes(alternatives).canAddToCart(false).cartAdded(false).build();
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("👩‍🍳 Nguyên liệu cho món **%s** (Khẩu phần %d người ăn):\n\n",
                        recipe.getTenMonAn(), recipe.getKhauPhan()));

                sb.append("📋 **Danh sách nguyên liệu trong kho:**\n");
                for (RecipeDetailDto.IngredientItemDto item : recipe.getNguyenLieuList()) {
                    sb.append(String.format("- **%s**: %s %s (Đơn giá: %s VND) -> %s\n",
                            item.getTenSP(),
                            item.getSoLuongDinhLuong(),
                            item.getDonViTinh(),
                            CURRENCY_FORMAT.format(item.getDonGia()),
                            item.getConHang() ? "✅ Còn hàng (tồn " + item.getTonKhoHienTai() + ")" : "❌ " + item.getLyDoKhongKhaDung()
                    ));
                }

                sb.append(String.format("\n💵 **Tổng chi phí nguyên liệu ước tính:** %s VND.\n",
                        CURRENCY_FORMAT.format(recipe.getTongTienUocTinh())));
                sb.append(Boolean.TRUE.equals(recipe.getDuNguyenLieuTrongKho())
                        ? "\n💡 **Bạn có muốn thêm các nguyên liệu này vào giỏ hàng không?**"
                        : "\nHiện có nguyên liệu không đủ điều kiện bán, nên chưa thể thêm cả món vào giỏ.");

                return ChatMessageResponse.builder()
                        .reply(sb.toString())
                        .mcpToolInvoked("search_recipe_and_groceries")
                        .recipeSuggested(recipe)
                        .canAddToCart(Boolean.TRUE.equals(recipe.getDuNguyenLieuTrongKho()))
                        .cartAdded(false)
                        .build();
            }
        }

        // Kịch bản 3: Hỏi về khuyến mãi / Flash Sale / Giảm giá / Xả kho
        if (msg.contains("khuyến mãi") || msg.contains("giam gia") || msg.contains("giảm giá") ||
                msg.contains("flash sale") || msg.contains("ưu đãi") || msg.contains("xả kho") || msg.contains("sale")) {
            List<PromotionResponse> activePromos = promotionService.getAllPromotions(true);
            if (!activePromos.isEmpty()) {
                StringBuilder sb = new StringBuilder("🔥 **CÁC CHƯƠNG TRÌNH KHUYẾN MÃI & FLASH SALE ĐANG DIỄN RA:**\n\n");
                for (PromotionResponse promo : activePromos) {
                    PromotionResponse detail = promotionService.getPromotionById(promo.getMaKM());
                    sb.append(String.format("🌟 **%s** (%s)\n", detail.getTenKM(), detail.getLoaiKM()));
                    if (detail.getMoTa() != null && !detail.getMoTa().isBlank()) {
                        sb.append(String.format("   _%s_\n", detail.getMoTa()));
                    }
                    if (detail.getItems() != null && !detail.getItems().isEmpty()) {
                        for (var item : detail.getItems()) {
                            sb.append(String.format("   • **%s**: Giá gốc ~~%s VND~~ -> Ưu đãi **%s VND** (-%.0f%%) (Tồn: %d %s)\n",
                                    item.getTenSP(),
                                    CURRENCY_FORMAT.format(item.getGiaGoc()),
                                    CURRENCY_FORMAT.format(item.getGiaKhuyenMai()),
                                    item.getPhanTramGiam(),
                                    item.getSoLuongTon(),
                                    item.getDonViTinh()));
                        }
                    }
                    sb.append("\n");
                }
                sb.append("💡 Bạn có thể yêu cầu tôi thêm bất kỳ món nào vào giỏ hàng hoặc gợi ý món ngon từ các nguyên liệu giảm giá này nhé!");
                return ChatMessageResponse.builder()
                        .reply(sb.toString())
                        .mcpToolInvoked("promotions_inquiry")
                        .canAddToCart(false)
                        .cartAdded(false)
                        .build();
            }
        }

        // Kịch bản 4: Tìm kiếm sản phẩm thông thường
        McpToolCallResponse searchRes = mcpServerService.executeTool(
                McpToolCallRequest.builder()
                        .name("semantic_product_search")
                        .arguments(Map.of("query", request.getMessage()))
                        .build()
        );

        if (searchRes.getIsSuccess() && searchRes.getContent() instanceof List<?> list && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder("Dạ bách hóa tìm thấy các sản phẩm phù hợp với nhu cầu của bạn:\n\n");
            for (Object obj : list) {
                if (obj instanceof com.ctu.freshgrocery.dto.SemanticProductResponse sp) {
                    String matchBadge = sp.getSimilarityScore() != null ? String.format(" (Độ phù hợp: %.0f%%)", sp.getSimilarityScore() * 100) : "";
                    if (Boolean.TRUE.equals(sp.getDangKhuyenMai())) {
                        sb.append(String.format("- **%s** (%s): 🔥 **%s VND** (~~%s VND~~ -%.0f%% - %s)%s (Còn tồn %d)\n",
                                sp.getTenSP(),
                                sp.getDonViTinh(),
                                CURRENCY_FORMAT.format(sp.getGiaHienTai() != null ? sp.getGiaHienTai() : sp.getGia()),
                                CURRENCY_FORMAT.format(sp.getGiaGoc() != null ? sp.getGiaGoc() : sp.getGia()),
                                sp.getPhanTramGiam() != null ? sp.getPhanTramGiam() : BigDecimal.ZERO,
                                sp.getTenChuongTrinhKM() != null ? sp.getTenChuongTrinhKM() : "Khuyến mãi",
                                matchBadge,
                                sp.getSoLuongTon()));
                    } else {
                        sb.append(String.format("- **%s** (%s): %s VND/đơn vị%s (Còn tồn %d)\n",
                                sp.getTenSP(), sp.getDonViTinh(), CURRENCY_FORMAT.format(sp.getGia()), matchBadge, sp.getSoLuongTon()));
                    }
                } else if (obj instanceof Product p) {
                    sb.append(String.format("- **%s** (%s): %s VND/đơn vị (Còn tồn %d)\n",
                            p.getTenSP(), p.getDonViTinh(), CURRENCY_FORMAT.format(p.getGia()), p.getSoLuongTon()));
                }
            }
            sb.append("\nBạn có muốn tư vấn công thức nấu ăn hoặc tự động thêm các nguyên liệu này vào giỏ hàng không ạ?");
            return ChatMessageResponse.builder()
                    .reply(sb.toString())
                    .mcpToolInvoked("semantic_product_search")
                    .canAddToCart(false)
                    .cartAdded(false)
                    .build();
        }

        // Fallback tự nhiên
        var availableRecipes = recipeService.getAllRecipes().stream()
                .filter(r -> Boolean.TRUE.equals(r.getDuNguyenLieuTrongKho())).limit(4).toList();
        String fallback = availableRecipes.isEmpty() ? "Kho chưa đủ nguyên liệu cho các công thức hiện có. Bạn có thể tìm sản phẩm để mua riêng."
                : "Đây là các món có đủ nguyên liệu còn hạn trong kho hiện tại. Bạn muốn chọn món nào?";
        return ChatMessageResponse.builder()
                .reply(fallback)
                .suggestedRecipes(availableRecipes)
                .mcpToolInvoked(null)
                .canAddToCart(false)
                .cartAdded(false)
                .build();
    }

    private int detectServings(String msg) {
        Pattern p = Pattern.compile("(\\d+)\\s*(người|phần|khẩu phần)");
        Matcher m = p.matcher(msg);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {
            }
        }
        return 4; // Mặc định 4 người
    }
}
