package com.example.demo.worker;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/workers")
@AllArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping
    public List<Worker> getAllWorkers() {
        return workerService.getAllWorkers();
    }

    @GetMapping("{id}")
    public Worker getWorker(@PathVariable Long id) {
        return workerService.getWorker(id);
    }

    @PostMapping
    public ResponseEntity<Worker> createWorker(
            @RequestBody Worker worker) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(workerService.createWorker(worker));
    }

    @PutMapping("{id}")
    public Worker updateWorker(
            @PathVariable Long id,
            @RequestBody Worker worker) {
        return workerService.updateWorker(id, worker);
    }
}