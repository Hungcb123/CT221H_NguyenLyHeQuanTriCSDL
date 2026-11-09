package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.ChatMessageRequest;
import com.ctu.freshgrocery.dto.ChatMessageResponse;

public interface ChatbotService {
    ChatMessageResponse processChatMessage(ChatMessageRequest request);
}
