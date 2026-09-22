package com.example.orchestrator.listener;

import com.example.orchestrator.model.BookingEvent;
import com.example.orchestrator.model.BookingState;
import org.springframework.stereotype.Component;

@Component
public class StateChangeListener {
    public void onStateChanged(String bookingId, BookingState oldState, BookingState newState, BookingEvent event) {
        // Listener hook for state changes
    }
}
