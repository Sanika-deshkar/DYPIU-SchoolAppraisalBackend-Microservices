package com.director_appraisal.form_data_service.controller.config;

import com.director_appraisal.form_data_service.dto.config.CompiledSchemaDto;
import com.director_appraisal.form_data_service.model.config.University;
import com.director_appraisal.form_data_service.service.config.FormConfigService;
import com.director_appraisal.form_data_service.service.config.UniversityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Form Config Service - ClientConfigController Tests")
class ClientConfigControllerTest {

    @Mock
    private FormConfigService formConfigService;
    @Mock
    private UniversityService universityService;

    private ClientConfigController clientConfigController;

    @BeforeEach
    void setUp() {
        clientConfigController = new ClientConfigController(formConfigService, universityService);
    }

    @Test
    @DisplayName("Should return active compiled schema for given audit type and university code")
    void testGetActiveSchema() {
        CompiledSchemaDto dto = CompiledSchemaDto.builder()
                .schemaId(1L)
                .versionId(10L)
                .versionNumber(1)
                .auditType("academic")
                .title("External Academic Audit")
                .build();

        when(formConfigService.getActiveCompiledSchema("dypiu", "academic")).thenReturn(dto);

        ResponseEntity<CompiledSchemaDto> response = clientConfigController.getActiveSchema("academic", "dypiu", null, null, null);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("External Academic Audit", response.getBody().getTitle());
        assertEquals(1, response.getBody().getVersionNumber());
    }

    @Test
    @DisplayName("Should return university branding info")
    void testGetBranding() {
        University u = University.builder()
                .id(1L)
                .code("dypiu")
                .name("D Y Patil International University Akurdi Pune")
                .domain("dypiu.ac.in")
                .primaryColor("#1e3a8a")
                .build();

        when(universityService.getByCode("dypiu")).thenReturn(Optional.of(u));

        ResponseEntity<Map<String, Object>> response = clientConfigController.getBranding("dypiu", null, null, null);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("D Y Patil International University Akurdi Pune", response.getBody().get("universityName"));
        assertEquals("#1e3a8a", response.getBody().get("primaryColor"));
    }
}
