package com.director_appraisal.form_data_service.model.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "university_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long universityId;

    @Column(nullable = false, length = 60)
    private String code; // e.g. "registrar", "hr", "dean-student-welfare", "dean-placement"

    @Column(nullable = false)
    private String name; // e.g. "Registrar", "HR", "Dean Student Welfare"

    private String description;

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
            code = code.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (code != null) {
            code = code.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }
    }
}
