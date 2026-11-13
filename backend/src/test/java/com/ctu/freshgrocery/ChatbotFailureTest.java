package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.*;
import com.ctu.freshgrocery.repository.RecipeRepository;
import com.ctu.freshgrocery.service.*;
import com.ctu.freshgrocery.service.impl.ChatbotServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatbotFailureTest {
    @Test void failedCartToolMustNotClaimSuccessOrOfferCheckout() {
        var tools = mock(McpServerService.class);
        when(tools.executeTool(any())).thenReturn(McpToolCallResponse.builder()
                .isSuccess(false).errorMessage("Expired ingredients").build());
        var service = new ChatbotServiceImpl(tools, mock(RecipeService.class), mock(PromotionService.class));
        var response = service.processChatMessage(ChatMessageRequest.builder().customerId(1).recipeId(37)
                .message("thêm vào giỏ").build());
        assertFalse(response.getCartAdded());
        assertEquals(0, response.getItemsAddedCount());
        assertNull(response.getCheckoutUrl());
        assertEquals("Expired ingredients", response.getReply());
        verify(tools, times(1)).executeTool(any());
    }
}
