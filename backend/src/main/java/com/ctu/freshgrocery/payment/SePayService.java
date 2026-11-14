package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.exception.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor
public class SePayService {
    private final SePayProperties config;
    private final SePayStore store;
    private static final Pattern CODE = Pattern.compile("(?i)(?<![a-z0-9])DM[0-9A-F]{32}(?![a-z0-9])");

    public Map<String,Object> availability() {
        return Map.of("available",config.ready(),"message", config.ready()
                ? "Quét QR bằng ứng dụng ngân hàng. Tự động xác nhận khi nhận tiền."
                : "Chưa cấu hình tài khoản nhận thanh toán SePay.");
    }
    public Map<String,Object> start(int customer,String address,String note,String key) {
        config.requireReady();
        return view(store.reserve(customer,address,note,key,config.getAccountNumber(),config.getBankCode(),config.getAccountName()));
    }
    public Map<String,Object> status(String id,int customer) {
        var p=store.get(id);
        if(p.maKH()!=customer)throw new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy thanh toán SePay.");
        return view(p);
    }
    public Map<String,Object> forOrder(int order,int customer) {
        return store.forOrder(order,customer).map(this::view).orElse(Map.of());
    }
    public void authenticate(String authorization) {
        config.requireReady();
        byte[] expected=("Apikey "+config.getWebhookApiKey()).getBytes(StandardCharsets.UTF_8);
        if(authorization==null || !MessageDigest.isEqual(expected,authorization.getBytes(StandardCharsets.UTF_8)))
            throw new AppException(HttpStatus.UNAUTHORIZED,"Khóa xác thực webhook không hợp lệ.");
    }
    static String paymentCode(JsonNode body) {
        var codes=new HashSet<String>();
        for(String field:List.of("code","content")) {
            var matcher=CODE.matcher(body.path(field).asText());
            while(matcher.find())codes.add(matcher.group().toUpperCase(Locale.ROOT));
        }
        return codes.size()==1?codes.iterator().next():null;
    }
    @Transactional
    public void notification(String authorization,JsonNode body) {
        authenticate(authorization);
        String transaction=body.path("id").asText();
        if(!transaction.matches("[1-9][0-9]{0,18}"))
            throw new AppException(HttpStatus.BAD_REQUEST,"Thiếu mã giao dịch SePay hợp lệ.");
        // Range lock and primary key prevent concurrent duplicate deliveries from applying twice.
        if(store.hasWebhook(transaction))return;
        String outcome="IGNORED";
        String id=paymentCode(body);
        if("in".equals(body.path("transferType").asText()) && id!=null) {
            SePayPayment p=null;
            try {p=store.get(id);} catch(AppException missing) {
                if(missing.getStatus()!=HttpStatus.NOT_FOUND)throw missing;
            }
            if(p!=null) {
                if(!p.accountNumber().equals(body.path("accountNumber").asText()))outcome="WRONG_ACCOUNT";
                else if(!body.path("transferAmount").isNumber()
                        || body.path("transferAmount").decimalValue().compareTo(java.math.BigDecimal.valueOf(p.amount()))!=0)outcome="WRONG_AMOUNT";
                else if("PAID".equals(p.status()) && !transaction.equals(p.transId()))outcome="EXTRA_PAYMENT";
                else {
                    store.settle(id,p.amount(),transaction);
                    outcome="MATCHED";
                }
            }
        }
        store.recordWebhook(transaction,body.toString(),outcome);
    }
    private Map<String,Object> view(SePayPayment p) {
        var result=new LinkedHashMap<String,Object>();
        result.put("paymentId",p.paymentId());result.put("maDH",p.maDH());result.put("amount",p.amount());
        result.put("status",p.status());result.put("expiresAt",p.expiresAt());result.put("bankCode",p.bankCode());result.put("accountNumber",p.accountNumber());
        result.put("accountName",p.accountName());result.put("transferContent",p.paymentId());
        result.put("qrUrl",p.pending()?"https://vietqr.app/img?bank="+encode(p.bankCode())+"&acc="+encode(p.accountNumber())
                +"&amount="+p.amount()+"&des="+encode(p.paymentId())+"&template=compact":null);
        result.put("message",switch(p.status()) {
            case "PAID" -> "Đã nhận thanh toán qua SePay.";
            case "FAILED" -> "Đã hết 30 phút giữ hàng. Không chuyển tiền cho mã này. Nếu đã chuyển, liên hệ cửa hàng để đối soát.";
            case "REVIEW" -> "Đã nhận tiền nhưng đơn cần đối soát. Liên hệ cửa hàng và không chuyển thêm tiền.";
            default -> "Quét QR hoặc chuyển đúng số tiền và nội dung bên dưới trong 30 phút từ lúc tạo đơn. Nếu đã chuyển, hãy chờ xác nhận và không chuyển lại.";
        });
        return result;
    }
    private static String encode(String s) {return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
