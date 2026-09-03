package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.dto.config.CompiledSchemaDto;
import com.director_appraisal.form_data_service.dto.config.SectionDto;
import com.director_appraisal.form_data_service.model.config.*;
import com.director_appraisal.form_data_service.repository.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Form Config Service - FormConfigService Tests")
class FormConfigServiceTest {

    @Mock
    private FormSchemaRepository formSchemaRepository;
    @Mock
    private SchemaVersionRepository schemaVersionRepository;
    @Mock
    private FormSectionRepository formSectionRepository;
    @Mock
    private FormTableRepository formTableRepository;
    @Mock
    private FormFieldRepository formFieldRepository;
    @Mock
    private UniversityRepository universityRepository;
    @Mock
    private SchemaCompilerService schemaCompilerService;

    private FormConfigService formConfigService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        formConfigService = new FormConfigService(
                formSchemaRepository,
                schemaVersionRepository,
                formSectionRepository,
                formTableRepository,
                formFieldRepository,
                universityRepository,
                schemaCompilerService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should create draft version and clone existing sections and tables")
    void testCreateDraftVersion() {
        FormSchema schema = FormSchema.builder()
                .id(1L)
                .universityId(1L)
                .auditType("academic")
                .name("Academic Audit")
                .activeVersionNumber(1)
                .activeVersionId(10L)
                .build();

        SchemaVersion sourceV1 = SchemaVersion.builder()
                .id(10L)
                .schemaId(1L)
                .versionNumber(1)
                .status("PUBLISHED")
                .academicYear("2025-26")
                .build();

        FormSection sec = FormSection.builder()
                .id(100L)
                .versionId(10L)
                .sectionKey("part-a")
                .title("Part A")
                .build();

        when(formSchemaRepository.findById(1L)).thenReturn(Optional.of(schema));
        when(schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(1L)).thenReturn(List.of(sourceV1));
        when(schemaVersionRepository.save(any())).thenAnswer(inv -> {
            SchemaVersion v = inv.getArgument(0);
            v.setId(20L);
            return v;
        });

        when(formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(10L)).thenReturn(List.of(sec));
        when(formSectionRepository.save(any())).thenAnswer(inv -> {
            FormSection s = inv.getArgument(0);
            s.setId(200L);
            return s;
        });
        when(formFieldRepository.findBySectionIdAndTableIdIsNullOrderByDisplayOrderAscIdAsc(100L)).thenReturn(List.of());
        when(formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(100L)).thenReturn(List.of());

        SchemaVersion draft = formConfigService.createDraftVersion(1L, "admin");
        assertNotNull(draft);
        assertEquals(2, draft.getVersionNumber());
        assertEquals("DRAFT", draft.getStatus());
        verify(formSectionRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Should validate schema and publish version successfully")
    void testPublishVersionSuccess() {
        FormSchema schema = FormSchema.builder()
                .id(1L)
                .universityId(1L)
                .auditType("academic")
                .name("Academic Audit")
                .build();

        SchemaVersion draft = SchemaVersion.builder()
                .id(20L)
                .schemaId(1L)
                .versionNumber(2)
                .status("DRAFT")
                .build();

        FormSection sec = FormSection.builder()
                .id(200L)
                .versionId(20L)
                .sectionKey("part-a")
                .title("Part A")
                .build();

        FormTable tbl = FormTable.builder()
                .id(300L)
                .sectionId(200L)
                .tableKey("table1")
                .title("Table 1")
                .build();

        FormField col = FormField.builder()
                .id(400L)
                .sectionId(200L)
                .tableId(300L)
                .fieldKey("col1")
                .label("Column 1")
                .build();

        when(schemaVersionRepository.findById(20L)).thenReturn(Optional.of(draft));
        when(formSchemaRepository.findById(1L)).thenReturn(Optional.of(schema));
        when(formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(20L)).thenReturn(List.of(sec));
        when(formTableRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(200L)).thenReturn(List.of(tbl));
        when(formFieldRepository.findByTableIdOrderByDisplayOrderAscIdAsc(300L)).thenReturn(List.of(col));

        CompiledSchemaDto compiledDto = CompiledSchemaDto.builder()
                .schemaId(1L)
                .versionId(20L)
                .versionNumber(2)
                .title("Academic Audit V2")
                .sections(List.of(SectionDto.builder().id(200L).sectionKey("part-a").build()))
                .build();

        when(schemaCompilerService.compile(20L)).thenReturn(compiledDto);

        CompiledSchemaDto result = formConfigService.publishVersion(20L, "admin");
        assertNotNull(result);
        assertEquals("PUBLISHED", draft.getStatus());
        assertEquals(20L, schema.getActiveVersionId());
        assertEquals(2, schema.getActiveVersionNumber());
        verify(schemaVersionRepository).save(draft);
        verify(formSchemaRepository).save(schema);
    }

    @Test
    @DisplayName("Should throw exception when attempting to publish schema with 0 sections")
    void testPublishEmptySchemaThrows() {
        FormSchema schema = FormSchema.builder().id(1L).build();
        SchemaVersion draft = SchemaVersion.builder().id(20L).schemaId(1L).build();

        when(schemaVersionRepository.findById(20L)).thenReturn(Optional.of(draft));
        when(formSchemaRepository.findById(1L)).thenReturn(Optional.of(schema));
        when(formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(20L)).thenReturn(List.of()); // 0 sections

        assertThrows(IllegalStateException.class, () -> formConfigService.publishVersion(20L, "admin"));
    }

    @Test
    @DisplayName("Should rollback active version to previous published version")
    void testRollbackVersion() {
        FormSchema schema = FormSchema.builder()
                .id(1L)
                .activeVersionId(20L)
                .activeVersionNumber(2)
                .name("Academic Audit")
                .build();

        SchemaVersion v1 = SchemaVersion.builder()
                .id(10L)
                .schemaId(1L)
                .versionNumber(1)
                .status("PUBLISHED")
                .build();

        when(formSchemaRepository.findById(1L)).thenReturn(Optional.of(schema));
        when(schemaVersionRepository.findById(10L)).thenReturn(Optional.of(v1));

        formConfigService.rollbackVersion(1L, 10L);
        assertEquals(10L, schema.getActiveVersionId());
        assertEquals(1, schema.getActiveVersionNumber());
        verify(formSchemaRepository).save(schema);
    }

    @Test
    @DisplayName("Should compile version with real SchemaCompilerService even if 0 sections and universityId null")
    void testRealSchemaCompilerService() {
        SchemaCompilerService compiler = new SchemaCompilerService(
                formSchemaRepository,
                schemaVersionRepository,
                formSectionRepository,
                formTableRepository,
                formFieldRepository,
                universityRepository,
                objectMapper
        );

        FormSchema schema = FormSchema.builder()
                .id(5L)
                .name("Part A")
                .auditType("academic")
                .universityId(null)
                .build();

        SchemaVersion draftV1 = SchemaVersion.builder()
                .id(5L)
                .schemaId(5L)
                .versionNumber(1)
                .status("DRAFT")
                .build();

        when(schemaVersionRepository.findById(5L)).thenReturn(Optional.of(draftV1));
        when(formSchemaRepository.findById(5L)).thenReturn(Optional.of(schema));
        when(formSectionRepository.findByVersionIdOrderByDisplayOrderAscIdAsc(5L)).thenReturn(List.of());
        when(universityRepository.findByCodeIgnoreCase("dypiu")).thenReturn(Optional.empty());

        CompiledSchemaDto dto = compiler.compile(5L);
        assertNotNull(dto);
        assertEquals(5L, dto.getSchemaId());
        assertEquals(5L, dto.getVersionId());
        assertEquals("Part A", dto.getTitle());
        assertTrue(dto.getSections().isEmpty());
    }
}
