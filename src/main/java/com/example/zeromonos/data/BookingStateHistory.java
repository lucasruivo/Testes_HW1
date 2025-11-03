package com.example.zeromonos.data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class BookingStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private BookingState status;

    @ManyToOne
    private Booking booking;

    public BookingStateHistory() {}

    public BookingStateHistory(Booking booking, BookingState status) {
        this.booking = booking;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public BookingState getStatus() { return status; }
}