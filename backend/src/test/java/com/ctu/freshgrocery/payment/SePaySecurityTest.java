package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SePaySecurityTest {
    final String id="DM0123456789ABCDEF0123456789ABCDEF";
    final String key="local-test-key-"+"x".repeat(32);
    SePayProperties config=new SePayProperties();
    SePayStore store=mock(SePayStore.class);
    SePayService service=new SePayService(config,store);
    SePayPayment payment=new SePayPayment(id,42,7,"123456789",125000,"PENDING",null,"MBBank","NGUYEN VAN A",LocalDateTime.now().plusMinutes(30));
    SePaySecurityTest() {
        config.setEnabled(true);config.setBankCode("MBBank");config.setAccountNumber("123456789");
        config.setAccountName("NGUYEN VAN A");config.setWebhookApiKey(key);
        when(store.get(id)).thenReturn(payment);
    }
    ObjectNode callback() {
        return new ObjectMapper().createObjectNode().put("id",98765).put("accountNumber","123456789")
                .put("content","Thanh toan "+id).put("transferType","in").put("transferAmount",125000);
    }
    void send(ObjectNode body) {service.notification("Apikey "+key,body);}
    @Test void validPaymentSettlesAndRecordsDelivery() {
        send(callback());verify(store).settle(id,125000,"98765");
        verify(store).recordWebhook(eq("98765"),anyString(),eq("MATCHED"));
    }
    @Test void invalidAuthorizationCannotAccessPayments() {
        for(String value:new String[]{null,"","Bearer "+key,"Apikey wrong"})
            assertThrows(AppException.class,()->service.notification(value,callback()));
        verifyNoInteractions(store);
    }
    @Test void duplicateDeliveryIsAcknowledgedWithoutApplyingAgain() {
        when(store.hasWebhook("98765")).thenReturn(true);send(callback());
        verify(store,never()).settle(anyString(),anyLong(),anyString());
        verify(store,never()).recordWebhook(anyString(),anyString(),anyString());
    }
    @Test void outgoingOrWrongAccountCannotPayOrder() {
        var out=callback().put("transferType","out");send(out);
        send(callback().put("accountNumber","other"));
        verify(store,never()).settle(anyString(),anyLong(),anyString());
        verify(store).recordWebhook(eq("98765"),anyString(),eq("WRONG_ACCOUNT"));
    }
    @Test void wrongOrFractionalAmountIsRecordedWithoutPayment() {
        for(double amount:new double[]{0,124999,125001,125000.5})send(callback().put("transferAmount",amount));
        var missing=callback();missing.remove("transferAmount");send(missing);
        verify(store,never()).settle(anyString(),anyLong(),anyString());
    }
    @Test void numericStringAmountIsNotTrusted() {
        send(callback().put("transferAmount","125000"));
        verify(store,never()).settle(anyString(),anyLong(),anyString());
    }
    @Test void ambiguousOrPartialCodesCannotMatchAnOrder() {
        var body=callback().put("code","DMFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");assertNull(SePayService.paymentCode(body));
        body=callback().put("content","prefix"+id);assertNull(SePayService.paymentCode(body));
        body=callback().put("content",id+"F");assertNull(SePayService.paymentCode(body));
        assertEquals(id,SePayService.paymentCode(callback().put("code",id.toLowerCase())));
    }
    @Test void missingOrInvalidTransactionIdIsRejected() {
        var body=callback();body.remove("id");assertThrows(AppException.class,()->send(body));
        assertThrows(AppException.class,()->send(callback().put("id",-1)));
        verifyNoInteractions(store);
    }
    @Test void anotherCustomerCannotReadPayment() {
        assertThrows(AppException.class,()->service.status(id,8));
        assertEquals(42,service.status(id,7).get("maDH"));
    }
    @Test void missingConfigurationCannotReserveInventory() {
        config.setWebhookApiKey("");assertFalse(config.ready());
        assertThrows(AppException.class,()->service.start(7,"Can Tho","","request-key-123456"));verifyNoInteractions(store);
    }
    @Test void extraTransferIsLoggedWithoutChangingPaidOrder() {
        when(store.get(id)).thenReturn(new SePayPayment(id,42,7,"123456789",125000,"PAID","123","MBBank","NGUYEN VAN A",payment.expiresAt()));
        send(callback());verify(store,never()).settle(anyString(),anyLong(),anyString());
        verify(store).recordWebhook(eq("98765"),anyString(),eq("EXTRA_PAYMENT"));
        assertNull(service.status(id,7).get("qrUrl"));
    }
}
