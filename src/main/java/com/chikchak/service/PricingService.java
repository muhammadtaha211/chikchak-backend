package com.chikchak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.Data;
import lombok.Builder;

import com.chikchak.model.ServiceType;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    @Value("${chikchak.service-fee:20}")
    private BigDecimal serviceFee;

    @Value("${chikchak.base-distance-rate:2.5}")
    private BigDecimal baseDistanceRate;

    @Value("${chikchak.max-distance-km:50}")
    private Integer maxDistanceKm;

    public PricingCalculation calculateBookingPrice(ServiceType serviceType,
                                                   Double customerLat, Double customerLng,
                                                   Double technicianLat, Double technicianLng,
                                                   BigDecimal baseDiagnosisFee) {

        // Movers have zero fees
        if (serviceType == ServiceType.MOVERS) {
            return PricingCalculation.builder()
                .diagnosisFee(BigDecimal.ZERO)
                .distanceKm(BigDecimal.ZERO)
                .distanceFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
        }

        // Calculate distance
        BigDecimal distanceKm = calculateDistance(customerLat, customerLng, technicianLat, technicianLng);
        
        // Validate maximum distance
        if (distanceKm.compareTo(new BigDecimal(maxDistanceKm)) > 0) {
            throw new RuntimeException("Distance exceeds maximum service radius");
        }
        
        // Calculate distance fee
        BigDecimal distanceFee = distanceKm.multiply(baseDistanceRate)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Total diagnosis fee = base fee + distance fee
        BigDecimal totalDiagnosisFee = baseDiagnosisFee.add(distanceFee);
        
        // Total amount = diagnosis fee + service fee
        BigDecimal totalAmount = totalDiagnosisFee.add(serviceFee);
        
        return PricingCalculation.builder()
            .diagnosisFee(totalDiagnosisFee)
            .distanceKm(distanceKm)
            .distanceFee(distanceFee)
            .serviceFee(serviceFee)
            .totalAmount(totalAmount)
            .build();
    }

    BigDecimal calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return BigDecimal.ZERO;
        }

        // Haversine formula
        final double R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }




    @Data
    @Builder
    public static class PricingCalculation {
        private BigDecimal diagnosisFee;
        private BigDecimal distanceKm;
        private BigDecimal distanceFee;
        private BigDecimal serviceFee;
        private BigDecimal totalAmount;
    }
}

