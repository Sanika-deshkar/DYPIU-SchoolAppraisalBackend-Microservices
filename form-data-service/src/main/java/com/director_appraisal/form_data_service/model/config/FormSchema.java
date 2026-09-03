package com.director_appraisal.form_data_service.model.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "form_schemas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long universityId;

    @Column(nullable = false, length = 50)
    private String auditType; // ACADEMIC, ADMINISTRATIVE

    @Column(nullable = false)
    private String name; // e.g., "Academic Audit 2025-26"

    private String description;

    @Column(length = 2000)
    @Builder.Default
    private String assignedSchools = "ALL"; // "ALL" or JSON array/comma-separated codes e.g. ["SoCSEA","SoE"]

    private Integer activeVersionNumber;

    private Long activeVersionId;

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (auditType != null) {
            auditType = auditType.trim().toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (auditType != null) {
            auditType = auditType.trim().toLowerCase();
        }
    }
}
