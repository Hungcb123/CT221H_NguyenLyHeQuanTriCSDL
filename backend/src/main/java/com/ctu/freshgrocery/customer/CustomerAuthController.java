package com.ctu.freshgrocery.customer;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.exception.AppException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/auth")
public class CustomerAuthController {
    private final JdbcTemplate jdbc;
    private final CustomerAccess access;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @PostConstruct void initialize() {
        jdbc.execute("""
            IF OBJECT_ID('KhachHangDangNhap') IS NULL
            CREATE TABLE KhachHangDangNhap (
                MaKH INT PRIMARY KEY REFERENCES KhachHang(MaKH),
                MatKhauHash VARCHAR(100) NOT NULL
            );
            """);
        // Existing course-demo accounts only; never change an existing password.
        String hash = encoder.encode("DailyMart123!");
        jdbc.update("""
            INSERT INTO KhachHangDangNhap(MaKH, MatKhauHash)
            SELECT MaKH, ? FROM KhachHang kh
            WHERE Email IN ('nguyenvanan@ctu.edu.vn','tranthibinh@ctu.edu.vn','levancuong@ctu.edu.vn')
              AND NOT EXISTS (SELECT 1 FROM KhachHangDangNhap dn WHERE dn.MaKH=kh.MaKH)
            """, hash);
    }

    private Map<String,Object> state() {
        var result = new LinkedHashMap<String,Object>();
        result.put("csrfToken", access.csrf());
        Integer id = access.optionalId();
        var rows = id == null ? List.<Map<String,Object>>of() : jdbc.queryForList(
                "SELECT MaKH AS maKH, HoTen AS hoTen, Email AS email, DiaChi AS diaChi FROM KhachHang WHERE MaKH=?", id);
        result.put("customer", rows.isEmpty() ? null : rows.getFirst());
        return result;
    }
    @GetMapping("/session") public ApiResponse<?> session() { return ApiResponse.ok("Phiên khách hàng", state()); }

    public record Register(
            @NotBlank @Size(min=2, max=100, message="Họ tên từ 2 đến 100 ký tự.") String hoTen,
            @NotBlank @Email(message="Email không hợp lệ.") @Size(max=100) String email,
            @NotBlank @Pattern(regexp="0[0-9]{9}", message="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.") String soDienThoai,
            @Size(max=255, message="Địa chỉ tối đa 255 ký tự.") String diaChi,
            @NotBlank @Size(min=8, max=72, message="Mật khẩu từ 8 đến 72 ký tự.") String password) {
        public Register {
            hoTen = hoTen == null ? null : hoTen.trim();
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
            soDienThoai = soDienThoai == null ? null : soDienThoai.trim();
            diaChi = diaChi == null ? "" : diaChi.trim();
        }
    }

    @PostMapping("/register")
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<?> register(@Valid @RequestBody Register registration, HttpServletRequest request) {
        access.verifyCsrf();
        if (access.optionalId() != null)
            throw new AppException(HttpStatus.CONFLICT, "Bạn đang đăng nhập. Hãy đăng xuất trước khi tạo tài khoản khác.");
        if (registration.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8. Hãy dùng mật khẩu ngắn hơn.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM KhachHang WHERE Email=?", Integer.class, registration.email()) > 0)
            throw new AppException(HttpStatus.CONFLICT, "Email này đã được đăng ký. Bạn có thể đăng nhập bằng email đó.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM KhachHang WHERE SoDienThoai=?", Integer.class, registration.soDienThoai()) > 0)
            throw new AppException(HttpStatus.CONFLICT, "Số điện thoại này đã được đăng ký.");
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                var statement = connection.prepareStatement("""
                    INSERT INTO KhachHang(HoTen,Email,SoDienThoai,DiaChi,SoDuVi,NgayTao)
                    VALUES(?,?,?,?,0,GETDATE())
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
                statement.setNString(1, registration.hoTen());
                statement.setString(2, registration.email());
                statement.setString(3, registration.soDienThoai());
                statement.setNString(4, registration.diaChi());
                return statement;
            }, key);
            int id = Objects.requireNonNull(key.getKey()).intValue();
            jdbc.update("INSERT INTO KhachHangDangNhap(MaKH,MatKhauHash) VALUES(?,?)", id, encoder.encode(registration.password()));
            jdbc.update("INSERT INTO GioHang(MaKH,NgayCapNhat) VALUES(?,GETDATE())", id);
            request.changeSessionId();
            request.getSession().setAttribute(CustomerAccess.ID, id);
        } catch (org.springframework.dao.DuplicateKeyException conflict) {
            throw new AppException(HttpStatus.CONFLICT, "Email hoặc số điện thoại đã được đăng ký.");
        }
        return ApiResponse.ok("Tạo tài khoản thành công", state());
    }

    public record Login(@NotBlank @Email String email, @NotBlank @Size(max=200) String password) {}
    @PostMapping("/login") public ApiResponse<?> login(@Valid @RequestBody Login login, HttpServletRequest request) {
        access.verifyCsrf();
        var rows = jdbc.queryForList("""
            SELECT kh.MaKH, dn.MatKhauHash FROM KhachHang kh
            JOIN KhachHangDangNhap dn ON dn.MaKH=kh.MaKH WHERE kh.Email=?
            """, login.email().trim());
        if (rows.isEmpty() || !encoder.matches(login.password(), (String) rows.getFirst().get("MatKhauHash")))
            throw new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
        request.changeSessionId();
        request.getSession().setAttribute(CustomerAccess.ID, ((Number)rows.getFirst().get("MaKH")).intValue());
        return ApiResponse.ok("Đăng nhập thành công", state());
    }
    @PostMapping("/logout") public ApiResponse<?> logout(HttpServletRequest request) {
        access.verifyCsrf();
        request.getSession().removeAttribute(CustomerAccess.ID);
        request.getSession().removeAttribute("customerCsrf");
        request.changeSessionId();
        return ApiResponse.ok("Đã đăng xuất", state());
    }
}
