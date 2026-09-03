package com.director_appraisal.form_data_service.model.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "university_schools")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversitySchool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long universityId;

    @Column(nullable = false, length = 50)
    private String code; // e.g. "SoCSEA", "SoE", "SoM"

    @Column(nullable = false)
    private String name; // e.g. "School of Computer Science & Applications"

    @Builder.Default
    private String groupName = "general"; // "engineering", "nonEngineering", "general"

    @Builder.Default
    private String status = "ACTIVE"; // "ACTIVE", "INACTIVE"

    private Integer displayOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (groupName == null || groupName.isBlank()) {
            groupName = "general";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }
}
