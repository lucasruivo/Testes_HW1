package com.example.zeromonos;

import com.example.zeromonos.data.Booking;
import com.example.zeromonos.data.BookingState;
import com.example.zeromonos.data.BookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Booking booking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();

        booking = new Booking();
        booking.setMunicipality("Lisboa");
        booking.setDescription("Teste IT");

        // Garante que não cai num fim de semana
        LocalDate requestedDate = LocalDate.now().plusDays(5);
        while (requestedDate.getDayOfWeek() == DayOfWeek.SATURDAY
            || requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            requestedDate = requestedDate.plusDays(1);
        }
        booking.setRequestedDate(requestedDate);

        booking.setTimeSlot("09:00-11:00");
        bookingRepository.save(booking);
    }

    @Test
    void CreateBooking() throws Exception {
        Booking newBooking = new Booking();
        newBooking.setMunicipality("Lisboa");
        newBooking.setDescription("Nova reserva");

        LocalDate requestedDate = LocalDate.now().plusDays(5);
        while (requestedDate.getDayOfWeek() == DayOfWeek.SATURDAY
            || requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            requestedDate = requestedDate.plusDays(1);
        }
        newBooking.setRequestedDate(requestedDate);
        newBooking.setTimeSlot("11:00-12:00");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBooking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipality").value("Lisboa"))
                .andExpect(jsonPath("$.status").value(BookingState.RECEBIDO.toString()));
    }

    @Test
    void GetBookingByToken() throws Exception {
        mockMvc.perform(get("/api/bookings/{token}", booking.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Teste IT"))
                .andExpect(jsonPath("$.status").value(BookingState.RECEBIDO.toString()));
    }

    @Test
    void UpdateBookingStatus() throws Exception {
        mockMvc.perform(put("/api/bookings/{token}", booking.getToken())
                .param("status", "EM_PROG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingState.EM_PROG.toString()));
    }

    @Test
    void ReturnErrorForInvalidStateUpdate() throws Exception {
        mockMvc.perform(put("/api/bookings/{token}", booking.getToken())
                .param("status", "INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Estado inválido: INVALIDO")));
    }

    @Test
    void ListBookingsByMunicipality() throws Exception {
        mockMvc.perform(get("/api/bookings")
                .param("municipality", "Lisboa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].municipality").value("Lisboa"));
    }

    @Test
    void CancelBooking() throws Exception {
        mockMvc.perform(delete("/api/bookings/{token}", booking.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingState.CANCELADO.toString()));
    }

    @Test
    void ShouldRejectBookingWithInvalidMunicipality() throws Exception {
        Booking invalid = new Booking();
        invalid.setMunicipality("Inexistente");
        invalid.setDescription("Teste inválido");
        invalid.setRequestedDate(LocalDate.now().plusDays(3));
        invalid.setTimeSlot("13:00-14:00");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Município inválido")));
    }

    @Test
    void ShouldRejectBookingWithTooShortDescription() throws Exception {
        Booking invalid = new Booking();
        invalid.setMunicipality("Lisboa");
        invalid.setDescription("Oi");
        invalid.setRequestedDate(LocalDate.now().plusDays(2));
        invalid.setTimeSlot("14:00-15:00");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Descrição inválida")));
    }

    @Test
    void ShouldNotAllowDuplicateTimeSlotSameDay() throws Exception {
        Booking duplicate = new Booking();
        duplicate.setMunicipality("Lisboa");
        duplicate.setDescription("Duplicado");
        duplicate.setRequestedDate(booking.getRequestedDate());
        duplicate.setTimeSlot("09:00-11:00");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("mesmo horário")));
    }

    @Test
    void ShouldListAllBookingsWhenNoMunicipalityProvided() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].municipality").value("Lisboa"));
    }
}