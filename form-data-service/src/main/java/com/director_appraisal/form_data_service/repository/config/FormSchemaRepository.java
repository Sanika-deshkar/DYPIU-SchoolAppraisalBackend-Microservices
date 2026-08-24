package com.director_appraisal.form_data_service.repository.config;

import com.director_appraisal.form_data_service.model.config.FormSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSchemaRepository extends JpaRepository<FormSchema, Long> {
    List<FormSchema> findByUniversityId(Long universityId);
    List<FormSchema> findByUniversityIdAndAuditTypeIgnoreCase(Long universityId, String auditType);
    Optional<FormSchema> findFirstByUniversityIdAndAuditTypeIgnoreCaseOrderByIdAsc(Long universityId, String auditType);
}
