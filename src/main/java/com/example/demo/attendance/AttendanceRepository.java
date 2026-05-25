package com.example.demo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {

    @Query("SELECT a FROM AttendanceLog a " +
            "JOIN FETCH a.worker " +
            "JOIN FETCH a.site " +
            "WHERE a.worker.id = :workerId " +
            "AND a.clockIn >= :from " +
            "AND a.clockIn <= :to")
    Page<AttendanceLog> findByWorkerAndDateRange(
            @Param("workerId") Long workerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT a FROM AttendanceLog a " +
            "WHERE a.worker.id = :workerId " +
            "AND a.clockOut IS NULL")
    Optional<AttendanceLog> findActiveByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) " +
            "FROM AttendanceLog a " +
            "WHERE a.worker.id = :workerId " +
            "AND YEAR(a.clockIn) = :year " +
            "AND MONTH(a.clockIn) = :month")
    java.math.BigDecimal sumOvertimeHoursForMonth(
            @Param("workerId") Long workerId,
            @Param("year") int year,
            @Param("month") int month);
}