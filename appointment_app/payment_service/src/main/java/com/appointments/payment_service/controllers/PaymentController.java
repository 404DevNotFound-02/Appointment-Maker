package com.appointments.payment_service.controllers;

import com.appointments.payment_service.dto.PaymentRequest;
import com.appointments.payment_service.entity.Payment;
import com.appointments.payment_service.repo.PaymentRepository;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.appointments.payment_service.dto.ProductRequest;
// import com.appointments.payment_service.dto.StripeResponse;
// import com.appointments.payment_service.services.StripeService;

// @RestController
// @RequestMapping("/product/v1")
// public class ProductCheckoutController {

//     private StripeService stripeService;

//     public ProductCheckoutController(StripeService stripeService) {
//         this.stripeService = stripeService;
//     }

//     @PostMapping("/checkout")
//     public ResponseEntity<StripeResponse> checkoutProducts(@RequestBody ProductRequest productRequest) {
//         StripeResponse stripeResponse = stripeService.checkoutProducts(productRequest);
//         return ResponseEntity
//                 .status(HttpStatus.OK)
//                 .body(stripeResponse);
//     }
// }

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<String> createCheckoutSession(@RequestBody PaymentRequest paymentRequest,
            @RequestHeader("Authorization") String authorizationHeader) {
        Stripe.apiKey = stripeSecretKey;

        try {
            String token = authorizationHeader.replace("Bearer ", "");
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(
                            "http://localhost:8083/appointments/payment-success?session_id={CHECKOUT_SESSION_ID}&token="
                                    + token)
                    .setCancelUrl(paymentRequest.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(1000L) // $10.00 (in cents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Appointment Booking")
                                                                    .build())
                                                    .build())
                                    .setQuantity(1L)
                                    .build())
                    .build();

            Session session = Session.create(params);
            return ResponseEntity.ok(session.getUrl());
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Error creating Stripe session: " + e.getMessage());
        }
    }

    @GetMapping("/verify-payment")
    public ResponseEntity<Boolean> verifyPayment(@RequestParam("session_id") String sessionId) {
        Stripe.apiKey = stripeSecretKey;
        try {
            Session session = Session.retrieve(sessionId);
            if ("paid".equals(session.getPaymentStatus())) {
                // Find payment by appointmentId (assumes appointmentId passed in metadata or
                // stored elsewhere)
                // For simplicity, assume payment-service tracks this separately or adjust flow
                return ResponseEntity.ok(true); // Rely on appoinment_service to update Payment
            }
            return ResponseEntity.ok(false);
        } catch (StripeException e) {
            return ResponseEntity.status(500).body(false);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Void> savePayment(@RequestBody Payment payment) {
        paymentRepository.save(payment);
        return ResponseEntity.ok().build();
    }
}