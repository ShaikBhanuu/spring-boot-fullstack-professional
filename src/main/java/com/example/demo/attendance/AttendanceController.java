package com.example.demo.attendance;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/attendance")
@AllArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("clock-in")
    public ResponseEntity<AttendanceLog> clockIn(
            @RequestBody Map<String, Long> request) {
        Long workerId = request.get("workerId");
        Long siteId = request.get("siteId");
        return ResponseEntity.ok(
                attendanceService.clockIn(workerId, siteId));
    }

    @PostMapping("clock-out")
    public ResponseEntity<AttendanceLog> clockOut(
            @RequestBody Map<String, Long> request) {
        Long workerId = request.get("workerId");
        return ResponseEntity.ok(
                attendanceService.clockOut(workerId));
    }

    @GetMapping("active")
    public ResponseEntity<List<Map<Object, Object>>> getActiveWorkers() {
        return ResponseEntity.ok(
                attendanceService.getActiveWorkers());
    }

    @GetMapping("log")
    public ResponseEntity<Page<AttendanceLog>> getAttendanceLog(
            @RequestParam Long workerId,
            @RequestParam @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                attendanceService.getAttendanceLog(
                        workerId, from, to, pageable));
    }
}