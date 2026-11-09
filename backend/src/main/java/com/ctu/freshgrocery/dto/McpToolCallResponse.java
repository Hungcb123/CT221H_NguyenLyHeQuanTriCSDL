package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả phản hồi thực thi công cụ MCP")
public class McpToolCallResponse {

    @Schema(description = "Tên công cụ vừa thực thi", example = "search_recipe_and_groceries")
    private String toolName;

    @Schema(description = "Trạng thái thực thi công cụ thành công hay thất bại", example = "true")
    private Boolean isSuccess;

    @Schema(description = "Dữ liệu kết quả trả về từ công cụ")
    private Object content;

    @Schema(description = "Thông báo lỗi (nếu có)")
    private String errorMessage;
}
