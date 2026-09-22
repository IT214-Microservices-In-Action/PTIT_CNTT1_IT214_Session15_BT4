package com.example.payment.service;

import com.example.orchestrator.model.BookingTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentProcessingService {
    public boolean processPayment(BookingTransaction transaction) {
        log.info("[PaymentService] Processing payment of {} for customer {}...", 
                transaction.getAmount(), transaction.getCustomerId());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("[PaymentService] Payment successful for booking {}.", transaction.getBookingId());
        return true;
    }

    public void refund(BookingTransaction transaction) {
        log.info("[PaymentService] Refunding {} for booking {}.", transaction.getAmount(), transaction.getBookingId());
    }
}
