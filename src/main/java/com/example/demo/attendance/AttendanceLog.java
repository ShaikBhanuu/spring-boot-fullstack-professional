package com.example.demo.attendance;

import com.example.demo.site.Site;
import com.example.demo.worker.Worker;
import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_logs", indexes = {
        @Index(name = "idx_attendance_worker", columnList = "worker_id"),
        @Index(name = "idx_attendance_clockin", columnList = "clock_in"),
        @Index(name = "idx_attendance_worker_clockin", columnList = "worker_id, clock_in")
})
public class AttendanceLog {

    @Id
    @SequenceGenerator(
            name = "attendance_sequence",
            sequenceName = "attendance_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "attendance_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "clock_in", nullable = false)
    private LocalDateTime clockIn;

    @Column(name = "clock_out")
    private LocalDateTime clockOut;

    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours;

    @Column(nullable = false)
    private Boolean flagged = false;
}