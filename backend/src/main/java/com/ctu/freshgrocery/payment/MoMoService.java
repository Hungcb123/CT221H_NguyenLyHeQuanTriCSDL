package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.exception.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MoMoService {
    private final MoMoProperties config;
    private final MoMoGateway gateway;
    private final MoMoStore store;

    public Map<String,Object> availability(){return Map.of("available",config.ready(),"sandbox",true,
            "message",config.ready()?"MoMo Sandbox — chỉ thử nghiệm, không dùng tiền thật.":"MoMo Sandbox chưa được cấu hình.");}
    public Map<String,Object> start(int customer,String address,String note,String key){
        config.requireReady();
        var payment=store.reserve(customer,address,note,key,config.getPartnerCode());
        if(payment.pending() && payment.payUrl()==null){
            try{
                JsonNode result=gateway.create(payment);
                gateway.validateIdentity(result,payment,payment.requestId());
                int code=result.path("resultCode").intValue();
                if(code==0){
                    if(amount(result)!=payment.amount())throw new AppException(HttpStatus.BAD_GATEWAY,"MoMo trả số tiền không khớp.");
                    String url=result.path("payUrl").asText();
                    if(!gateway.safePayUrl(url))throw new AppException(HttpStatus.BAD_GATEWAY,"Liên kết thanh toán MoMo không hợp lệ.");
                    store.setPayUrl(payment.paymentId(),url);
                }else{
                    if(MoMoStore.terminalFailure(code))store.apply(payment.paymentId(),payment.amount(),code,null,result.path("message").asText());
                    else store.note(payment.paymentId(),"MoMo chưa tạo được liên kết. Vui lòng kiểm tra trạng thái hoặc liên hệ cửa hàng.");
                }
            }catch(AppException uncertain){
                store.note(payment.paymentId(),"Chưa xác nhận được việc tạo thanh toán. Đơn được giữ để đối soát; không trả tiền lần nữa.");
            }
        }
        return view(store.get(payment.paymentId()));
    }
    public Map<String,Object> status(String id,int customer){return view(owned(id,customer));}
    public Map<String,Object> forOrder(int order,int customer){return store.forOrder(order,customer).map(this::view).orElse(Map.of());}
    public Map<String,Object> refresh(String id,int customer){
        var payment=owned(id,customer);
        reconcile(payment);
        return view(store.get(id));
    }
    public void reconcile(MoMoPayment payment){
        if(!config.ready() || !payment.pending() || !store.claimQuery(payment.paymentId()))return;
        if(!config.getPartnerCode().equals(payment.partnerCode()))return;
        JsonNode result=gateway.query(payment);
        store.apply(payment.paymentId(),amount(result),result.path("resultCode").intValue(),result.path("transId").asText(),result.path("message").asText());
    }
    public void notification(JsonNode body){
        config.requireReady();
        String raw=gateway.callbackRaw(body);
        if(!MoMoSignature.verify(raw,body.path("signature").asText(),config.getSecretKey()))
            throw new AppException(HttpStatus.UNAUTHORIZED,"Chữ ký MoMo không hợp lệ.");
        var payment=store.get(body.path("orderId").asText());
        gateway.validateIdentity(body,payment,payment.requestId());
        if(!config.getPartnerCode().equals(payment.partnerCode()) || !body.path("orderInfo").asText().equals(payment.orderInfo())
                || !body.path("extraData").asText().isEmpty() || !body.path("resultCode").isIntegralNumber()
                || !body.path("resultCode").canConvertToInt() || amount(body)!=payment.amount())
            throw new AppException(HttpStatus.BAD_REQUEST,"Thông tin thanh toán MoMo không khớp.");
        store.apply(payment.paymentId(),amount(body),body.path("resultCode").intValue(),body.path("transId").asText(),body.path("message").asText());
    }
    private long amount(JsonNode result){
        String value=result.path("amount").asText();
        if(!value.matches("[0-9]{1,12}"))throw new AppException(HttpStatus.BAD_REQUEST,"Số tiền MoMo không hợp lệ.");
        return Long.parseLong(value);
    }
    private MoMoPayment owned(String id,int customer){
        var payment=store.get(id);
        if(payment.maKH()!=customer)throw new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy thanh toán MoMo.");
        return payment;
    }
    private Map<String,Object> view(MoMoPayment payment){
        var result=new LinkedHashMap<String,Object>();
        result.put("paymentId",payment.paymentId());result.put("maDH",payment.maDH());result.put("amount",payment.amount());
        result.put("status",payment.status());result.put("sandbox",true);
        result.put("payUrl",payment.pending() && gateway.safePayUrl(payment.payUrl())?payment.payUrl():null);
        result.put("message",switch(payment.status()){
            case "PAID"->"Thanh toán thử nghiệm đã được MoMo xác nhận.";
            case "FAILED"->"Thanh toán thất bại hoặc đã hủy. Hàng giữ chỗ đã được trả về kho, sản phẩm được đưa lại giỏ hàng.";
            case "REVIEW"->"Giao dịch cần đối soát. Vui lòng liên hệ cửa hàng và không thanh toán lại.";
            default->"Đang chờ MoMo xác nhận. Không thanh toán lại nếu bạn đã trả tiền.";
        });
        return result;
    }
}
