package com.ctu.freshgrocery.customer;

import com.ctu.freshgrocery.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerAccess {
    public static final String ID = "dailyMartCustomerId";
    private final HttpServletRequest request;
    public Integer optionalId() {
        var session = request.getSession(false);
        return session == null ? null : (Integer) session.getAttribute(ID);
    }
    public String csrf() {
        var session = request.getSession();
        if (session.getAttribute("customerCsrf") == null) session.setAttribute("customerCsrf", UUID.randomUUID().toString());
        return (String) session.getAttribute("customerCsrf");
    }
    public void verifyCsrf() {
        if (!csrf().equals(request.getHeader("X-Customer-CSRF")))
            throw new AppException(HttpStatus.FORBIDDEN, "Phiên đã thay đổi. Vui lòng tải lại trang.");
    }
    public int requireId() {
        Integer id = optionalId();
        if (id == null) throw new AppException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập tài khoản khách hàng.");
        if (!java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) verifyCsrf();
        return id;
    }
    public void owner(Integer id) {
        if (!Integer.valueOf(requireId()).equals(id))
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không được truy cập dữ liệu của khách hàng khác.");
    }
}
