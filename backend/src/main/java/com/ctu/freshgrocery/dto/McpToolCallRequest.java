package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thực thi công cụ theo giao thức MCP (Model Context Protocol)")
public class McpToolCallRequest {

    @NotBlank(message = "Tên công cụ (tool name) không được để trống")
    @Schema(description = "Tên công cụ cần gọi", example = "search_recipe_and_groceries")
    private String name;

    @Schema(description = "Các đối số truyền vào công cụ dưới dạng JSON Map")
    private Map<String, Object> arguments;
}
