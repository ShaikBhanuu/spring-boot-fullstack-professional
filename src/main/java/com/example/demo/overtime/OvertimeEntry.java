package com.example.demo.overtime;

import com.example.demo.attendance.AttendanceLog;
import com.example.demo.worker.Worker;
import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "overtime_entries", indexes = {
        @Index(name = "idx_overtime_worker", columnList = "worker_id"),
        @Index(name = "idx_overtime_worker_date", columnList = "worker_id, date"),
        @Index(name = "idx_overtime_status", columnList = "settlement_status")
})
public class OvertimeEntry {

    @Id
    @SequenceGenerator(
            name = "overtime_sequence",
            sequenceName = "overtime_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "overtime_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceLog attendance;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "overtime_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeHours;

    @Column(name = "overtime_rate_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtimeRateApplied;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;
}