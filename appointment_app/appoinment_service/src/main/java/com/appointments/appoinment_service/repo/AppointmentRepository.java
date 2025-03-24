package com.appointments.appoinment_service.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointments.appoinment_service.entity.Appointment;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserId(Long userId);

    Optional<Appointment> findTopByUserIdAndStatusOrderByIdDesc(Long userId, String status);
}
