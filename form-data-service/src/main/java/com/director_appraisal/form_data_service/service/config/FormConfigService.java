package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.dto.config.*;
import com.director_appraisal.form_data_service.model.config.*;
import com.director_appraisal.form_data_service.repository.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormConfigService {

    private final FormSchemaRepository formSchemaRepository;
    private final SchemaVersionRepository schemaVersionRepository;
    private final FormSectionRepository formSectionRepository;
    private final FormTableRepository formTableRepository;
    private final FormFieldRepository formFieldRepository;
    private final UniversityRepository universityRepository;
    private final SchemaCompilerService schemaCompilerService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CompiledSchemaDto getActiveCompiledSchema(String universityCode, String auditType) {
        String code = (universityCode != null && !universityCode.isBlank()) ? universityCode.trim().toLowerCase() : "dypiu";
        String type = (auditType != null && !auditType.isBlank()) ? auditType.trim().toLowerCase() : "academic";

        University university = universityRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> universityRepository.findByCodeIgnoreCase("dypiu").orElse(null));

        if (university == null) {
            throw new IllegalArgumentException("University not found for code: " + code);
        }

        FormSchema schema = formSchemaRepository.findByUniversityIdAndAuditTypeIgnoreCase(university.getId(), type)
                .orElseThrow(() -> new IllegalArgumentException("Form schema not found for " + code + " and " + type));

        Long versionId = schema.getActiveVersionId();
        if (versionId == null) {
            List<SchemaVersion> versions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schema.getId());
            if (versions.isEmpty()) {
                throw new IllegalStateException("No versions found for schema: " + schema.getName());
            }
            versionId = versions.get(0).getId();
        }

        return getCompiledSchemaByVersion(versionId);
    }

    @Transactional(readOnly = true)
    public CompiledSchemaDto getCompiledSchemaByVersion(Long versionId) {
        SchemaVersion version = schemaVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        if (version.getCompiledSchema() != null && !version.getCompiledSchema().isBlank()) {
            try {
                return objectMapper.readValue(version.getCompiledSchema(), CompiledSchemaDto.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached compiled schema, recompiling version {}: {}", versionId, e.getMessage());
            }
        }

        return schemaCompilerService.compile(versionId);
    }

    @Transactional
    public SchemaVersion createDraftVersion(Long schemaId, String createdBy) {
        FormSchema schema = formSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        List<SchemaVersion> existingVersions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schemaId);
        
        Optional<SchemaVersion> existingDraft = existingVersions.stream()
                .filter(v -> "DRAFT".equalsIgnoreCase(v.getStatus()))
                .findFirst();
        if (existingDraft.isPresent()) {
            return existingDraft.get();
        }

        int nextVersionNumber = 1;
        SchemaVersion sourceVersion = null;
        if (!existingVersions.isEmpty()) {
            sourceVersion = existingVersions.get(0);
            nextVersionNumber = sourceVersion.getVersionNumber() + 1;
        }

        SchemaVersion draft = SchemaVersion.builder()
                .schemaId(schema.getId())
                .versionNumber(nextVersionNumber)
                .status("DRAFT")
                .academicYear(sourceVersion != null ? sourceVersion.getAcademicYear() : "2025-26")
                .title(sourceVersion != null ? sourceVersion.getTitle() : schema.getName())
                .ownerRole(sourceVersion != null ? sourceVersion.getOwnerRole() : ("administrative".equalsIgnoreCase(schema.getAuditType()) ? "registrar" : "director-schools"))
                .publishedBy(createdBy)
                .build();

        SchemaVersion savedDraft = schemaVersionRepository.save(draft);

        if (sourceVersion != null) {
            cloneVersionTree(sourceVersion.getId(), savedDraft.getId());
        }

        return savedDraft;
    }

    private void cloneVersionTree(Long sourceVersionId, Long targetVersionId) {
        List<FormSection> sourceSections = formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(sourceVersionId);

        for (FormSection srcSec : sourceSections) {
            FormSection newSec = FormSection.builder()
                    .versionId(targetVersionId)
                    .sectionKey(srcSec.getSectionKey())
                    .title(srcSec.getTitle())
                    .sectionNumber(srcSec.getSectionNumber())
                    .ownerRole(srcSec.getOwnerRole())
                    .description(srcSec.getDescription())
                    .displayOrder(srcSec.getDisplayOrder())
                    .build();
            FormSection savedSec = formSectionRepository.save(newSec);

            List<FormField> topFields = formFieldRepository.findBySectionIdAndTableIdIsNullOrderByDisplayOrderAscIdAsc(srcSec.getId());
            for (FormField f : topFields) {
                FormField newField = FormField.builder()
                        .sectionId(savedSec.getId())
                        .tableId(null)
                        .fieldKey(f.getFieldKey())
                        .label(f.getLabel())
                        .fieldType(f.getFieldType())
                        .kind(f.getKind())
                        .isRequired(f.getIsRequired())
                        .placeholder(f.getPlaceholder())
                        .defaultValue(f.getDefaultValue())
                        .validationRules(f.getValidationRules())
                        .options(f.getOptions())
                        .attachmentRules(f.getAttachmentRules())
                        .displayOrder(f.getDisplayOrder())
                        .build();
                formFieldRepository.save(newField);
            }

            List<FormTable> tables = formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(srcSec.getId());
            for (FormTable tbl : tables) {
                FormTable newTbl = FormTable.builder()
                        .sectionId(savedSec.getId())
                        .tableKey(tbl.getTableKey())
                        .title(tbl.getTitle())
                        .showTitle(tbl.getShowTitle())
                        .isRepeatable(tbl.getIsRepeatable())
                        .displayOrder(tbl.getDisplayOrder())
                        .initialRows(tbl.getInitialRows())
                        .selectOptions(tbl.getSelectOptions())
                        .dateColumns(tbl.getDateColumns())
                        .numberColumns(tbl.getNumberColumns())
                        .textareaColumns(tbl.getTextareaColumns())
                        .textareaMaxLengths(tbl.getTextareaMaxLengths())
                        .build();
                FormTable savedTbl = formTableRepository.save(newTbl);

                List<FormField> colFields = formFieldRepository.findByTableIdOrderByDisplayOrderAscIdAsc(tbl.getId());
                for (FormField cf : colFields) {
                    FormField newCol = FormField.builder()
                            .sectionId(savedSec.getId())
                            .tableId(savedTbl.getId())
                            .fieldKey(cf.getFieldKey())
                            .label(cf.getLabel())
                            .fieldType(cf.getFieldType())
                            .kind(cf.getKind())
                            .isRequired(cf.getIsRequired())
                            .placeholder(cf.getPlaceholder())
                            .defaultValue(cf.getDefaultValue())
                            .validationRules(cf.getValidationRules())
                            .options(cf.getOptions())
                            .attachmentRules(cf.getAttachmentRules())
                            .displayOrder(cf.getDisplayOrder())
                            .build();
                    formFieldRepository.save(newCol);
                }
            }
        }
    }

    @Transactional
    public CompiledSchemaDto publishVersion(Long versionId, String publisher) {
        SchemaVersion version = schemaVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        FormSchema schema = formSchemaRepository.findById(version.getSchemaId())
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + version.getSchemaId()));

        validateVersionIntegrity(versionId);

        CompiledSchemaDto compiled = schemaCompilerService.compile(versionId);
        try {
            version.setCompiledSchema(objectMapper.writeValueAsString(compiled));
        } catch (Exception e) {
            log.error("Failed to serialize compiled schema for version {}: {}", versionId, e.getMessage());
        }

        version.setStatus("PUBLISHED");
        version.setPublishedBy(publisher);
        version.setPublishedAt(LocalDateTime.now());
        schemaVersionRepository.save(version);

        schema.setActiveVersionId(version.getId());
        schema.setActiveVersionNumber(version.getVersionNumber());
        formSchemaRepository.save(schema);

        log.info("Successfully published schema '{}' version {} (ID: {})", schema.getName(), version.getVersionNumber(), version.getId());
        return compiled;
    }

    @Transactional
    public void rollbackVersion(Long schemaId, Long targetVersionId) {
        FormSchema schema = formSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        SchemaVersion target = schemaVersionRepository.findById(targetVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Target version not found: " + targetVersionId));

        if (!target.getSchemaId().equals(schemaId)) {
            throw new IllegalArgumentException("Version does not belong to schema: " + schemaId);
        }

        if (!"PUBLISHED".equalsIgnoreCase(target.getStatus())) {
            throw new IllegalStateException("Cannot rollback to a non-published version.");
        }

        schema.setActiveVersionId(target.getId());
        schema.setActiveVersionNumber(target.getVersionNumber());
        formSchemaRepository.save(schema);
        log.info("Rolled back schema '{}' to version {} (ID: {})", schema.getName(), target.getVersionNumber(), target.getId());
    }

    @Transactional
    public void deleteVersion(Long versionId) {
        SchemaVersion version = schemaVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        Long schemaId = version.getSchemaId();

        // 1. Delete all fields, tables, sections
        List<FormSection> sections = formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(versionId);
        for (FormSection s : sections) {
            List<FormTable> tables = formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(s.getId());
            for (FormTable t : tables) {
                formFieldRepository.deleteByTableId(t.getId());
            }
            formTableRepository.deleteBySectionId(s.getId());
            formFieldRepository.deleteBySectionId(s.getId());
        }
        formSectionRepository.deleteByVersionId(versionId);

        // 2. Delete the version
        schemaVersionRepository.deleteById(versionId);

        // 3. Update active version on parent schema if this was the active version
        if (schemaId != null) {
            formSchemaRepository.findById(schemaId).ifPresent(schema -> {
                if (versionId.equals(schema.getActiveVersionId())) {
                    List<SchemaVersion> remainingVersions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schemaId);
                    Optional<SchemaVersion> latestPublished = remainingVersions.stream()
                            .filter(v -> "PUBLISHED".equalsIgnoreCase(v.getStatus()))
                            .findFirst();
                    if (latestPublished.isPresent()) {
                        schema.setActiveVersionId(latestPublished.get().getId());
                        schema.setActiveVersionNumber(latestPublished.get().getVersionNumber());
                    } else if (!remainingVersions.isEmpty()) {
                        schema.setActiveVersionId(remainingVersions.get(0).getId());
                        schema.setActiveVersionNumber(remainingVersions.get(0).getVersionNumber());
                    } else {
                        schema.setActiveVersionId(null);
                        schema.setActiveVersionNumber(null);
                    }
                    formSchemaRepository.save(schema);
                }
            });
        }
        log.info("Deleted version {}", versionId);
    }

    @Transactional
    public void deleteSchema(Long schemaId) {
        FormSchema schema = formSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        List<SchemaVersion> versions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schemaId);
        for (SchemaVersion v : versions) {
            deleteVersion(v.getId());
        }

        formSchemaRepository.deleteById(schemaId);
        log.info("Deleted schema '{}' (ID: {})", schema.getName(), schemaId);
    }

    private void validateVersionIntegrity(Long versionId) {
        List<FormSection> sections = formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(versionId);
        if (sections.isEmpty()) {
            throw new IllegalStateException("Cannot publish a schema with 0 sections.");
        }

        Set<String> sectionKeys = new HashSet<>();
        for (FormSection s : sections) {
            if (s.getSectionKey() == null || s.getSectionKey().isBlank()) {
                throw new IllegalStateException("Section key cannot be blank.");
            }
            if (!sectionKeys.add(s.getSectionKey().toLowerCase())) {
                throw new IllegalStateException("Duplicate section key found: " + s.getSectionKey());
            }

            List<FormTable> tables = formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(s.getId());
            Set<String> tableKeys = new HashSet<>();
            for (FormTable t : tables) {
                if (t.getTableKey() == null || t.getTableKey().isBlank()) {
                    throw new IllegalStateException("Table key cannot be blank in section: " + s.getTitle());
                }
                if (!tableKeys.add(t.getTableKey().toLowerCase())) {
                    throw new IllegalStateException("Duplicate table key in section " + s.getTitle() + ": " + t.getTableKey());
                }

                List<FormField> columns = formFieldRepository.findByTableIdOrderByDisplayOrderAscIdAsc(t.getId());
                if (columns.isEmpty()) {
                    throw new IllegalStateException("Table '" + t.getTitle() + "' must have at least one column.");
                }
            }
        }
    }

    public static String toSnakeCase(String label) {
        if (label == null) return "field";
        String s = label.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "_").toLowerCase();
        return s.isBlank() ? "field_" + System.currentTimeMillis() : s;
    }
}
