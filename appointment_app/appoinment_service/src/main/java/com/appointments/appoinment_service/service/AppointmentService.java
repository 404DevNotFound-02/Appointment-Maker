package com.appointments.appoinment_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.appointments.appoinment_service.entity.Appointment;
import com.appointments.appoinment_service.repo.AppointmentRepository;

import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    public Appointment createOrUpdateAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + id));
    }

    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }
    public Appointment findPendingByUserId(Long userId) {
        return appointmentRepository.findTopByUserIdAndStatusOrderByIdDesc(userId, "PENDING_PAYMENT")
                .orElse(null);
    }
}
