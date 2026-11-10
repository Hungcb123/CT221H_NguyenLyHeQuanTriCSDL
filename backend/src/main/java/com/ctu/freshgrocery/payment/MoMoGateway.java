package com.ctu.freshgrocery.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ctu.freshgrocery.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
public class MoMoGateway {
    private final MoMoProperties config;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public Map<String,Object> createBody(MoMoPayment p) {
        String raw = "accessKey="+config.getAccessKey()+"&amount="+p.amount()+"&extraData="
                +"&ipnUrl="+config.getIpnUrl()+"&orderId="+p.paymentId()+"&orderInfo="+p.orderInfo()
                +"&partnerCode="+p.partnerCode()+"&redirectUrl="+config.getRedirectUrl()
                +"&requestId="+p.requestId()+"&requestType=captureWallet";
        var body = new LinkedHashMap<String,Object>();
        body.put("partnerCode",p.partnerCode()); body.put("requestId",p.requestId());
        body.put("orderId",p.paymentId()); body.put("amount",p.amount()); body.put("orderInfo",p.orderInfo());
        body.put("redirectUrl",config.getRedirectUrl()); body.put("ipnUrl",config.getIpnUrl());
        body.put("requestType","captureWallet"); body.put("extraData",""); body.put("autoCapture",true);
        body.put("lang","vi"); body.put("signature",MoMoSignature.sign(raw,config.getSecretKey()));
        return body;
    }
    public JsonNode create(MoMoPayment p) { return post("/v2/gateway/api/create", createBody(p)); }
    public JsonNode query(MoMoPayment p) {
        String requestId = UUID.randomUUID().toString();
        String raw = "accessKey="+config.getAccessKey()+"&orderId="+p.paymentId()+"&partnerCode="+p.partnerCode()+"&requestId="+requestId;
        JsonNode reply = post("/v2/gateway/api/query", Map.of("partnerCode",p.partnerCode(),"orderId",p.paymentId(),
                "requestId",requestId,"lang","vi","signature",MoMoSignature.sign(raw,config.getSecretKey())));
        validateIdentity(reply,p,requestId);
        return reply;
    }
    public void validateIdentity(JsonNode reply, MoMoPayment p, String requestId) {
        if (!reply.path("partnerCode").asText().equals(p.partnerCode())
                || !reply.path("orderId").asText().equals(p.paymentId())
                || !reply.path("requestId").asText().equals(requestId))
            throw new AppException(HttpStatus.BAD_GATEWAY,"Phản hồi MoMo không khớp yêu cầu.");
    }
    public boolean safePayUrl(String value) {
        try {
            var uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "test-payment.momo.vn".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && (uri.getPort()==-1 || uri.getPort()==443);
        } catch (Exception invalid) { return false; }
    }
    public String callbackRaw(JsonNode body) {
        StringBuilder raw = new StringBuilder("accessKey="+config.getAccessKey());
        for (String key : List.of("amount","extraData","message","orderId","orderInfo","orderType", "partnerCode",
                "payType","requestId","responseTime","resultCode","transId")) {
            if (!body.hasNonNull(key) || body.get(key).isContainerNode())
                throw new AppException(HttpStatus.BAD_REQUEST,"Thiếu trường kết quả thanh toán.");
            raw.append('&').append(key).append('=').append(body.get(key).asText());
        }
        return raw.toString();
    }
    private JsonNode post(String path, Map<String,Object> body) {
        config.requireReady();
        try {
            var request = HttpRequest.newBuilder(URI.create(MoMoProperties.GATEWAY+path)).timeout(Duration.ofSeconds(35))
                    .header("Content-Type","application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response = http.send(request,HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new AppException(HttpStatus.BAD_GATEWAY,"MoMo chưa phản hồi thành công. Đơn được giữ để đối soát, không tạo lại thanh toán.");
            JsonNode result = json.readTree(response.body());
            if (!result.path("resultCode").isIntegralNumber() || !result.path("resultCode").canConvertToInt())
                throw new AppException(HttpStatus.BAD_GATEWAY,"Phản hồi MoMo không hợp lệ.");
            return result;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.BAD_GATEWAY,"Kết nối MoMo bị gián đoạn. Vui lòng kiểm tra trạng thái đơn.");
        } catch (java.io.IOException failure) {
            throw new AppException(HttpStatus.BAD_GATEWAY,"Chưa kết nối được MoMo. Vui lòng kiểm tra trạng thái đơn trước khi thử lại.");
        }
    }
}
