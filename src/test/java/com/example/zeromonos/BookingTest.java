package com.example.zeromonos;

import com.example.zeromonos.data.Booking;
import com.example.zeromonos.data.BookingState;
import com.example.zeromonos.data.BookingStateHistory;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BookingTest {

    @Test
    void InitializeWithReceivedStateAndToken() {
        Booking booking = new Booking();

        assertNotNull(booking.getToken(), "O token não deve ser nulo");
        assertEquals(BookingState.RECEBIDO, booking.getStatus());
        assertFalse(booking.getStateHistory().isEmpty());
    }

    @Test
    void AddMultipleStatesShouldKeepOrder() {
        Booking booking = new Booking();
        booking.addState(BookingState.EM_PROG);
        booking.addState(BookingState.CONCLUIDO);

        List<BookingStateHistory> history = booking.getStateHistory();
        assertEquals(3, history.size());
        assertEquals(BookingState.RECEBIDO, history.get(0).getStatus());
        assertEquals(BookingState.EM_PROG, history.get(1).getStatus());
        assertEquals(BookingState.CONCLUIDO, history.get(2).getStatus());
    }

    @Test
    void FailWhenDateIsWeekend() {
        Booking booking = validBooking();

        LocalDate saturday = LocalDate.now().with(DayOfWeek.SATURDAY);
        if (saturday.isBefore(LocalDate.now())) saturday = saturday.plusWeeks(1);
        booking.setRequestedDate(saturday);

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertTrue(e.getMessage().contains("Não é permitido fazer pedidos ao fim de semana."));
    }

    @Test
    void StoreStatusAndTimestamp() {
        Booking booking = new Booking();
        BookingStateHistory history = new BookingStateHistory(booking, BookingState.CONCLUIDO);

        assertNotNull(history.getTimestamp());
        assertEquals(BookingState.CONCLUIDO, history.getStatus());
    }

    @Test
    void AddNewStateAndKeepHistory() {
        Booking booking = new Booking();
        booking.addState(BookingState.EM_PROG);

        assertEquals(BookingState.EM_PROG, booking.getStatus());
        assertEquals(2, booking.getStateHistory().size());
        assertEquals(BookingState.EM_PROG, booking.getStateHistory().get(1).getStatus());
    }

    @Test
    void AddingSameStateTwiceDoesNotDuplicate() {
        Booking booking = new Booking();
        booking.addState(BookingState.RECEBIDO);
        assertEquals(1, booking.getStateHistory().size(), "Não deve duplicar o estado inicial");
    }

    @Test
    void SetAndGetFields() {
        Booking booking = new Booking();
        booking.setMunicipality("Lisboa");
        booking.setDescription("Troca de contador");
        booking.setRequestedDate(nextWeekday(5));
        booking.setTimeSlot("09:00");

        assertEquals("Lisboa", booking.getMunicipality());
        assertEquals("Troca de contador", booking.getDescription());
        assertEquals("09:00", booking.getTimeSlot());
    }

    @Test
    void PassValidationWhenAllFieldsValid() {
        Booking booking = validBooking();
        assertDoesNotThrow(booking::validateSelf);
    }

    @Test
    void FailWhenMunicipalityMissing() {
        Booking booking = validBooking();
        booking.setMunicipality(null);

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Município é obrigatório.", e.getMessage());
    }

    @Test
    void FailWhenDescriptionMissing() {
        Booking booking = validBooking();
        booking.setDescription("   ");

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Descrição é obrigatória.", e.getMessage());
    }

    @Test
    void FailWhenDateMissing() {
        Booking booking = validBooking();
        booking.setRequestedDate(null);

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Data solicitada é obrigatória.", e.getMessage());
    }

    @Test
    void FailWhenTimeSlotMissing() {
        Booking booking = validBooking();
        booking.setTimeSlot("");

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Time slot é obrigatório.", e.getMessage());
    }

    @Test
    void FailWhenDateTooSoon() {
        Booking booking = validBooking();
        booking.setRequestedDate(nextWeekday(1));

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertTrue(e.getMessage().contains("O pedido deve ser feito com pelo menos 3 dias de antecedência"));
    }

    @Test
    void EachBookingHasUniqueToken() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            Booking booking = new Booking();
            assertTrue(tokens.add(booking.getToken()), "Token duplicado encontrado!");
        }
    }

    @Test
    void ChangeStateToCancelled() {
        Booking booking = validBooking();
        booking.addState(BookingState.CANCELADO);
        assertEquals(BookingState.CANCELADO, booking.getStatus());
    }

    @Test
    void HistoryShouldContainTimestampsInOrder() {
        Booking booking = new Booking();
        booking.addState(BookingState.EM_PROG);
        booking.addState(BookingState.CONCLUIDO);

        List<BookingStateHistory> history = booking.getStateHistory();
        assertTrue(history.get(0).getTimestamp().isBefore(history.get(1).getTimestamp())
                || history.get(0).getTimestamp().equals(history.get(1).getTimestamp()));
    }

    @RepeatedTest(3)
    void ValidateMultipleValidBookings() {
        Booking booking = validBooking();
        assertDoesNotThrow(booking::validateSelf);
    }

    @Test
    void ShouldAllowFutureDates() {
        Booking booking = validBooking();
        booking.setRequestedDate(LocalDate.now().plusDays(10));
        assertDoesNotThrow(booking::validateSelf);
    }

    private Booking validBooking() {
        Booking booking = new Booking();
        booking.setMunicipality("Lisboa");
        booking.setDescription("Pedido de recolha");
        booking.setRequestedDate(nextWeekday(5));
        booking.setTimeSlot("10:00");
        return booking;
    }

    private LocalDate nextWeekday(int daysAhead) {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}