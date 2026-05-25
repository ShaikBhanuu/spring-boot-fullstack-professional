package com.example.demo.worker;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    public Worker getWorker(Long id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with id: " + id));
    }

    public Worker createWorker(Worker worker) {
        if (workerRepository.existsByPhone(worker.getPhone())) {
            throw new BadRequestException(
                    "Phone number already exists: "
                            + worker.getPhone());
        }
        return workerRepository.save(worker);
    }

    public Worker updateWorker(Long id, Worker updated) {
        Worker existing = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with id: " + id));

        existing.setName(updated.getName());
        existing.setDesignation(updated.getDesignation());
        existing.setDailyWageRate(updated.getDailyWageRate());
        existing.setActive(updated.getActive());

        // Cache invalidation - worker profile changed
        // Remove from active workers cache if present
        String redisKey = "active_workers:" + id;
        redisTemplate.delete(redisKey);

        return workerRepository.save(existing);
    }
}