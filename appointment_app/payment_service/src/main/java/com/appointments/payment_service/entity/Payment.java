package com.appointments.payment_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long appointmentId;

    private BigDecimal amount;

    private String paymentStatus;

    private Date paymentDate;

    // Getters and Setters
}
