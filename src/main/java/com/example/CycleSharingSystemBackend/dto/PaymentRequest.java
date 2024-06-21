package com.example.CycleSharingSystemBackend.dto;

import lombok.*;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String id;
    private Long userId;
    private double estimatedAmount;
}
