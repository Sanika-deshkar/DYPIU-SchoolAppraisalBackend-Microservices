package com.director_appraisal.form_data_service.repository.config;

import com.director_appraisal.form_data_service.model.config.UniversitySchool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversitySchoolRepository extends JpaRepository<UniversitySchool, Long> {

    List<UniversitySchool> findByUniversityIdOrderByDisplayOrderAscIdAsc(Long universityId);

    List<UniversitySchool> findByUniversityIdAndStatusOrderByDisplayOrderAscIdAsc(Long universityId, String status);

    Optional<UniversitySchool> findByUniversityIdAndCodeIgnoreCase(Long universityId, String code);

    boolean existsByUniversityIdAndCodeIgnoreCase(Long universityId, String code);

    void deleteByUniversityId(Long universityId);
}
