package com.example.CycleSharingSystemBackend.controller;

import com.example.CycleSharingSystemBackend.dto.TotalEstimatedAmountDTO;
import com.example.CycleSharingSystemBackend.service.UserPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payment")
public class UserPaymentController {


    @Autowired
    private UserPaymentService userPaymentService;



    @GetMapping("/total")
    public TotalEstimatedAmountDTO getTotalEstimatedAmount(@RequestParam("date") String date) {
        LocalDate paymentDate = LocalDate.parse(date);
        Double totalAmount = userPaymentService.getTotalEstimatedAmountForDate(paymentDate);

        TotalEstimatedAmountDTO response = new TotalEstimatedAmountDTO();
        response.setPaymentDate(paymentDate);
        response.setTotalEstimatedAmount(totalAmount);

        return response;
    }
    @GetMapping("/estimatedAmount/{id}")
    public double getestimatedAmount(@PathVariable Long id) {
        return userPaymentService.getestimatedAmount(id);
    }
    }