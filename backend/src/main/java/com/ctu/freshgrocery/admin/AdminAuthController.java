package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    @GetMapping("/session")
    public ApiResponse<?> session(Authentication auth, HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        boolean authenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        return ApiResponse.ok("Trạng thái phiên quản trị", Map.of(
                "authenticated", authenticated, "username", authenticated ? auth.getName() : "",
                "csrfToken", token.getToken(), "csrfHeader", token.getHeaderName()));
    }
}
