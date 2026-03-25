package com.chepamotos.chepa_api.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_liquidation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLiquidation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "liquidation_id")
    private Long liquidationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private Mechanic mechanic;
    
    @Column(name = "liquidation_date", nullable = false)
    private LocalDate liquidationDate;
    
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
}