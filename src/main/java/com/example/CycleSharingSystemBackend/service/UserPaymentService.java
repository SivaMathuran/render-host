package com.example.CycleSharingSystemBackend.service;

import com.example.CycleSharingSystemBackend.dto.CreatePaymentResponse;
import com.example.CycleSharingSystemBackend.dto.PaymentRequest;

import java.time.LocalDate;

public interface UserPaymentService {
    Double getTotalEstimatedAmountForDate(LocalDate paymentDate);
    CreatePaymentResponse createPaymentIntent(PaymentRequest paymentRequest);
    public double getestimatedAmount(Long id );
}