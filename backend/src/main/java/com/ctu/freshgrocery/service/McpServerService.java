package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.McpToolCallRequest;
import com.ctu.freshgrocery.dto.McpToolCallResponse;
import com.ctu.freshgrocery.dto.McpToolDto;

import java.util.List;

public interface McpServerService {
    List<McpToolDto> getAvailableTools();
    McpToolCallResponse executeTool(McpToolCallRequest request);
}
