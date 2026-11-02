package com.cinemasaar.bookings.Repository;

import com.cinemasaar.bookings.Model.AdminAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AdminAuditLogRepository extends MongoRepository<AdminAuditLog, String> {
    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();
    List<AdminAuditLog> findByTargetIdOrderByCreatedAtDesc(String targetId);
}