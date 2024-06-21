package com.example.CycleSharingSystemBackend.service;


import com.example.CycleSharingSystemBackend.dto.CreatePaymentResponse;
import com.example.CycleSharingSystemBackend.dto.PaymentRequest;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;

@Service
public class StripePaymentService {

    @Value("${stripe.key.secret}")
    private String stripeApiKey;

    public CreatePaymentResponse createPaymentIntent(PaymentRequest paymentRequest) {
        Stripe.apiKey = stripeApiKey;

        try {
            CustomerCreateParams customerParams = new CustomerCreateParams.Builder().build();
            Customer customer = Customer.create(customerParams);

            PaymentIntentCreateParams createParams = new PaymentIntentCreateParams.Builder()
                    .setCurrency("usd")
                    .setAmount((long) (paymentRequest.getEstimatedAmount() * 100))
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                    .setCustomer(customer.getId())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods
                                    .builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(createParams);
            CreatePaymentResponse response = new CreatePaymentResponse();
            response.setClientSecret(intent.getClientSecret());
            return response;
        } catch (StripeException e) {
            // Handle Stripe exceptions
            System.out.println("Error code is : " + e.getCode());
            throw new RuntimeException(e); // Re-throw the exception as a runtime exception
        }
    }

    public void chargeCustomer(String customerId, double estimatedAmount) throws StripeException {
        // Set up Stripe API key
        Stripe.apiKey = stripeApiKey;

        // Lookup the payment methods available for the customer
        PaymentMethodListParams listParams = new PaymentMethodListParams.Builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
        PaymentMethodCollection paymentMethods = PaymentMethod.list(listParams);

        // Create PaymentIntent creation parameters
        PaymentIntentCreateParams createParams = new PaymentIntentCreateParams.Builder()
                .setCurrency("usd")
                .setAmount((long) (estimatedAmount * 100))
                .setPaymentMethod(paymentMethods.getData().get(0).getId())
                .setCustomer(customerId)
                .setConfirm(true)
                .setOffSession(true)
                .build();

        try {
            // Charge the customer and payment method immediately
            PaymentIntent paymentIntent = PaymentIntent.create(createParams);
        } catch (StripeException e) {
            // Handle Stripe exceptions
            // Error code will be authentication_required if authentication is needed
            System.out.println("Error code is : " + e.getCode());
            String paymentIntentId = e.getStripeError().getPaymentIntent().getId();
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            System.out.println(paymentIntent.getId());
            throw e; // Re-throw the exception for handling at a higher level if needed
        }
    }
}


