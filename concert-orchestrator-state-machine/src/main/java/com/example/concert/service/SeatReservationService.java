package com.example.concert.service;

import com.example.orchestrator.model.BookingTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeatReservationService {
    public boolean reserveSeats(BookingTransaction transaction) {
        log.info("[ConcertService] Reserving {} seats for concert {}...", 
                transaction.getTicketQuantity(), transaction.getConcertCode());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("[ConcertService] Seats reserved successfully for booking {}.", transaction.getBookingId());
        return true;
    }
}
