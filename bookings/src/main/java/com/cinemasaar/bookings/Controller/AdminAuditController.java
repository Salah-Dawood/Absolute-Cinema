package com.cinemasaar.bookings.Controller;

import com.cinemasaar.bookings.Enums.AuditAction;
import com.cinemasaar.bookings.Model.AdminAuditLog;
import com.cinemasaar.bookings.Service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/audit-logs")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    public List<AdminAuditLog> logs(
            @RequestParam(required = false) String targetId,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-admin") String adminUserId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        AdminGuard.requireAdmin(role);
        List<AdminAuditLog> logs = auditService.getLogs(targetId);
        auditService.log(adminUserId, AuditAction.ADMIN_LOG_VIEWED, "AUDIT_LOG", targetId, null, null);
        return logs;
    }
}