package com.cinemasaar.queue.admission.controller;

import com.cinemasaar.queue.admission.dto.QueueStatsResponse;
import com.cinemasaar.queue.admission.dto.QueueStatusResponse;
import com.cinemasaar.queue.admission.service.AdmissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final AdmissionService admission;

    public QueueController(AdmissionService admission) {
        this.admission = admission;
    }

    @PostMapping("/{showtimeId}/join")
    public QueueStatusResponse join(@PathVariable String showtimeId,
                                    @RequestHeader("X-User-Id") String userId) {
        return admission.join(showtimeId, userId);
    }

    @GetMapping("/{showtimeId}/status")
    public QueueStatusResponse status(@PathVariable String showtimeId,
                                      @RequestHeader("X-User-Id") String userId) {
        return admission.status(showtimeId, userId);
    }

    @GetMapping("/{showtimeId}/stats")
    public QueueStatsResponse stats(@PathVariable String showtimeId) {
        return admission.stats(showtimeId);
    }
}
