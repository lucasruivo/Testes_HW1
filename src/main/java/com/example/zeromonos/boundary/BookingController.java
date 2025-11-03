package com.example.zeromonos.boundary;

import com.example.zeromonos.data.Booking;
import com.example.zeromonos.data.BookingState;
import com.example.zeromonos.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Criar novo booking
    @PostMapping
    public Booking createBooking(@RequestBody Booking booking) {
        logger.info("Criar novo booking: município={}, data={}, timeslot={}", 
                    booking.getMunicipality(), booking.getRequestedDate(), booking.getTimeSlot());
        return bookingService.createBooking(booking);
    }

    // Encontrar booking pelo token
    @GetMapping("/{token}")
    public Booking getBooking(@PathVariable String token) {
        logger.info("Consultar booking pelo token: {}", token);
        return bookingService.getBookingByToken(token)
                .orElseThrow(() -> new RuntimeException("Booking não encontrado"));
    }

    // Listar bookings por município
    @GetMapping
    public List<Booking> getBookingsByMunicipality(@RequestParam(required = false) String municipality) {
        if (municipality != null ) {
            logger.info("Listar bookings para município: {}", municipality);
            return bookingService.getBookingsByMunicipality(municipality);
        }
        logger.info("Listar todos os bookings");
        return bookingService.getAllBookings();
    }

    // Atualizar estado do booking
    @PutMapping("/{token}")
    public Booking updateBookingStatus(@PathVariable String token, @RequestParam String status) {
        try {
            BookingState newState = BookingState.valueOf(status);
            logger.info("Atualizar booking token={} para estado={}", token, newState);
            return bookingService.updateBookingStatus(token, newState);
        } catch (IllegalArgumentException e) {
            logger.warn("Tentativa de atualizar booking token={} para estado inválido: {}", token, status);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado inválido: " + status + ". Valores válidos: " + java.util.Arrays.toString(BookingState.values())
            );
        }
    }

    // Cancelar um booking
    @DeleteMapping("/{token}")
    public Booking cancelBooking(@PathVariable String token) {
        logger.info("Cancelar booking token={}", token);
        return bookingService.updateBookingStatus(token, BookingState.CANCELADO);
    }
}