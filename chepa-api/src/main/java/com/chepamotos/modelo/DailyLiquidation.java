package com.chepamotos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(

        name = "daily_liquidation", uniqueConstraints = {

                @UniqueConstraint(columnNames = { "mechanic_id", "date" })

        }

)
public class DailyLiquidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_liquidation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private Mechanic mechanic;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_revenue", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "mechanic_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal mechanicShare;

    @Column(name = "shop_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal shopShare;

    @Column(name = "invoice_count", nullable = false)
    private Integer invoiceCount;

}