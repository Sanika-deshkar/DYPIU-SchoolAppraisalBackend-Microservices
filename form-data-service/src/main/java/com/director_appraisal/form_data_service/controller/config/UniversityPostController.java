package com.director_appraisal.form_data_service.controller.config;

import com.director_appraisal.form_data_service.model.config.UniversityPost;
import com.director_appraisal.form_data_service.service.config.UniversityPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/admin/config/universities/{universityId}/posts", "/api/config/universities/{universityId}/posts"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UniversityPostController {

    private final UniversityPostService universityPostService;

    @GetMapping
    public ResponseEntity<List<UniversityPost>> getPosts(
            @PathVariable Long universityId,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        List<UniversityPost> posts = universityPostService.getPostsByUniversity(universityId, !all);
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    public ResponseEntity<UniversityPost> createPost(
            @PathVariable Long universityId,
            @RequestBody UniversityPost req) {
        UniversityPost saved = universityPostService.createPost(universityId, req);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<UniversityPost> updatePost(
            @PathVariable Long universityId,
            @PathVariable Long postId,
            @RequestBody UniversityPost req) {
        UniversityPost updated = universityPostService.updatePost(postId, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> deletePost(
            @PathVariable Long universityId,
            @PathVariable Long postId) {
        universityPostService.deletePost(postId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Administrative post deleted."));
    }
}
