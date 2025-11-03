package com.example.zeromonos;

import com.example.zeromonos.data.Booking;
import com.example.zeromonos.data.BookingRepository;
import com.example.zeromonos.data.BookingState;
import com.example.zeromonos.service.BookingService;
import com.example.zeromonos.service.MunicipioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private BookingRepository repository;
    private MunicipioService municipioService;
    private BookingService bookingService;

    private Booking validBooking;

    @BeforeEach
    void setUp() {
        repository = mock(BookingRepository.class);
        municipioService = mock(MunicipioService.class);
        bookingService = new BookingService(repository, municipioService);

        validBooking = new Booking();
        validBooking.setMunicipality("Lisboa");
        validBooking.setDescription("Limpeza");
        validBooking.setRequestedDate(nextWeekday(5));
        validBooking.setTimeSlot("09:00-11:00");

        when(municipioService.isValidMunicipality("Lisboa")).thenReturn(true);
    }

    @Test
    void CreateValidBooking() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findByMunicipality("Lisboa")).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking saved = bookingService.createBooking(validBooking);

        assertEquals("Lisboa", saved.getMunicipality());
        assertEquals(BookingState.RECEBIDO, saved.getStatus());
        assertNotNull(saved.getToken());
        verify(repository, times(1)).save(validBooking);
    }

    @Test
    void RejectBookingIfInvalidMunicipality() {
        when(municipioService.isValidMunicipality("Lisboa")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(validBooking));

        assertTrue(ex.getMessage().contains("Município inválido"));
        verify(repository, never()).save(any());
    }

    @Test
    void RejectBookingIfDailyLimitReached() {
        List<Booking> existing = List.of(
                createBookingWithDateAndState(validBooking.getRequestedDate(), BookingState.RECEBIDO),
                createBookingWithDateAndState(validBooking.getRequestedDate(), BookingState.RECEBIDO),
                createBookingWithDateAndState(validBooking.getRequestedDate(), BookingState.RECEBIDO),
                createBookingWithDateAndState(validBooking.getRequestedDate(), BookingState.RECEBIDO),
                createBookingWithDateAndState(validBooking.getRequestedDate(), BookingState.RECEBIDO)
        );
        when(repository.findByMunicipality("Lisboa")).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(validBooking));
        assertEquals("Limite de pedidos atingido para este dia", ex.getMessage());
    }

    @Test
    void RejectBookingIfTimeSlotConflict() {
        Booking conflictBooking = createBookingWithDateAndState(validBooking.getRequestedDate(), BookingState.RECEBIDO);
        conflictBooking.setTimeSlot(validBooking.getTimeSlot());
        when(repository.findAll()).thenReturn(List.of(conflictBooking));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(validBooking));
        assertEquals("Não é possível reservar dois serviços no mesmo horário.", ex.getMessage());
    }

    @Test
    void RejectBookingIfActiveLimitExceeded() {
        List<Booking> activeBookings = List.of(
                createBookingWithDateAndStateAndTimeSlot(nextWeekday(5), BookingState.RECEBIDO,"09:00-10:00"),
                createBookingWithDateAndStateAndTimeSlot(nextWeekday(5), BookingState.EM_PROG,"10:00-11:00"),
                createBookingWithDateAndStateAndTimeSlot(nextWeekday(6), BookingState.RECEBIDO,"11:00-12:00")
        );
        when(repository.findAll()).thenReturn(activeBookings);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(validBooking));
        assertEquals("O cidadão já atingiu o limite de reservas ativas.", ex.getMessage());
    }

    @Test
    void HandleAllStateTransitions() {
        validBooking.addState(BookingState.RECEBIDO);
        when(repository.findByToken(validBooking.getToken())).thenReturn(Optional.of(validBooking));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking updated1 = bookingService.updateBookingStatus(validBooking.getToken(), BookingState.EM_PROG);
        assertEquals(BookingState.EM_PROG, updated1.getStatus());

        when(repository.findByToken(validBooking.getToken())).thenReturn(Optional.of(updated1));
        Booking updated2 = bookingService.updateBookingStatus(validBooking.getToken(), BookingState.CONCLUIDO);
        assertEquals(BookingState.CONCLUIDO, updated2.getStatus());

        when(repository.findByToken(validBooking.getToken())).thenReturn(Optional.of(updated2));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBookingStatus(validBooking.getToken(), BookingState.RECEBIDO));
        assertTrue(ex.getMessage().contains("Transição inválida"));
    }

    @Test
    void GetBookingsByMunicipalityShouldReturnList() {
        when(repository.findByMunicipality("Lisboa")).thenReturn(List.of(validBooking));

        List<Booking> result = bookingService.getBookingsByMunicipality("Lisboa");

        assertEquals(1, result.size());
        assertEquals("Lisboa", result.get(0).getMunicipality());
    }

    @Test
    void GetBookingsByMunicipalityShouldReturnEmptyList() {
        when(repository.findByMunicipality("Porto")).thenReturn(List.of());

        List<Booking> result = bookingService.getBookingsByMunicipality("Porto");

        assertTrue(result.isEmpty());
    }

    @Test
    void ShouldPreventTransitionFromConcluidoToAnyOther() {
        validBooking.addState(BookingState.CONCLUIDO);
        when(repository.findByToken(validBooking.getToken())).thenReturn(Optional.of(validBooking));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBookingStatus(validBooking.getToken(), BookingState.EM_PROG));

        assertTrue(ex.getMessage().contains("Transição inválida"));
    }

    @Test
    void RejectBookingIfDescriptionTooShort() {
        validBooking.setDescription("A");
        when(repository.findAll()).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(validBooking));

        assertTrue(ex.getMessage().contains("Descrição inválida"));
    }

    // ------------------------------
    // Métodos auxiliares
    // ------------------------------

    private Booking createBookingWithDateAndState(LocalDate date, BookingState state) {
        Booking b = new Booking();
        b.setRequestedDate(date);
        b.setTimeSlot("09:00-11:00");
        b.setDescription("Teste");
        b.setMunicipality("Lisboa");
        b.addState(state);
        return b;
    }

    private Booking createBookingWithDateAndStateAndTimeSlot(LocalDate date, BookingState state, String timeSlot) {
        Booking b = new Booking();
        b.setRequestedDate(date);
        b.setTimeSlot(timeSlot);
        b.setDescription("Teste");
        b.setMunicipality("Lisboa");
        b.addState(state);
        return b;
    }

    private LocalDate nextWeekday(int daysAhead) {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}