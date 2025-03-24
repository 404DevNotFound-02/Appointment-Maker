package com.appointments.appoinment_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.appointments.appoinment_service.dto.Appointment_dto;
import com.appointments.appoinment_service.dto.PaymentRequest;
import com.appointments.appoinment_service.entity.Appointment;
import com.appointments.appoinment_service.service.AppointmentService;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping
    public ResponseEntity<String> createAppointment(
            @RequestBody Appointment_dto appointmentDto,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        // Create pending appointment
        Appointment appointment = new Appointment();
        appointment.setUserId(Long.parseLong(userId));
        appointment.setTitle(appointmentDto.getTitle());
        appointment.setDescription(appointmentDto.getDescription());
        appointment.setAppointmentTime(appointmentDto.getAppointmentTime());
        appointment.setLocation(appointmentDto.getLocation());
        appointment.setStatus("PENDING_PAYMENT");
        Appointment savedAppointment = appointmentService.createOrUpdateAppointment(appointment);

        // Prepare payment request
        String successUrl = "http://localhost:8083/appointments/payment-success";
        String cancelUrl = "http://localhost:3000/payment-cancel";
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setSuccessUrl(successUrl);
        paymentRequest.setCancelUrl(cancelUrl);
        paymentRequest.setAppointmentId(savedAppointment.getId());
        paymentRequest.setUserId(Long.parseLong(userId));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

        // Call payment-service
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://payment-service/payments/create-checkout-session",
                    entity,
                    String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok(response.getBody()); // Stripe Checkout URL
            } else {
                return ResponseEntity.status(500).body("Failed to initiate payment");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Payment service error: " + e.getMessage());
        }
    }

    @GetMapping("/payment-success")
    public ResponseEntity<Void> handlePaymentSuccess(
            @RequestParam("session_id") String sessionId,
            @RequestParam(value = "token", required = false) String tokenFromQuery) {
        try {
            String token = tokenFromQuery;
            System.out.println("Received token: " + (token != null ? "present" : "null"));
            if (token == null || !isTokenValid(token)) {
                System.out.println("Token invalid or missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userId = getUserIdFromToken(token);
            System.out.println("User ID: " + userId);
            if (userId == null) {
                System.out.println("User ID is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Verify payment
            System.out.println("Verifying payment with session ID: " + sessionId);
            ResponseEntity<Boolean> paymentResponse;
            try {
                paymentResponse = restTemplate.exchange(
                        "http://payment-service/payments/verify-payment?session_id=" + sessionId,
                        HttpMethod.GET,
                        entity,
                        Boolean.class);
                System.out.println("Payment verification response: " + paymentResponse.getStatusCode() + ", body: "
                        + paymentResponse.getBody());
            } catch (RestClientException e) {
                System.err.println("Payment verification failed: " + e.getMessage());
                throw new RuntimeException("Failed to verify payment", e);
            }

            if (paymentResponse.getStatusCode() == HttpStatus.OK && Boolean.TRUE.equals(paymentResponse.getBody())) {
                Appointment appointment = appointmentService.findPendingByUserId(Long.parseLong(userId));
                System.out.println("Found appointment: " + (appointment != null ? appointment.getId() : "null"));
                if (appointment != null && "PENDING_PAYMENT".equals(appointment.getStatus())) {
                    appointment.setStatus("CONFIRMED");
                    appointmentService.createOrUpdateAppointment(appointment);
                    System.out.println("Appointment updated to CONFIRMED");

                    PaymentRequest paymentRequest = new PaymentRequest();
                    paymentRequest.setUserId(Long.parseLong(userId));
                    paymentRequest.setAppointmentId(appointment.getId());
                    HttpEntity<PaymentRequest> paymentEntity = new HttpEntity<>(paymentRequest, headers);
                    try {
                        restTemplate.postForEntity(
                                "http://payment-service/payments/save",
                                paymentEntity,
                                Void.class);
                        System.out.println("Payment status update request sent");
                    } catch (RestClientException e) {
                        System.err.println("Failed to update payment: " + e.getMessage());
                        throw new RuntimeException("Failed to update payment status", e);
                    }

                    HttpHeaders redirectHeaders = new HttpHeaders();
                    redirectHeaders.setLocation(URI.create("http://localhost:3000/dashboard"));
                    return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);
                } else {
                    System.out.println("No pending appointment found or incorrect status");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
            } else {
                System.out.println("Payment not verified as successful");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (Exception e) {
            System.err.println("Internal server error in payment-success: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/me")
    public ResponseEntity<List<Appointment>> getAppointmentsByUserId(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Appointment> appointments = appointmentService.getAppointmentsByUserId(Long.parseLong(userId));
        return ResponseEntity.ok(appointments);
    }

    // Other methods (update, delete) follow similar pattern...

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    private boolean isTokenValid(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            System.out.println("Attempting to validate token with auth-service at: http://auth-service/auth/validate");
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    "http://auth-service/auth/validate",
                    HttpMethod.POST,
                    entity,
                    Boolean.class);
            System.out.println("Validation response: " + response.getStatusCode() + ", body: " + response.getBody());
            return response.getStatusCode() == HttpStatus.OK && Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            System.err.println("Failed to validate token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // Full stack trace for debugging
            return false;
        }
    }

    private String getUserIdFromToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            System.out.println("Attempting to get user ID from auth-service at: http://auth-service/auth/user-id");
            ResponseEntity<String> response = restTemplate.exchange(
                    "http://auth-service/auth/user-id",
                    HttpMethod.GET,
                    entity,
                    String.class);
            System.out.println("User ID response: " + response.getStatusCode() + ", body: " + response.getBody());
            return response.getStatusCode() == HttpStatus.OK ? response.getBody() : null;
        } catch (Exception e) {
            System.err.println("Failed to get user ID: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable String id,
            @RequestBody Appointment_dto appointmentDto,
            @RequestHeader("Authorization") String authorizationHeader) {
        // Extract and validate token
        String token = extractToken(authorizationHeader);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // Get user ID from token
        String userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // Convert ID to Long and fetch existing appointment
        Long appointmentId;
        try {
            appointmentId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(null); // Invalid ID format
        }

        // Check if appointment exists and belongs to the user
        Appointment existingAppointment = appointmentService.getAppointmentById(appointmentId);
        if (existingAppointment == null || !existingAppointment.getUserId().equals(Long.parseLong(userId))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Appointment not found or unauthorized
        }

        // Update appointment fields
        existingAppointment.setTitle(appointmentDto.getTitle());
        existingAppointment.setDescription(appointmentDto.getDescription());
        existingAppointment.setAppointmentTime(appointmentDto.getAppointmentTime());
        existingAppointment.setLocation(appointmentDto.getLocation());

        // Save updated appointment
        Appointment updatedAppointment = appointmentService.createOrUpdateAppointment(existingAppointment);
        return ResponseEntity.ok(updatedAppointment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAppointment(@PathVariable String id,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // Get user ID from token
        String userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // Convert ID to Long and fetch existing appointment
        Long appointmentId;
        try {
            appointmentId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(null); // Invalid ID format
        }

        // Check if appointment exists and belongs to the user
        Appointment existingAppointment = appointmentService.getAppointmentById(appointmentId);
        if (existingAppointment == null || !existingAppointment.getUserId().equals(Long.parseLong(userId))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Appointment not found or unauthorized
        }

        try {
            appointmentService.deleteAppointment(appointmentId);
        } catch (Exception e) {
            System.out.println(e.getClass() + " " + e.getMessage());
        }

        return ResponseEntity.ok("Appointment Deleted");
    }
}