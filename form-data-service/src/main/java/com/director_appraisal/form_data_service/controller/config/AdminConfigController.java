package com.director_appraisal.form_data_service.controller.config;

import com.director_appraisal.form_data_service.dto.config.*;
import com.director_appraisal.form_data_service.model.config.*;
import com.director_appraisal.form_data_service.repository.config.*;
import com.director_appraisal.form_data_service.service.config.FormConfigService;
import com.director_appraisal.form_data_service.service.config.SchemaCompilerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@CrossOrigin
public class AdminConfigController {

    private final FormSchemaRepository formSchemaRepository;
    private final SchemaVersionRepository schemaVersionRepository;
    private final FormSectionRepository formSectionRepository;
    private final FormTableRepository formTableRepository;
    private final FormFieldRepository formFieldRepository;
    private final UniversityRepository universityRepository;
    private final FormConfigService formConfigService;
    private final SchemaCompilerService schemaCompilerService;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_ADMIN_ROLES = Set.of("super_admin", "admin", "iqac", "director");

    private void validateAdminRole(String userRole) {
        if (userRole != null && !ALLOWED_ADMIN_ROLES.contains(userRole.trim().toLowerCase(Locale.ROOT))) {
            throw new SecurityException("Access denied. Administrative role required.");
        }
    }

