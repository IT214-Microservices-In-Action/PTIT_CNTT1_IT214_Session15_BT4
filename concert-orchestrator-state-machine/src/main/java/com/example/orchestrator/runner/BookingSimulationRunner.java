package com.example.orchestrator.runner;

import com.example.orchestrator.machine.ConcertBookingStateMachine;
import com.example.orchestrator.model.BookingState;
import com.example.orchestrator.model.BookingTransaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BookingSimulationRunner implements CommandLineRunner {

    private final ConcertBookingStateMachine stateMachine;

    public BookingSimulationRunner(ConcertBookingStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=================================================");
        System.out.println("Bắt đầu mô phỏng Orchestrator Saga State Machine");
        System.out.println("=================================================");

        BookingTransaction transaction = BookingTransaction.builder()
                .bookingId("CONCERT-2026-088")
                .concertCode("LIVE-HCM-2026-ULTRA")
                .customerId("VIP-2024")
                .customerEmail("rika@email.com")
                .ticketQuantity(3)
                .amount(5500000)
                .currentState(BookingState.INITIATED)
                .build();

        stateMachine.process(transaction);

        System.out.println("=================================================");
        System.out.println("Kết thúc mô phỏng");
        System.out.println("=================================================");
    }
}
