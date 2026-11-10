package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MoMoSecurityTest {
    MoMoProperties config=new MoMoProperties();
    ObjectMapper json=new ObjectMapper();
    MoMoGateway gateway=new MoMoGateway(config,json);
    MoMoStore store=mock(MoMoStore.class);
    MoMoService service=new MoMoService(config,gateway,store);
    MoMoPayment payment=new MoMoPayment("DM-test",42,7,"req-1","partner",125000,"DailyMart sandbox - Don #42","PENDING",null,null);
    MoMoSecurityTest(){config.setEnabled(true);config.setPartnerCode("partner");config.setAccessKey("access");config.setSecretKey("secret");config.setIpnUrl("https://merchant.example/ipn");when(store.get("DM-test")).thenReturn(payment);}

    ObjectNode callback(){
        var body=json.createObjectNode();body.put("amount",125000);body.put("extraData","");body.put("message","Thanh cong");
        body.put("orderId","DM-test");body.put("orderInfo",payment.orderInfo());body.put("orderType","momo_wallet");
        body.put("partnerCode","partner");body.put("payType","qr");body.put("requestId","req-1");
        body.put("responseTime",1700000000000L);body.put("resultCode",0);body.put("transId",98765L);
        return signed(body);
    }
    ObjectNode signed(ObjectNode body){body.put("signature",MoMoSignature.sign(gateway.callbackRaw(body),"secret"));return body;}

    @Test void hmacMatchesKnownReference(){
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",MoMoSignature.sign("The quick brown fox jumps over the lazy dog","key"));
        assertFalse(MoMoSignature.verify("abc","0".repeat(64),"secret"));
    }
    @Test void createSignatureUsesExactOfficialFieldOrder(){
        var body=gateway.createBody(payment);
        String raw="accessKey=access&amount=125000&extraData=&ipnUrl=https://merchant.example/ipn&orderId=DM-test&orderInfo=DailyMart sandbox - Don #42&partnerCode=partner&redirectUrl=http://localhost:3000/payments/momo/return&requestId=req-1&requestType=captureWallet";
        assertEquals(MoMoSignature.sign(raw,"secret"),body.get("signature"));
        assertEquals(true,body.get("autoCapture"));assertEquals(125000L,body.get("amount"));
    }
    @Test void validIpnAppliesPayment(){
        service.notification(callback());
        verify(store).apply("DM-test",125000,0,"98765","Thanh cong");
    }
    @Test void unsignedOrTamperedNotificationCannotChangeAnything(){
        var body=callback();body.put("amount",1);
        assertThrows(AppException.class,()->service.notification(body));
        verifyNoInteractions(store);
        body.remove("signature");assertThrows(AppException.class,()->service.notification(body));
    }
    @Test void signedWrongAmountOrRequestIdentityIsRejected(){
        var wrongAmount=callback();wrongAmount.put("amount",99999);signed(wrongAmount);
        assertThrows(AppException.class,()->service.notification(wrongAmount));
        var mismatched=callback();mismatched.put("requestId","other-request");signed(mismatched);
        assertThrows(AppException.class,()->service.notification(mismatched));
        verify(store,never()).apply(anyString(),anyLong(),anyInt(),any(),any());
    }
    @Test void malformedAmountAndMissingFieldsAreRejected(){
        var body=callback();body.put("amount",125000.5);signed(body);
        assertThrows(AppException.class,()->service.notification(body));
        var missing=callback();missing.remove("orderType");assertThrows(AppException.class,()->service.notification(missing));
        verify(store,never()).apply(anyString(),anyLong(),anyInt(),any(),any());
    }
    @Test void clientCannotLoadAnotherCustomersPayment(){
        assertThrows(AppException.class,()->service.status("DM-test",8));
    }
    @Test void missingConfigurationDoesNotCreateAnOrder(){
        config.setSecretKey("");
        assertFalse(config.ready());
        assertThrows(AppException.class,()->service.start(7,"Can Tho","","request-id-123456789"));
        verifyNoInteractions(store);
    }
    @Test void onlySandboxPaymentLinksAreAllowed(){
        assertTrue(gateway.safePayUrl("https://test-payment.momo.vn/v2/gateway/pay?x=1"));
        assertFalse(gateway.safePayUrl("https://payment.momo.vn/pay"));
        assertFalse(gateway.safePayUrl("https://test-payment.momo.vn.evil.example/pay"));
        assertFalse(gateway.safePayUrl("javascript:alert(1)"));
        assertFalse(gateway.safePayUrl("https://evil@test-payment.momo.vn/pay"));
    }
    @Test void pendingAndAuthorizedCodesAreNotFailures(){
        for(int code:new int[]{0,1000,7000,7002,9000,99})assertFalse(MoMoStore.terminalFailure(code));
        assertTrue(MoMoStore.terminalFailure(1006));assertTrue(MoMoStore.terminalFailure(1005));
    }
}
