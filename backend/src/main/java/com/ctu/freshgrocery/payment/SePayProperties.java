package com.ctu.freshgrocery.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.ctu.freshgrocery.exception.AppException;
import org.springframework.http.HttpStatus;

@Getter @Setter @Component
@ConfigurationProperties(prefix="payments.sepay")
public class SePayProperties {
    private boolean enabled;
    private String bankCode = "";
    private String accountNumber = "";
    private String accountName = "";
    private String webhookApiKey = "";
    public boolean ready() {
        return enabled && bankCode.matches("[A-Za-z0-9]{2,50}")
                && accountNumber.matches("[A-Za-z0-9]{1,100}") && !accountName.isBlank()
                && webhookApiKey.length() >= 32;
    }
    public void requireReady() {
        if(!ready()) throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,"SePay chưa được cấu hình tài khoản nhận tiền và khóa webhook.");
    }
}
