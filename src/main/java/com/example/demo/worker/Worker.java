package com.example.demo.worker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "workers", indexes = {
        @Index(name = "idx_worker_phone", columnList = "phone"),
        @Index(name = "idx_worker_active", columnList = "active")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Worker {

    @Id
    @SequenceGenerator(name = "worker_sequence", sequenceName = "worker_sequence", allocationSize = 1)
    @GeneratedValue(generator = "worker_sequence",strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Designation designation;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal dailyWageRate;

    @Column(nullable = false)
    private Boolean active = true;
}
