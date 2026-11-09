package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mô tả công cụ chuẩn giao thức Model Context Protocol (MCP Tool Specification)")
public class McpToolDto {

    @Schema(description = "Tên định danh công cụ", example = "search_recipe_and_groceries")
    private String name;

    @Schema(description = "Mô tả mục đích và cách sử dụng công cụ cho LLM", example = "Tìm kiếm công thức món ăn và kiểm tra sự sẵn có của các nguyên liệu tươi sống trong kho bách hóa.")
    private String description;

    @Schema(description = "JSON Schema của các tham số đầu vào")
    private Map<String, Object> inputSchema;
}
