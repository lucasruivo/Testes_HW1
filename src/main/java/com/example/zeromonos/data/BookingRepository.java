package com.example.zeromonos.data;

import com.example.zeromonos.data.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByToken(String token);
    List<Booking> findByMunicipality(String municipality);
}