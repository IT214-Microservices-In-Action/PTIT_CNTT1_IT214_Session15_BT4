package com.example.orchestrator.service;

import com.example.orchestrator.model.BookingTransaction;
import com.example.payment.service.PaymentProcessingService;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrchestrationServiceImpl implements PaymentOrchestrationService {

    private final PaymentProcessingService paymentProcessingService;

    public PaymentOrchestrationServiceImpl(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @Override
    public boolean processPayment(BookingTransaction transaction) {
        return paymentProcessingService.processPayment(transaction);
    }

    @Override
    public void refund(BookingTransaction transaction) {
        paymentProcessingService.refund(transaction);
    }
}
