package com.director_appraisal.form_data_service.controller.config;

import com.director_appraisal.form_data_service.model.config.UniversitySchool;
import com.director_appraisal.form_data_service.service.config.UniversitySchoolService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/config/universities/{universityId}/schools", "/api/config/universities/{universityId}/schools", "/api/universities/{universityId}/schools"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UniversitySchoolController {

    private final UniversitySchoolService schoolService;

    @GetMapping
    public ResponseEntity<List<UniversitySchool>> getSchools(
            @PathVariable Long universityId,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        List<UniversitySchool> schools = all
                ? schoolService.getSchoolsByUniversity(universityId)
                : schoolService.getActiveSchoolsByUniversity(universityId);
        return ResponseEntity.ok(schools);
    }

    @PostMapping
    public ResponseEntity<?> createSchool(
            @PathVariable Long universityId,
            @RequestBody CreateSchoolRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "School name is required."));
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "School code/abbreviation is required."));
        }

        try {
            UniversitySchool school = UniversitySchool.builder()
                    .name(request.getName().trim())
                    .code(request.getCode().trim().toUpperCase())
                    .groupName(request.getGroupName() != null && !request.getGroupName().isBlank() ? request.getGroupName().trim() : "general")
                    .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : "ACTIVE")
                    .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                    .build();

            UniversitySchool created = schoolService.createSchool(universityId, school);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{schoolId}")
    public ResponseEntity<?> updateSchool(
            @PathVariable Long universityId,
            @PathVariable Long schoolId,
            @RequestBody CreateSchoolRequest request) {
        try {
            UniversitySchool school = UniversitySchool.builder()
                    .name(request.getName())
                    .code(request.getCode())
                    .groupName(request.getGroupName())
                    .status(request.getStatus())
                    .displayOrder(request.getDisplayOrder())
                    .build();

            UniversitySchool updated = schoolService.updateSchool(schoolId, school);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{schoolId}")
    public ResponseEntity<?> deleteSchool(
            @PathVariable Long universityId,
            @PathVariable Long schoolId) {
        schoolService.deleteSchool(schoolId);
        return ResponseEntity.ok(Map.of("success", true, "message", "School deleted successfully."));
    }

    @Data
    public static class CreateSchoolRequest {
        private String name;
        private String code;
        private String groupName;
        private String status;
        private Integer displayOrder;
    }
}
