package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.model.config.UniversityPost;
import com.director_appraisal.form_data_service.repository.config.UniversityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityPostService {

    private final UniversityPostRepository universityPostRepository;

    @Transactional
    public List<UniversityPost> getPostsByUniversity(Long universityId, boolean activeOnly) {
        if (universityId == null) {
            universityId = 1L;
        }

        List<UniversityPost> existing = activeOnly
                ? universityPostRepository.findByUniversityIdAndStatusOrderByDisplayOrderAscNameAsc(universityId, "ACTIVE")
                : universityPostRepository.findByUniversityIdOrderByDisplayOrderAscNameAsc(universityId);

        if (existing.isEmpty()) {
            seedDefaultPosts(universityId);
            existing = activeOnly
                    ? universityPostRepository.findByUniversityIdAndStatusOrderByDisplayOrderAscNameAsc(universityId, "ACTIVE")
                    : universityPostRepository.findByUniversityIdOrderByDisplayOrderAscNameAsc(universityId);
        }

        return existing;
    }

    @Transactional
    public UniversityPost createPost(Long universityId, UniversityPost req) {
        if (universityId == null) {
            universityId = 1L;
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Post name is required.");
        }

        String code = req.getCode();
        if (code == null || code.isBlank()) {
            code = req.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        } else {
            code = code.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }

        if (universityPostRepository.existsByUniversityIdAndCodeIgnoreCase(universityId, code)) {
            throw new IllegalArgumentException("Administrative post with code '" + code + "' already exists for this university.");
        }

        req.setUniversityId(universityId);
        req.setCode(code);
        if (req.getStatus() == null || req.getStatus().isBlank()) {
            req.setStatus("ACTIVE");
        }

        List<UniversityPost> current = universityPostRepository.findByUniversityIdOrderByDisplayOrderAscNameAsc(universityId);
        req.setDisplayOrder(current.size() + 1);

        UniversityPost saved = universityPostRepository.save(req);
        log.info("Created administrative post '{}' ({}) for university {}", saved.getName(), saved.getCode(), universityId);
        return saved;
    }

    @Transactional
    public UniversityPost updatePost(Long postId, UniversityPost req) {
        UniversityPost existing = universityPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        if (req.getName() != null && !req.getName().isBlank()) {
            existing.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription().trim());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            existing.setStatus(req.getStatus().trim().toUpperCase());
        }
        if (req.getDisplayOrder() != null) {
            existing.setDisplayOrder(req.getDisplayOrder());
        }

        return universityPostRepository.save(existing);
    }

    @Transactional
    public void deletePost(Long postId) {
        if (universityPostRepository.existsById(postId)) {
            universityPostRepository.deleteById(postId);
            log.info("Deleted administrative post {}", postId);
        }
    }

    private void seedDefaultPosts(Long universityId) {
        log.info("Seeding default administrative posts for university {}", universityId);
        List<UniversityPost> defaults = List.of(
                UniversityPost.builder().universityId(universityId).code("registrar").name("Registrar").description("University Registrar and administrative head").displayOrder(1).status("ACTIVE").build(),
                UniversityPost.builder().universityId(universityId).code("hr").name("HR (Human Resources)").description("Human Resources department").displayOrder(2).status("ACTIVE").build(),
                UniversityPost.builder().universityId(universityId).code("dean-student-welfare").name("Dean Student Welfare").description("Student Welfare & Development office").displayOrder(3).status("ACTIVE").build(),
                UniversityPost.builder().universityId(universityId).code("dean-placement").name("Dean Placement").description("Training and Placement cell").displayOrder(4).status("ACTIVE").build()
        );
        universityPostRepository.saveAll(defaults);
    }
}
