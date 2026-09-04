package com.director_appraisal.form_data_service.repository.config;

import com.director_appraisal.form_data_service.model.config.UniversityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversityPostRepository extends JpaRepository<UniversityPost, Long> {

    List<UniversityPost> findByUniversityIdOrderByDisplayOrderAscNameAsc(Long universityId);

    List<UniversityPost> findByUniversityIdAndStatusOrderByDisplayOrderAscNameAsc(Long universityId, String status);

    Optional<UniversityPost> findByUniversityIdAndCodeIgnoreCase(Long universityId, String code);

    boolean existsByUniversityIdAndCodeIgnoreCase(Long universityId, String code);
}
