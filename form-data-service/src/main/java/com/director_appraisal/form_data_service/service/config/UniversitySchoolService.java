package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.model.config.UniversitySchool;
import com.director_appraisal.form_data_service.repository.config.UniversitySchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversitySchoolService {

    private final UniversitySchoolRepository schoolRepository;

    public List<UniversitySchool> getSchoolsByUniversity(Long universityId) {
        if (universityId == null) {
            return List.of();
        }
        return schoolRepository.findByUniversityIdOrderByDisplayOrderAscIdAsc(universityId);
    }

    public List<UniversitySchool> getActiveSchoolsByUniversity(Long universityId) {
        if (universityId == null) {
            return List.of();
        }
        return schoolRepository.findByUniversityIdAndStatusOrderByDisplayOrderAscIdAsc(universityId, "ACTIVE");
    }

    public Optional<UniversitySchool> getSchoolById(Long id) {
        return schoolRepository.findById(id);
    }

    @Transactional
    public UniversitySchool createSchool(Long universityId, UniversitySchool school) {
        if (universityId == null) {
            throw new IllegalArgumentException("University ID is required.");
        }
        if (school.getName() == null || school.getName().isBlank()) {
            throw new IllegalArgumentException("School name is required.");
        }
        if (school.getCode() == null || school.getCode().isBlank()) {
            throw new IllegalArgumentException("School code/abbreviation is required.");
        }

        String normalizedCode = school.getCode().trim().toUpperCase();
        if (schoolRepository.existsByUniversityIdAndCodeIgnoreCase(universityId, normalizedCode)) {
            throw new IllegalArgumentException("School code '" + normalizedCode + "' already exists for this university.");
        }

        school.setUniversityId(universityId);
        school.setCode(normalizedCode);
        school.setName(school.getName().trim());
        if (school.getGroupName() == null || school.getGroupName().isBlank()) {
            school.setGroupName("general");
        }
        if (school.getStatus() == null || school.getStatus().isBlank()) {
            school.setStatus("ACTIVE");
        }
        return schoolRepository.save(school);
    }

    @Transactional
    public UniversitySchool updateSchool(Long schoolId, UniversitySchool updatedSchool) {
        UniversitySchool existing = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found with ID: " + schoolId));

        if (updatedSchool.getName() != null && !updatedSchool.getName().isBlank()) {
            existing.setName(updatedSchool.getName().trim());
        }

        if (updatedSchool.getCode() != null && !updatedSchool.getCode().isBlank()) {
            String newCode = updatedSchool.getCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(existing.getCode()) &&
                schoolRepository.existsByUniversityIdAndCodeIgnoreCase(existing.getUniversityId(), newCode)) {
                throw new IllegalArgumentException("School code '" + newCode + "' already exists for this university.");
            }
            existing.setCode(newCode);
        }

        if (updatedSchool.getGroupName() != null && !updatedSchool.getGroupName().isBlank()) {
            existing.setGroupName(updatedSchool.getGroupName().trim());
        }

        if (updatedSchool.getStatus() != null && !updatedSchool.getStatus().isBlank()) {
            existing.setStatus(updatedSchool.getStatus().trim());
        }

        if (updatedSchool.getDisplayOrder() != null) {
            existing.setDisplayOrder(updatedSchool.getDisplayOrder());
        }

        return schoolRepository.save(existing);
    }

    @Transactional
    public void deleteSchool(Long schoolId) {
        schoolRepository.deleteById(schoolId);
    }
}
