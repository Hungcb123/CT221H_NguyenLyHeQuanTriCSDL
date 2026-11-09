package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.McpToolCallRequest;
import com.ctu.freshgrocery.dto.McpToolCallResponse;
import com.ctu.freshgrocery.dto.McpToolDto;
import com.ctu.freshgrocery.service.McpServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/mcp")
@RequiredArgsConstructor
@Tag(name = "7. Model Context Protocol (MCP Server Tools)", description = "Cổng kết nối chuẩn giao thức MCP (Model Context Protocol) cho các LLM Agent bên ngoài")
public class McpServerController {

    private final McpServerService mcpServerService;

    @GetMapping("/tools")
    @Operation(
            summary = "Liệt kê danh sách công cụ MCP Server (MCP Tool Registry)",
            description = "Trả về danh sách 4 công cụ chuẩn hóa JSON Schema để mọi LLM Agent (Claude, Cursor, Gemini, DeepSeek) có thể hiểu và tự động gọi."
    )
    public ResponseEntity<ApiResponse<List<McpToolDto>>> getAvailableTools() {
        List<McpToolDto> tools = mcpServerService.getAvailableTools();
        return ResponseEntity.ok(ApiResponse.<List<McpToolDto>>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy danh sách công cụ MCP thành công")
                .data(tools)
                .build());
    }

    @PostMapping("/tools/call")
    @Operation(
            summary = "Thực thi công cụ theo giao thức MCP (Tool Execution Endpoint)",
            description = "Nhận lệnh Tool Call từ LLM Model, chuyển tiếp thực thi các tác vụ CSDL (tra cứu món ăn, bỏ giỏ hàng, tìm kiếm thông minh) và phản hồi kết quả chuẩn hóa."
    )
    public ResponseEntity<ApiResponse<McpToolCallResponse>> callTool(@Valid @RequestBody McpToolCallRequest request) {
        McpToolCallResponse response = mcpServerService.executeTool(request);
        return ResponseEntity.ok(ApiResponse.<McpToolCallResponse>builder()
                .success(response.getIsSuccess())
                .statusCode(response.getIsSuccess() ? 1 : -1)
                .message(response.getIsSuccess() ? "Thực thi công cụ MCP thành công" : response.getErrorMessage())
                .data(response)
                .build());
    }
}
