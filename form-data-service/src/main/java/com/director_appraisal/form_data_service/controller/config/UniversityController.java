package com.director_appraisal.form_data_service.controller.config;

import com.director_appraisal.form_data_service.model.config.University;
import com.director_appraisal.form_data_service.service.config.UniversityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/universities", "/api/config/universities"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UniversityController {

    private final UniversityService universityService;

    @GetMapping
    public ResponseEntity<List<University>> getAllUniversities() {
        return ResponseEntity.ok(universityService.getAllUniversities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<University> getById(@PathVariable Long id) {
        return universityService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<University> getByCode(@PathVariable String code) {
        return universityService.getByCode(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<University> createUniversity(@RequestBody University req) {
        University created = universityService.createUniversity(req);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<University> updateUniversity(@PathVariable Long id, @RequestBody University req) {
        University updated = universityService.updateUniversity(id, req);
        return ResponseEntity.ok(updated);
    }
}
