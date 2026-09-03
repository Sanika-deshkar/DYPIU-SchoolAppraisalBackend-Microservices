package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.dto.config.*;
import com.director_appraisal.form_data_service.model.config.*;
import com.director_appraisal.form_data_service.repository.config.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaCompilerService {

    private final FormSchemaRepository formSchemaRepository;
    private final SchemaVersionRepository schemaVersionRepository;
    private final FormSectionRepository formSectionRepository;
    private final FormTableRepository formTableRepository;
    private final FormFieldRepository formFieldRepository;
    private final UniversityRepository universityRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CompiledSchemaDto compile(Long versionId) {
        SchemaVersion version = schemaVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("SchemaVersion not found: " + versionId));

        FormSchema schema = formSchemaRepository.findById(version.getSchemaId())
                .orElseThrow(() -> new IllegalArgumentException("FormSchema not found: " + version.getSchemaId()));

        University university = null;
        if (schema.getUniversityId() != null) {
            university = universityRepository.findById(schema.getUniversityId()).orElse(null);
        }
        if (university == null) {
            university = universityRepository.findByCodeIgnoreCase("dypiu").orElse(null);
        }

        Map<String, Object> header = new LinkedHashMap<>();
        if (university != null) {
            header.put("university", university.getName());
            header.put("address", university.getAddress() != null ? university.getAddress() : "");
            header.put("act", university.getEstablishmentAct() != null ? university.getEstablishmentAct() : "");
            header.put("logoUrl", university.getLogoUrl());
            header.put("iqacLogoUrl", university.getIqacLogoUrl());
        } else {
            header.put("university", "D Y Patil International University Akurdi Pune");
            header.put("address", "Sector 29, Pradhikaran, Akurdi, Pune - Maharashtra, INDIA 411044");
            header.put("act", "Establishment by Maharashtra Act No. LXIII of 2017");
        }

        Map<String, Object> universityInfo = new LinkedHashMap<>(header);
        if (university != null) {
            universityInfo.put("code", university.getCode());
            universityInfo.put("domain", university.getDomain());
            universityInfo.put("primaryColor", university.getPrimaryColor());
        }

        List<FormSection> sections = formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(versionId);
        List<SectionDto> sectionDtos = new ArrayList<>();

        for (FormSection sec : sections) {
            List<FormField> topFields = formFieldRepository.findBySectionIdAndTableIdIsNullOrderByDisplayOrderAscIdAsc(sec.getId());
            List<FieldDto> topFieldDtos = topFields.stream().map(this::toFieldDto).toList();

            List<FormTable> tables = formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(sec.getId());
            List<TableDto> tableDtos = new ArrayList<>();

            for (FormTable tbl : tables) {
                List<FormField> colFields = formFieldRepository.findByTableIdOrderByDisplayOrderAscIdAsc(tbl.getId());
                List<FieldDto> colFieldDtos = colFields.stream().map(this::toFieldDto).toList();

                List<String> columnHeaders = new ArrayList<>();
                for (FieldDto f : colFieldDtos) {
                    columnHeaders.add(f.getLabel() != null ? f.getLabel() : f.getFieldKey());
                }

                TableDto tableDto = TableDto.builder()
                        .id(tbl.getId())
                        .idString(tbl.getTableKey())
                        .tableKey(tbl.getTableKey())
                        .title(tbl.getTitle())
                        .showTitle(tbl.getShowTitle())
                        .isRepeatable(tbl.getIsRepeatable())
                        .displayOrder(tbl.getDisplayOrder())
                        .columns(columnHeaders)
                        .fields(colFieldDtos)
                        .initialRows(parseJsonList(tbl.getInitialRows()))
                        .selectOptions(parseJsonMapList(tbl.getSelectOptions()))
                        .dateColumns(parseJsonStringList(tbl.getDateColumns()))
                        .numberColumns(parseJsonStringList(tbl.getNumberColumns()))
                        .textareaColumns(parseJsonStringList(tbl.getTextareaColumns()))
                        .textareaMaxLengths(parseJsonMapInteger(tbl.getTextareaMaxLengths()))
                        .build();

                tableDtos.add(tableDto);
            }

            SectionDto secDto = SectionDto.builder()
                    .id(sec.getId())
                    .idString(sec.getSectionKey())
                    .sectionKey(sec.getSectionKey())
                    .title(sec.getTitle())
                    .number(sec.getSectionNumber())
                    .ownerRole(sec.getOwnerRole())
                    .description(sec.getDescription())
                    .displayOrder(sec.getDisplayOrder())
                    .fields(topFieldDtos)
                    .tables(tableDtos)
                    .build();

            sectionDtos.add(secDto);
        }

        String formId = (schema.getAuditType() != null ? schema.getAuditType().toLowerCase() : "academic")
                + "-audit-" + (version.getAcademicYear() != null ? version.getAcademicYear().replace("/", "-") : "2025-26");

        return CompiledSchemaDto.builder()
                .id(formId)
                .schemaId(schema.getId())
                .versionId(version.getId())
                .versionNumber(version.getVersionNumber())
                .auditType(schema.getAuditType() != null ? schema.getAuditType().toLowerCase() : "academic")
                .title(version.getTitle() != null ? version.getTitle() : schema.getName())
                .academicYear(version.getAcademicYear() != null ? version.getAcademicYear() : "July, 2025 - June, 2026")
                .ownerRole(version.getOwnerRole() != null ? version.getOwnerRole() : "director-schools")
                .status(version.getStatus())
                .header(header)
                .universityInfo(universityInfo)
                .sections(sectionDtos)
                .build();
    }

    public FieldDto toFieldDto(FormField f) {
        return FieldDto.builder()
                .id(f.getId())
                .idString(f.getFieldKey())
                .fieldKey(f.getFieldKey())
                .label(f.getLabel())
                .fieldType(f.getFieldType() != null ? f.getFieldType() : "TEXT")
                .kind(f.getKind())
                .isRequired(Boolean.TRUE.equals(f.getIsRequired()))
                .placeholder(f.getPlaceholder())
                .defaultValue(f.getDefaultValue())
                .validationRules(parseJsonObject(f.getValidationRules()))
                .options(parseJsonStringList(f.getOptions()))
                .attachmentRules(parseJsonObject(f.getAttachmentRules()))
                .displayOrder(f.getDisplayOrder())
                .build();
    }

    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, List<String>> parseJsonMapList(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Integer> parseJsonMapInteger(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Object parseJsonObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}
