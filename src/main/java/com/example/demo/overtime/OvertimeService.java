package com.example.demo.overtime;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.worker.WorkerRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class OvertimeService {

    private final OvertimeRepository overtimeRepository;
    private final WorkerRepository workerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Map<String, Object> getOvertimeSummary(
            Long workerId, String month) {

        // Fetch external data BEFORE opening transaction
        // (Fix for Ticket LF-205 - don't hold DB connection
        // while calling external API)
        BigDecimal minimumWageRate = fetchMinimumWageRate();

        YearMonth yearMonth = YearMonth.parse(month);
        int year = yearMonth.getYear();
        int monthValue = yearMonth.getMonthValue();

        // Validate worker exists
        workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with id: " + workerId));

        List<OvertimeEntry> entries = overtimeRepository
                .findByWorkerAndMonth(workerId, year, monthValue);

        BigDecimal totalHours = entries.stream()
                .map(OvertimeEntry::getOvertimeHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = entries.stream()
                .map(OvertimeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> breakdown = entries.stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", e.getDate().toString());
                    item.put("overtimeHours",
                            e.getOvertimeHours());
                    item.put("amount", e.getAmount());
                    item.put("status",
                            e.getSettlementStatus().name());
                    return item;
                }).toList();

        Map<String, Object> summary = new HashMap<>();
        summary.put("workerId", workerId);
        summary.put("month", month);
        summary.put("totalOvertimeHours", totalHours);
        summary.put("totalAmount", totalAmount);
        summary.put("minimumWageRate", minimumWageRate);
        summary.put("breakdown", breakdown);

        return summary;
    }

    @Transactional
    public Map<String, Object> settleOvertime(
            Long workerId, String month) {

        // Cannot settle current month
        YearMonth yearMonth = YearMonth.parse(month);
        YearMonth currentMonth = YearMonth.now();

        if (!yearMonth.isBefore(currentMonth)) {
            throw new BadRequestException(
                    "Cannot settle current or future month. " +
                            "Only past months can be settled.");
        }

        // Validate worker
        workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with id: " + workerId));

        int year = yearMonth.getYear();
        int monthValue = yearMonth.getMonthValue();

        List<OvertimeEntry> entries = overtimeRepository
                .findByWorkerAndMonth(workerId, year, monthValue);

        if (entries.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No overtime entries found for worker "
                            + workerId + " in month " + month);
        }

        // Check if already settled
        boolean alreadySettled = entries.stream()
                .allMatch(e -> e.getSettlementStatus()
                        == SettlementStatus.SETTLED);
        if (alreadySettled) {
            throw new ConflictException(
                    "Overtime for worker " + workerId
                            + " in " + month + " is already settled.");
        }

        // Settle all entries atomically
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OvertimeEntry entry : entries) {
            entry.setSettlementStatus(SettlementStatus.SETTLED);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        overtimeRepository.saveAll(entries);

        // Publish event - SMS fires AFTER transaction commits
        // This is the fix for Ticket LF-204
        eventPublisher.publishEvent(
                new OvertimeSettledEvent(this, workerId,
                        month, totalAmount));

        Map<String, Object> response = new HashMap<>();
        response.put("workerId", workerId);
        response.put("month", month);
        response.put("totalSettledAmount", totalAmount);
        response.put("entriesSettled", entries.size());
        response.put("status", "SETTLED");

        return response;
    }

    private BigDecimal fetchMinimumWageRate() {
        // Simulated external API call
        // In production this would call government API
        // Fetched OUTSIDE @Transactional to not hold DB connection
        return BigDecimal.valueOf(400.00);
    }
}