package com.example.demo.overtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeRepository extends JpaRepository<OvertimeEntry, Long> {

    @Query("SELECT o FROM OvertimeEntry o " +
            "WHERE o.worker.id = :workerId " +
            "AND YEAR(o.date) = :year " +
            "AND MONTH(o.date) = :month")
    List<OvertimeEntry> findByWorkerAndMonth(
            @Param("workerId") Long workerId,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.amount), 0) " +
            "FROM OvertimeEntry o " +
            "WHERE o.worker.id = :workerId " +
            "AND YEAR(o.date) = :year " +
            "AND MONTH(o.date) = :month")
    java.math.BigDecimal sumAmountByWorkerAndMonth(
            @Param("workerId") Long workerId,
            @Param("year") int year,
            @Param("month") int month);

    boolean existsByWorkerIdAndSettlementStatus(
            Long workerId,
            SettlementStatus settlementStatus);
}