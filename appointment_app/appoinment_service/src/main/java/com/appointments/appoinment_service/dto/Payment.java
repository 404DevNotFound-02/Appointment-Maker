package com.appointments.appoinment_service.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class Payment {
    private Long userId;

    private Long AppointmentId;

    private BigDecimal amount;

    private String paymentStatus;

    private Date paymentDate;
}
