package com.example.orchestrator.service;

import com.example.orchestrator.model.BookingTransaction;

public interface PaymentOrchestrationService {
    boolean processPayment(BookingTransaction transaction);
    void refund(BookingTransaction transaction);
}
