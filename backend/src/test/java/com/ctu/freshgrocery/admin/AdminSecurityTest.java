package com.ctu.freshgrocery.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=AdminAuthController.class)
@Import({AdminSecurityConfig.class, AdminSecurityTest.Probe.class, AdminSecurityTest.CredentialsConfig.class})
class AdminSecurityTest {
    @Autowired MockMvc mvc;
    @org.springframework.boot.test.context.TestConfiguration static class CredentialsConfig {
        @org.springframework.context.annotation.Bean AdminCredentials credentials(){
            var credentials=org.mockito.Mockito.mock(AdminCredentials.class);
            when(credentials.username()).thenReturn("admin");
            when(credentials.passwordHash()).thenReturn(new BCryptPasswordEncoder(12).encode("test-admin-password"));
            return credentials;
        }
    }
    @RestController static class Probe {
        @GetMapping("/api/v1/admin/probe") String admin(){return "ok";}
        @GetMapping("/api/v1/products") String browse(){return "ok";}
        @PostMapping("/api/v1/products") String write(){return "ok";}
        @GetMapping("/api/v1/benchmark/compare") String compare(){return "ok";}
        @PostMapping("/api/v1/search/sync") String sync(){return "ok";}
        @PostMapping("/api/v1/orders/checkout") String checkout(){return "ok";}
    }
    @Test void sessionAndBrowsingRemainPublic() throws Exception {
        mvc.perform(get("/api/v1/admin/auth/session")).andExpect(status().isOk()).andExpect(jsonPath("$.data.authenticated").value(false)).andExpect(jsonPath("$.data.csrfToken").isNotEmpty());
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/orders/checkout")).andExpect(status().isOk());
    }
    @Test void adminApisRequireRole() throws Exception {
        mvc.perform(get("/api/v1/admin/probe")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/probe").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/probe").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void writesAndLegacyMutatingGetRequireCsrf() throws Exception {
        mvc.perform(post("/api/v1/products").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/products").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/products").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/benchmark/compare").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/benchmark/compare").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/search/sync").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
    }
    @Test void validLoginEstablishesSessionAndLogoutClearsIt() throws Exception {
        var response=mvc.perform(post("/api/v1/admin/auth/login").with(csrf()).param("username","admin").param("password","test-admin-password"))
                .andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)response.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        mvc.perform(get("/api/v1/admin/auth/session").session(session)).andExpect(jsonPath("$.data.authenticated").value(true));
        mvc.perform(post("/api/v1/admin/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }
    @Test void loginAndLogoutRejectMissingToken() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/login").param("username","admin").param("password","wrong")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/auth/login").with(csrf()).param("username","admin").param("password","wrong")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/auth/logout").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/auth/logout").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
    }
}