    // 1. Schemas Lifecycle
    @GetMapping("/schemas")
    public ResponseEntity<List<FormSchema>> getSchemas(
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) String universityCode) {

        Long targetUniId = universityId;
        if (targetUniId == null && universityCode != null && !universityCode.isBlank()) {
            targetUniId = universityRepository.findByCodeIgnoreCase(universityCode.trim())
                    .map(University::getId)
                    .orElse(1L);
        }
        if (targetUniId == null) {
            targetUniId = 1L;
        }

        List<FormSchema> schemas = formSchemaRepository.findByUniversityId(targetUniId);
        return ResponseEntity.ok(schemas);
    }

    @PostMapping("/schemas")
    public ResponseEntity<FormSchema> createSchema(
            @RequestBody FormSchema schemaReq,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminRole(userRole);
        if (schemaReq.getUniversityId() == null) {
            schemaReq.setUniversityId(1L);
        }
        if (schemaReq.getAuditType() == null || schemaReq.getAuditType().isBlank()) {

            throw new IllegalArgumentException("Audit type is required.");
        }
        if (schemaReq.getName() == null || schemaReq.getName().isBlank()) {
            throw new IllegalArgumentException("Schema name is required.");
        }

        FormSchema saved = formSchemaRepository.save(schemaReq);
        // Create initial v1 draft
        formConfigService.createDraftVersion(saved.getId(), "admin");
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/schemas/{schemaId}")
    public ResponseEntity<Map<String, Object>> getSchemaDetails(@PathVariable Long schemaId) {
        FormSchema schema = formSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        List<SchemaVersion> versions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schemaId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", schema);
        result.put("versions", versions);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/schemas/{schemaId}")
    public ResponseEntity<Map<String, Object>> deleteSchema(
            @PathVariable Long schemaId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminRole(userRole);
        formConfigService.deleteSchema(schemaId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Schema deleted successfully."));
    }

    // 2. Version Lifecycle
    @PostMapping("/schemas/{schemaId}/draft")
    public ResponseEntity<SchemaVersion> createDraft(
            @PathVariable Long schemaId,
            @RequestParam(required = false, defaultValue = "admin") String createdBy) {
        SchemaVersion draft = formConfigService.createDraftVersion(schemaId, createdBy);
        return ResponseEntity.ok(draft);
    }

    @DeleteMapping("/versions/{versionId}")
    public ResponseEntity<Map<String, Object>> deleteVersion(
            @PathVariable Long versionId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminRole(userRole);
        formConfigService.deleteVersion(versionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Version deleted successfully."));
    }

    @GetMapping("/versions/{versionId}")
    public ResponseEntity<CompiledSchemaDto> getVersionTree(@PathVariable Long versionId) {
        CompiledSchemaDto tree = schemaCompilerService.compile(versionId);
        return ResponseEntity.ok(tree);
    }

    @PostMapping("/versions/{versionId}/publish")
    public ResponseEntity<CompiledSchemaDto> publishVersion(
            @PathVariable Long versionId,
            @RequestParam(required = false, defaultValue = "admin") String publisher) {
        CompiledSchemaDto published = formConfigService.publishVersion(versionId, publisher);
        return ResponseEntity.ok(published);
    }

    @PostMapping("/schemas/{schemaId}/rollback")
    public ResponseEntity<Map<String, Object>> rollback(
            @PathVariable Long schemaId,
            @RequestParam Long targetVersionId) {
        formConfigService.rollbackVersion(schemaId, targetVersionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Rolled back successfully."));
    }

    // 3. Sections Management
    @PostMapping("/sections")
    public ResponseEntity<FormSection> createSection(@RequestBody FormSection req) {
        if (req.getVersionId() == null) throw new IllegalArgumentException("versionId is required.");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new IllegalArgumentException("title is required.");
        if (req.getSectionKey() == null || req.getSectionKey().isBlank()) {
            req.setSectionKey(FormConfigService.toSnakeCase(req.getTitle()));
        }

        List<FormSection> existing = formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(req.getVersionId());
        req.setDisplayOrder(existing.size() + 1);
        FormSection saved = formSectionRepository.save(req);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<FormSection> updateSection(@PathVariable Long id, @RequestBody FormSection req) {
        FormSection existing = formSectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        if (req.getTitle() != null) existing.setTitle(req.getTitle());
        if (req.getSectionKey() != null) existing.setSectionKey(req.getSectionKey());
        if (req.getSectionNumber() != null) existing.setSectionNumber(req.getSectionNumber());
        if (req.getOwnerRole() != null) existing.setOwnerRole(req.getOwnerRole());
        if (req.getDescription() != null) existing.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) existing.setDisplayOrder(req.getDisplayOrder());

        return ResponseEntity.ok(formSectionRepository.save(existing));
    }

    @DeleteMapping("/sections/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteSection(@PathVariable Long id) {
        List<FormTable> tables = formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(id);
        for (FormTable t : tables) {
            formFieldRepository.deleteByTableId(t.getId());
        }
        formTableRepository.deleteBySectionId(id);
        formFieldRepository.deleteBySectionId(id);
        formSectionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Section deleted."));
    }

    @PutMapping("/versions/{versionId}/reorder-sections")
    @Transactional
    public ResponseEntity<Map<String, Object>> reorderSections(
            @PathVariable Long versionId,
            @RequestBody List<Long> sectionIds) {
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sId = sectionIds.get(i);
            int order = i + 1;
            formSectionRepository.findById(sId).ifPresent(sec -> {
                sec.setDisplayOrder(order);
                formSectionRepository.save(sec);
            });
        }
        return ResponseEntity.ok(Map.of("success", true));
    }


    // 4. Tables Management
    @PostMapping("/tables")
    public ResponseEntity<FormTable> createTable(@RequestBody FormTable req) {
        if (req.getSectionId() == null) throw new IllegalArgumentException("sectionId is required.");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new IllegalArgumentException("title is required.");
        if (req.getTableKey() == null || req.getTableKey().isBlank()) {
            req.setTableKey(FormConfigService.toSnakeCase(req.getTitle()));
        }

        List<FormTable> existing = formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(req.getSectionId());
        req.setDisplayOrder(existing.size() + 1);
        FormTable saved = formTableRepository.save(req);

        // Auto-add Sr No default column
        FormField srNo = FormField.builder()
                .sectionId(req.getSectionId())
                .tableId(saved.getId())
                .fieldKey("sr_no")
                .label("Sr No")
                .fieldType("TEXT")
                .displayOrder(1)
                .isRequired(false)
                .build();
        formFieldRepository.save(srNo);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<FormTable> updateTable(@PathVariable Long id, @RequestBody FormTable req) {
        FormTable existing = formTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));

        if (req.getTitle() != null) existing.setTitle(req.getTitle());
        if (req.getTableKey() != null) existing.setTableKey(req.getTableKey());
        if (req.getShowTitle() != null) existing.setShowTitle(req.getShowTitle());
        if (req.getIsRepeatable() != null) existing.setIsRepeatable(req.getIsRepeatable());
        if (req.getDisplayOrder() != null) existing.setDisplayOrder(req.getDisplayOrder());
        if (req.getInitialRows() != null) existing.setInitialRows(req.getInitialRows());
        if (req.getSelectOptions() != null) existing.setSelectOptions(req.getSelectOptions());
        if (req.getDateColumns() != null) existing.setDateColumns(req.getDateColumns());
        if (req.getNumberColumns() != null) existing.setNumberColumns(req.getNumberColumns());
        if (req.getTextareaColumns() != null) existing.setTextareaColumns(req.getTextareaColumns());

        return ResponseEntity.ok(formTableRepository.save(existing));
    }

    @DeleteMapping("/tables/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteTable(@PathVariable Long id) {
        formFieldRepository.deleteByTableId(id);
        formTableRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Table deleted."));
    }

    @PutMapping("/sections/{sectionId}/reorder-tables")
    @Transactional
    public ResponseEntity<Map<String, Object>> reorderTables(
            @PathVariable Long sectionId,
            @RequestBody List<Long> tableIds) {
        for (int i = 0; i < tableIds.size(); i++) {
            Long tId = tableIds.get(i);
            int order = i + 1;
            formTableRepository.findById(tId).ifPresent(tbl -> {
                tbl.setDisplayOrder(order);
                formTableRepository.save(tbl);
            });
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 5. Fields Management
    @PostMapping("/fields")
    public ResponseEntity<FormField> createField(@RequestBody FormField req) {
        if (req.getSectionId() == null) throw new IllegalArgumentException("sectionId is required.");
        if (req.getLabel() == null || req.getLabel().isBlank()) throw new IllegalArgumentException("label is required.");
        if (req.getFieldKey() == null || req.getFieldKey().isBlank()) {
            req.setFieldKey(FormConfigService.toSnakeCase(req.getLabel()));
        }
        if (req.getFieldType() == null || req.getFieldType().isBlank()) {
            req.setFieldType("TEXT");
        }

        List<FormField> existing = req.getTableId() != null
                ? formFieldRepository.findByTableIdOrderByDisplayOrderAscIdAsc(req.getTableId())
                : formFieldRepository.findBySectionIdAndTableIdIsNullOrderByDisplayOrderAscIdAsc(req.getSectionId());

        req.setDisplayOrder(existing.size() + 1);
        FormField saved = formFieldRepository.save(req);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/fields/{id}")
    public ResponseEntity<FormField> updateField(@PathVariable Long id, @RequestBody FormField req) {
        FormField existing = formFieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + id));

        if (req.getLabel() != null) existing.setLabel(req.getLabel());
        if (req.getFieldKey() != null) existing.setFieldKey(req.getFieldKey());
        if (req.getFieldType() != null) existing.setFieldType(req.getFieldType());
        if (req.getKind() != null) existing.setKind(req.getKind());
        if (req.getIsRequired() != null) existing.setIsRequired(req.getIsRequired());
        if (req.getPlaceholder() != null) existing.setPlaceholder(req.getPlaceholder());
        if (req.getDefaultValue() != null) existing.setDefaultValue(req.getDefaultValue());
        if (req.getValidationRules() != null) existing.setValidationRules(req.getValidationRules());
        if (req.getOptions() != null) existing.setOptions(req.getOptions());
        if (req.getAttachmentRules() != null) existing.setAttachmentRules(req.getAttachmentRules());
        if (req.getDisplayOrder() != null) existing.setDisplayOrder(req.getDisplayOrder());

        return ResponseEntity.ok(formFieldRepository.save(existing));
    }

    @DeleteMapping("/fields/{id}")
    public ResponseEntity<Map<String, Object>> deleteField(@PathVariable Long id) {
        formFieldRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Field deleted."));
    }

    @PutMapping("/tables/{tableId}/reorder-fields")
    @Transactional
    public ResponseEntity<Map<String, Object>> reorderFields(
            @PathVariable Long tableId,
            @RequestBody List<Long> fieldIds) {
        for (int i = 0; i < fieldIds.size(); i++) {
            Long fId = fieldIds.get(i);
            int order = i + 1;
            formFieldRepository.findById(fId).ifPresent(f -> {
                f.setDisplayOrder(order);
                formFieldRepository.save(f);
            });
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}

