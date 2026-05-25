package com.example.demo.overtime;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/overtime")
@AllArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/summary/{workerId}")
    public ResponseEntity<?> getOvertimeSummary(
            @PathVariable Long workerId,
            @RequestParam String month) {

        // Parse the YYYY-MM string to extract year and month safely
        YearMonth yearMonth = YearMonth.parse(month);

        // Assuming your service takes workerId, year, and month as arguments
        return ResponseEntity.ok(overtimeService.getOvertimeSummary(workerId, yearMonth.getYear(), yearMonth.getMonthValue()));
    }

    @PostMapping("/settle/{workerId}")
    public ResponseEntity<?> settleOvertime(
            @PathVariable Long workerId,
            @RequestParam String month) {

        YearMonth yearMonth = YearMonth.parse(month);

        overtimeService.settleOvertime(workerId, yearMonth.getYear(), yearMonth.getMonthValue());

        return ResponseEntity.ok(Map.of("message", "Overtime settled successfully. SMS notification event triggered."));
    }
}