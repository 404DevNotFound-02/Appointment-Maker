package com.appointments.appoinment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String successUrl;
    private String cancelUrl;
    private Long appointmentId;
    private Long userId;
}
