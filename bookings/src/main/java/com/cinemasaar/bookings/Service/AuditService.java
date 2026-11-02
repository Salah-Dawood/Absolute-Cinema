package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Enums.AuditAction;
import com.cinemasaar.bookings.Model.AdminAuditLog;
import com.cinemasaar.bookings.Repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AuditService {

    private final AdminAuditLogRepository auditRepository;

    public void log(String adminUserId, AuditAction action, String targetType,
                    String targetId, String reason, String details) {
        auditRepository.save(AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .details(details)
                .createdAt(Instant.now())
                .build());
    }

    public List<AdminAuditLog> getLogs(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return auditRepository.findAllByOrderByCreatedAtDesc();
        }
        return auditRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
    }
}