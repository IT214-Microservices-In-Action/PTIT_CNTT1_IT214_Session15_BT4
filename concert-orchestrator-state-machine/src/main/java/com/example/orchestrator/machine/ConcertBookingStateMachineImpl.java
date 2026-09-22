package com.example.orchestrator.machine;

import com.example.orchestrator.config.RetryPolicy;
import com.example.orchestrator.listener.StateChangeListener;
import com.example.orchestrator.model.BookingEvent;
import com.example.orchestrator.model.BookingState;
import com.example.orchestrator.model.BookingTransaction;
import com.example.orchestrator.service.PaymentOrchestrationService;
import com.example.orchestrator.service.ReservationOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ConcertBookingStateMachineImpl implements ConcertBookingStateMachine {
    
    private final PaymentOrchestrationService paymentService;
    private final ReservationOrchestrationService reservationService;
    private final StateChangeListener stateChangeListener;
    private final RetryPolicy retryPolicy;
    
    private Map<String, BookingState> transactionStates = new ConcurrentHashMap<>();
    
    public ConcertBookingStateMachineImpl(PaymentOrchestrationService paymentService,
                                          ReservationOrchestrationService reservationService,
                                          StateChangeListener stateChangeListener) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        this.stateChangeListener = stateChangeListener;
        this.retryPolicy = new RetryPolicy(3, 2000);
    }
    
    @Override
    public void process(BookingTransaction transaction) {
        String bookingId = transaction.getBookingId();
        BookingState currentState = transaction.getCurrentState();
        
        switch (currentState) {
            case INITIATED:
                transition(bookingId, BookingState.PAYMENT_PENDING, BookingEvent.PROCESS_PAYMENT);
                transaction.setCurrentState(BookingState.PAYMENT_PENDING);
                processPayment(transaction);
                break;
                
            case PAYMENT_PENDING:
                break;
                
            case PAYMENT_COMPLETED:
                transition(bookingId, BookingState.SEAT_RESERVING, BookingEvent.RESERVE_SEATS);
                transaction.setCurrentState(BookingState.SEAT_RESERVING);
                processReservation(transaction);
                break;
                
            case SEAT_RESERVING:
                break;
                
            case BOOKING_CONFIRMED:
                log.info("[Orchestrator] Final State: BOOKING_CONFIRMED for booking {}", bookingId);
                break;
                
            case CANCELLED:
                log.info("[Orchestrator] Transaction {} is CANCELLED", bookingId);
                break;
                
            default:
                log.error("Unknown state: {}", currentState);
        }
    }
    
    private void transition(String bookingId, BookingState newState, BookingEvent event) {
        BookingState oldState = transactionStates.getOrDefault(bookingId, BookingState.INITIATED);
        transactionStates.put(bookingId, newState);
        stateChangeListener.onStateChanged(bookingId, oldState, newState, event);
        log.info("[Orchestrator] State: {} -> Event: {} -> New State: {}", 
                 oldState, event, newState);
    }
    
    private void processPayment(BookingTransaction transaction) {
        String bookingId = transaction.getBookingId();
        for (int attempt = 1; attempt <= retryPolicy.getMaxAttempts(); attempt++) {
            try {
                log.info("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt {}/{}", 
                         attempt, retryPolicy.getMaxAttempts());
                boolean success = paymentService.processPayment(transaction);
                if (success) {
                    transition(bookingId, BookingState.PAYMENT_COMPLETED, BookingEvent.PAYMENT_SUCCESS);
                    transaction.setCurrentState(BookingState.PAYMENT_COMPLETED);
                    process(transaction);
                    return;
                }
            } catch (Exception e) {
                if (attempt == retryPolicy.getMaxAttempts()) {
                    transition(bookingId, BookingState.CANCELLED, BookingEvent.PAYMENT_FAILED);
                    transaction.setCurrentState(BookingState.CANCELLED);
                    compensationTransaction(transaction);
                    return;
                }
                try {
                    Thread.sleep(retryPolicy.getDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    private void processReservation(BookingTransaction transaction) {
        String bookingId = transaction.getBookingId();
        try {
            boolean success = reservationService.reserveSeats(transaction);
            if (success) {
                transition(bookingId, BookingState.BOOKING_CONFIRMED, BookingEvent.RESERVATION_SUCCESS);
                transaction.setCurrentState(BookingState.BOOKING_CONFIRMED);
                process(transaction);
            } else {
                transition(bookingId, BookingState.CANCELLED, BookingEvent.RESERVATION_FAILED);
                transaction.setCurrentState(BookingState.CANCELLED);
                compensationTransaction(transaction);
            }
        } catch (Exception e) {
            transition(bookingId, BookingState.CANCELLED, BookingEvent.RESERVATION_FAILED);
            transaction.setCurrentState(BookingState.CANCELLED);
            compensationTransaction(transaction);
        }
    }
    
    private void compensationTransaction(BookingTransaction transaction) {
        log.info("[Orchestrator] Compensation triggered for booking: {}", transaction.getBookingId());
        // Since we call compensation from processReservation when it fails, payment was already successful
        paymentService.refund(transaction);
    }
}
