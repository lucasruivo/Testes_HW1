package com.example.zeromonos;

import org.junit.jupiter.api.Test;

import com.example.zeromonos.data.Booking;
import com.example.zeromonos.data.BookingState;
import com.example.zeromonos.data.BookingStateHistory;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BookingTest {

    @Test
    void shouldInitializeWithReceivedStateAndToken() {
        Booking booking = new Booking();

        assertNotNull(booking.getToken());
        assertEquals(BookingState.RECEBIDO, booking.getStatus());
        assertFalse(booking.getStateHistory().isEmpty());
    }

    @Test
    void shouldAddNewStateAndKeepHistory() {
        Booking booking = new Booking();
        booking.addState(BookingState.EM_PROG);

        assertEquals(BookingState.EM_PROG, booking.getStatus());
        assertEquals(2, booking.getStateHistory().size());
        assertEquals(BookingState.EM_PROG, booking.getStateHistory().get(1).getStatus());
    }

    @Test
    void shouldSetAndGetFields() {
        Booking booking = new Booking();
        booking.setMunicipality("Lisboa");
        booking.setDescription("Troca de contador");
        booking.setRequestedDate(nextWeekday(5));
        booking.setTimeSlot("09:00");

        assertEquals("Lisboa", booking.getMunicipality());
        assertEquals("Troca de contador", booking.getDescription());
        assertEquals("09:00", booking.getTimeSlot());
    }

    // --- Novos testes de regras de domínio ---

    @Test
    void shouldPassValidationWhenAllFieldsValid() {
        Booking booking = validBooking();
        assertDoesNotThrow(booking::validateSelf);
    }

    @Test
    void shouldFailWhenMunicipalityMissing() {
        Booking booking = validBooking();
        booking.setMunicipality(null);

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Município é obrigatório.", e.getMessage());
    }

    @Test
    void shouldFailWhenDescriptionMissing() {
        Booking booking = validBooking();
        booking.setDescription("   ");

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Descrição é obrigatória.", e.getMessage());
    }

    @Test
    void shouldFailWhenDateMissing() {
        Booking booking = validBooking();
        booking.setRequestedDate(null);

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Data solicitada é obrigatória.", e.getMessage());
    }

    @Test
    void shouldFailWhenTimeSlotMissing() {
        Booking booking = validBooking();
        booking.setTimeSlot("");

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertEquals("Time slot é obrigatório.", e.getMessage());
    }

    @Test
    void shouldFailWhenDateIsWeekend() {
        Booking booking = validBooking();

        // Força para um sábado
        LocalDate saturday = LocalDate.now().with(DayOfWeek.SATURDAY);
        if (saturday.isBefore(LocalDate.now())) saturday = saturday.plusWeeks(1);

        booking.setRequestedDate(saturday);

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertTrue(e.getMessage().contains("Não é permitido fazer pedidos ao fim de semana."));
    }

    @Test
    void shouldFailWhenDateTooSoon() {
        Booking booking = validBooking();
        booking.setRequestedDate(nextWeekday(2));

        Exception e = assertThrows(IllegalArgumentException.class, booking::validateSelf);
        assertTrue(e.getMessage().contains("O pedido deve ser feito com pelo menos 3 dias de antecedência"));
    }

    @Test
    void shouldStoreStatusAndTimestamp() {
        Booking booking = new Booking();
        BookingStateHistory history = new BookingStateHistory(booking, BookingState.CONCLUIDO);

        assertNotNull(history.getTimestamp());
        assertEquals(BookingState.CONCLUIDO, history.getStatus());
    }

    // --- Método auxiliar ---
    private Booking validBooking() {
        Booking booking = new Booking();
        booking.setMunicipality("Lisboa");
        booking.setDescription("Pedido de recolha");
        booking.setRequestedDate(nextWeekday(5));
        booking.setTimeSlot("10:00");
        return booking;
    }

    // --- Garante que a data não cai num fim de semana ---
    private LocalDate nextWeekday(int daysAhead) {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}