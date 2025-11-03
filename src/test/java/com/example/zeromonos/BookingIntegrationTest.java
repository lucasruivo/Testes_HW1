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

        // Garante que não cai em fim de semana
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
    void shouldCreateBooking() throws Exception {
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
    void shouldGetBookingByToken() throws Exception {
        mockMvc.perform(get("/api/bookings/{token}", booking.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Teste IT"))
                .andExpect(jsonPath("$.status").value(BookingState.RECEBIDO.toString()));
    }

    @Test
    void shouldUpdateBookingStatus() throws Exception {
        mockMvc.perform(put("/api/bookings/{token}", booking.getToken())
                .param("status", "EM_PROG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingState.EM_PROG.toString()));
    }

    @Test
    void shouldReturnErrorForInvalidStateUpdate() throws Exception {
        mockMvc.perform(put("/api/bookings/{token}", booking.getToken())
                .param("status", "INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Estado inválido: INVALIDO")));
    }

    @Test
    void shouldListBookingsByMunicipality() throws Exception {
        mockMvc.perform(get("/api/bookings")
                .param("municipality", "Lisboa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].municipality").value("Lisboa"));
    }

    @Test
    void shouldCancelBooking() throws Exception {
        mockMvc.perform(delete("/api/bookings/{token}", booking.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingState.CANCELADO.toString()));
    }
}