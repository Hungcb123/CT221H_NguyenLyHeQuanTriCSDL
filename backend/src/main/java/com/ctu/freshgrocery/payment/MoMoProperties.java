package com.ctu.freshgrocery.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.ctu.freshgrocery.exception.AppException;
import org.springframework.http.HttpStatus;
import java.net.URI;

@Getter @Setter
@Component
@ConfigurationProperties(prefix="payments.momo")
public class MoMoProperties {
    private boolean enabled = false;
    private String partnerCode = "";
    private String accessKey = "";
    private String secretKey = "";
    private String redirectUrl = "http://localhost:3000/payments/momo/return";
    private String ipnUrl = "";
    private String receiverPhone = "0366643213";
    // This implementation is intentionally sandbox-only. No switch can charge real wallets.
    public static final String GATEWAY = "https://test-payment.momo.vn";

    public boolean ready() {
        return enabled && !partnerCode.isBlank() && !accessKey.isBlank() && !secretKey.isBlank()
                && validUrl(redirectUrl, false) && validUrl(ipnUrl, true);
    }
    public void requireReady() {
        if (!ready()) throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                "MoMo Sandbox chưa được cấu hình khóa đối tác và URL nhận kết quả.");
    }
    private boolean validUrl(String value, boolean publicHttps) {
        try {
            URI url = URI.create(value);
            if (url.getHost() == null || url.getUserInfo() != null || url.getFragment() != null) return false;
            if (publicHttps) return "https".equals(url.getScheme())
                    && !java.util.Set.of("localhost","127.0.0.1","::1","[::1]").contains(url.getHost());
            return "https".equals(url.getScheme()) || ("http".equals(url.getScheme())
                    && java.util.Set.of("localhost","127.0.0.1").contains(url.getHost()));
        } catch (Exception invalid) { return false; }
    }
}
