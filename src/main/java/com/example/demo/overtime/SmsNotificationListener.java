package com.example.demo.overtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SmsNotificationListener {

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleOvertimeSettled(OvertimeSettledEvent event) {
        try {
            // In production: call SMS gateway API here
            System.out.println("SMS sent to worker "
                    + event.getWorkerId()
                    + ": Your overtime for "
                    + event.getMonth()
                    + " of Rs."
                    + event.getTotalAmount()
                    + " has been settled.");
        } catch (Exception e) {
            // SMS failure must NOT crash settlement
            // Log and continue - data is already correct
            System.err.println("SMS failed for worker "
                    + event.getWorkerId()
                    + ": " + e.getMessage());
        }
    }
}