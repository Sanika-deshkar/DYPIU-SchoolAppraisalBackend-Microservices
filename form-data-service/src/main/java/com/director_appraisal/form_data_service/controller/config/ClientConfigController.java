package com.director_appraisal.form_data_service.controller.config;

import com.director_appraisal.form_data_service.dto.config.CompiledSchemaDto;
import com.director_appraisal.form_data_service.model.config.University;
import com.director_appraisal.form_data_service.service.config.FormConfigService;
import com.director_appraisal.form_data_service.service.config.UniversityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@CrossOrigin
public class ClientConfigController {

    private final FormConfigService formConfigService;
    private final UniversityService universityService;

    @GetMapping("/active")
    public ResponseEntity<CompiledSchemaDto> getActiveSchema(
            @RequestParam(required = false, defaultValue = "academic") String auditType,
            @RequestParam(required = false) String universityCode,
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) String school,
            @RequestHeader(value = "X-University-Code", required = false) String headerUniversityCode,
            @RequestHeader(value = "X-University-Id", required = false) Long headerUniversityId,
            @RequestHeader(value = "X-User-School", required = false) String headerSchool) {

        String code = universityCode != null && !universityCode.isBlank() ? universityCode : headerUniversityCode;
        Long uId = universityId != null ? universityId : headerUniversityId;
        if ((code == null || code.isBlank()) && uId != null) {
            code = universityService.getById(uId).map(University::getCode).orElse("dypiu");
        }
        if (code == null || code.isBlank()) {
            code = "dypiu";
        }

        String schoolToUse = (school != null && !school.isBlank()) ? school : headerSchool;
        CompiledSchemaDto compiled = formConfigService.getActiveCompiledSchema(code, auditType, schoolToUse);
        return ResponseEntity.ok(compiled);
    }

    @GetMapping("/version/{versionId}")
    public ResponseEntity<CompiledSchemaDto> getSchemaByVersion(@PathVariable Long versionId) {
        CompiledSchemaDto compiled = formConfigService.getCompiledSchemaByVersion(versionId);
        return ResponseEntity.ok(compiled);
    }

    @GetMapping("/branding")
    public ResponseEntity<Map<String, Object>> getBranding(
            @RequestParam(required = false) String universityCode,
            @RequestParam(required = false) Long universityId,
            @RequestHeader(value = "X-University-Code", required = false) String headerUniversityCode,
            @RequestHeader(value = "X-University-Id", required = false) Long headerUniversityId) {

        String code = universityCode != null && !universityCode.isBlank() ? universityCode : headerUniversityCode;
        Long uId = universityId != null ? universityId : headerUniversityId;

        University u = null;
        if (code != null && !code.isBlank()) {
            u = universityService.getByCode(code).orElse(null);
        } else if (uId != null) {
            u = universityService.getById(uId).orElse(null);
        }
        if (u == null) {
            u = universityService.getByCode("dypiu").orElse(null);
        }

        if (u == null) {
            return ResponseEntity.ok(Map.of(
                    "universityName", "D Y Patil International University Akurdi Pune",
                    "code", "dypiu",
                    "address", "Sector 29, Pradhikaran, Akurdi, Pune - Maharashtra, INDIA 411044",
                    "act", "Establishment by Maharashtra Act No. LXIII of 2017"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "code", u.getCode(),
                "universityName", u.getName(),
                "domain", u.getDomain() != null ? u.getDomain() : "",
                "address", u.getAddress() != null ? u.getAddress() : "",
                "act", u.getEstablishmentAct() != null ? u.getEstablishmentAct() : "",
                "logoUrl", u.getLogoUrl() != null ? u.getLogoUrl() : "",
                "iqacLogoUrl", u.getIqacLogoUrl() != null ? u.getIqacLogoUrl() : "",
                "primaryColor", u.getPrimaryColor() != null ? u.getPrimaryColor() : "#1e3a8a",
                "themeBranding", u.getThemeBranding() != null ? u.getThemeBranding() : "{}"
        ));
    }
}
