package com.example.orchestrator.service;

import com.example.orchestrator.model.BookingTransaction;

public interface ReservationOrchestrationService {
    boolean reserveSeats(BookingTransaction transaction);
}
