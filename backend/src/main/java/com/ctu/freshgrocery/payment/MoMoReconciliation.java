package com.ctu.freshgrocery.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration @EnableScheduling @RequiredArgsConstructor @Slf4j
public class MoMoReconciliation {
    private final MoMoProperties config;
    private final MoMoStore store;
    private final MoMoService service;
    @Scheduled(initialDelay=60000,fixedDelay=60000)
    public void reconcile(){
        if(!config.ready())return;
        for(var payment:store.awaitingReconciliation()){
            try{service.reconcile(payment);}catch(Exception unavailable){log.warn("MoMo reconciliation pending for {}",payment.paymentId());}
        }
    }
}
