package com.ctu.freshgrocery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FreshGroceryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshGroceryApplication.class, args);
        System.out.println("""
            ========================================================================
            🥬 HỆ THỐNG BÁCH HÓA THỰC PHẨM TƯƠI SỐNG (BÁCH HÓA XANH) - CT221H SẴN SÀNG!
            👉 Swagger UI Test API: http://localhost:8080/swagger-ui.html
            👉 OpenAPI Docs:       http://localhost:8080/v3/api-docs
            ========================================================================
            """);
    }
}
