package com.cinemasaar.bookings.Model;

import com.cinemasaar.bookings.Enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("admin_audit_logs")
public class AdminAuditLog {
    @Id
    private String id;
    private String adminUserId;
    private AuditAction action;
    private String targetType;
    private String targetId;
    private String reason;
    private String details;
    private Instant createdAt;
}