package com.example.demo.attendance;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.overtime.OvertimeEntry;
import com.example.demo.overtime.OvertimeRepository;
import com.example.demo.overtime.SettlementStatus;
import com.example.demo.site.Site;
import com.example.demo.site.SiteRepository;
import com.example.demo.worker.Worker;
import com.example.demo.worker.WorkerRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final OvertimeRepository overtimeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACTIVE_WORKERS_KEY = "active_workers";
    private static final int STANDARD_HOURS = 8;
    private static final int MAX_SHIFT_HOURS = 16;
    private static final int MONTHLY_OT_CAP = 60;

    // Only DB work inside @Transactional
    @Transactional
    public AttendanceLog clockIn(Long workerId, Long siteId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with id: " + workerId));

        if (!worker.getActive()) {
            throw new BadRequestException(
                    "Worker is not active: " + worker.getName());
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Site not found with id: " + siteId));

        if (!site.getActive()) {
            throw new BadRequestException(
                    "Site is not active: " + site.getSiteName());
        }

        attendanceRepository.findActiveByWorkerId(workerId)
                .ifPresent(a -> {
                    throw new ConflictException(
                            "Worker is already clocked in at Site: "
                                    + a.getSite().getSiteName());
                });

        LocalDateTime now = LocalDateTime.now();
        AttendanceLog log = new AttendanceLog();
        log.setWorker(worker);
        log.setSite(site);
        log.setClockIn(now);
        log.setFlagged(false);
        return attendanceRepository.save(log);
    }

    // Redis write happens here — outside any transaction
    public void addToCache(AttendanceLog log) {
        try {
            Map<String, String> cacheEntry = new HashMap<>();
            cacheEntry.put("workerId", log.getWorker().getId().toString());
            cacheEntry.put("workerName", log.getWorker().getName());
            cacheEntry.put("siteId", log.getSite().getId().toString());
            cacheEntry.put("siteName", log.getSite().getSiteName());
            cacheEntry.put("clockIn", log.getClockIn().toString());

            String redisKey = ACTIVE_WORKERS_KEY + ":" + log.getWorker().getId();
            redisTemplate.opsForHash().putAll(redisKey, cacheEntry);
            redisTemplate.expire(redisKey, MAX_SHIFT_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // Redis failure must not affect clock-in response
        }
    }

    // Only DB work inside @Transactional
    @Transactional
    public AttendanceLog clockOut(Long workerId) {
        AttendanceLog log = attendanceRepository
                .findActiveByWorkerId(workerId)
                .orElseThrow(() -> new BadRequestException(
                        "Worker is not clocked in: " + workerId));

        LocalDateTime clockOut = LocalDateTime.now();
        log.setClockOut(clockOut);

        long minutes = ChronoUnit.MINUTES.between(log.getClockIn(), clockOut);
        BigDecimal totalHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        log.setTotalHours(totalHours);

        if (totalHours.compareTo(BigDecimal.valueOf(MAX_SHIFT_HOURS)) > 0) {
            log.setFlagged(true);
        }

        if (totalHours.compareTo(BigDecimal.valueOf(STANDARD_HOURS)) > 0) {
            BigDecimal rawOvertimeHours = totalHours
                    .subtract(BigDecimal.valueOf(STANDARD_HOURS));

            BigDecimal usedThisMonth = attendanceRepository
                    .sumOvertimeHoursForMonth(
                            workerId,
                            clockOut.getYear(),
                            clockOut.getMonthValue());

            BigDecimal remaining = BigDecimal.valueOf(MONTHLY_OT_CAP)
                    .subtract(usedThisMonth);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cappedOT = rawOvertimeHours.min(remaining);
                log.setOvertimeHours(cappedOT);

                BigDecimal amount = calculateOvertimeAmount(
                        cappedOT, log.getWorker().getDailyWageRate());

                OvertimeEntry entry = new OvertimeEntry();
                entry.setWorker(log.getWorker());
                entry.setAttendance(log);
                entry.setDate(clockOut.toLocalDate());
                entry.setOvertimeHours(cappedOT);
                entry.setOvertimeRateApplied(log.getWorker().getDailyWageRate());
                entry.setAmount(amount);
                entry.setSettlementStatus(SettlementStatus.PENDING);
                overtimeRepository.save(entry);
            }
        }

        return attendanceRepository.save(log);
    }

    // Redis delete happens here — outside any transaction
    public void removeFromCache(Long workerId) {
        try {
            String redisKey = ACTIVE_WORKERS_KEY + ":" + workerId;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            // Redis failure must not affect clock-out response
        }
    }

    public java.util.List<Map<Object, Object>> getActiveWorkers() {
        java.util.Set<String> keys = redisTemplate.keys(
                ACTIVE_WORKERS_KEY + ":*");
        java.util.List<Map<Object, Object>> activeWorkers =
                new java.util.ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> entries =
                        redisTemplate.opsForHash().entries(key);
                if (!entries.isEmpty()) {
                    activeWorkers.add(entries);
                }
            }
        }
        return activeWorkers;
    }

    public Page<AttendanceLog> getAttendanceLog(
            Long workerId, LocalDateTime from,
            LocalDateTime to, Pageable pageable) {
        return attendanceRepository.findByWorkerAndDateRange(
                workerId, from, to, pageable);
    }

    private BigDecimal calculateOvertimeAmount(
            BigDecimal overtimeHours, BigDecimal dailyWageRate) {
        BigDecimal hourlyRate = dailyWageRate
                .divide(BigDecimal.valueOf(STANDARD_HOURS),
                        2, RoundingMode.HALF_UP);

        BigDecimal firstTwoHours = overtimeHours.min(BigDecimal.valueOf(2));
        BigDecimal beyondTwoHours = overtimeHours
                .subtract(firstTwoHours).max(BigDecimal.ZERO);

        BigDecimal amount = firstTwoHours
                .multiply(hourlyRate)
                .multiply(BigDecimal.valueOf(1.5));
        amount = amount.add(beyondTwoHours
                .multiply(hourlyRate)
                .multiply(BigDecimal.valueOf(2.0)));

        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}