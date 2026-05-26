package com.example.demo.overtime;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/overtime")
@AllArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/summary/{workerId}")
    public ResponseEntity<?> getOvertimeSummary(
            @PathVariable Long workerId,
            @RequestParam String month) {

        // Let the service handle parsing and processing the String month
        return ResponseEntity.ok(overtimeService.getOvertimeSummary(workerId, month));
    }

    @PostMapping("/settle/{workerId}")
    public ResponseEntity<?> settleOvertime(
            @PathVariable Long workerId,
            @RequestParam String month) {

        // Pass the raw parameters straight through to match your service's signature
        return ResponseEntity.ok(overtimeService.settleOvertime(workerId, month));
    }
}