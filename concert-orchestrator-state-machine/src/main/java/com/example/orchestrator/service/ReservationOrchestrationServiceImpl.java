package com.example.orchestrator.service;

import com.example.orchestrator.model.BookingTransaction;
import com.example.concert.service.SeatReservationService;
import org.springframework.stereotype.Service;

@Service
public class ReservationOrchestrationServiceImpl implements ReservationOrchestrationService {

    private final SeatReservationService seatReservationService;

    public ReservationOrchestrationServiceImpl(SeatReservationService seatReservationService) {
        this.seatReservationService = seatReservationService;
    }

    @Override
    public boolean reserveSeats(BookingTransaction transaction) {
        return seatReservationService.reserveSeats(transaction);
    }
}
