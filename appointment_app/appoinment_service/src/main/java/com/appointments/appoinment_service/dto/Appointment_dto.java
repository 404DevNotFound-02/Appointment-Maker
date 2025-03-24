package com.appointments.appoinment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment_dto {
    private String title;
    private String description;
    private Timestamp appointmentTime;
    private String location;
}
