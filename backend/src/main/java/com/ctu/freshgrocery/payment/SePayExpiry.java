package com.ctu.freshgrocery.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration @EnableScheduling @RequiredArgsConstructor @Slf4j
public class SePayExpiry {
    private final SePayStore store;
    @Scheduled(initialDelay=60000,fixedDelay=60000)
    public void expire() {
        for(var p:store.expired()) {
            try {store.expire(p.paymentId());}
            catch(Exception failure) {log.warn("Chưa hết hạn được thanh toán SePay {}",p.paymentId());}
        }
    }
}
