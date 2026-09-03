package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.model.config.University;
import com.director_appraisal.form_data_service.repository.config.UniversityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Form Config Service - UniversityService Tests")
class UniversityServiceTest {

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private DefaultSchemaTemplateService defaultSchemaTemplateService;

    private UniversityService universityService;

    @BeforeEach
    void setUp() {
        universityService = new UniversityService(universityRepository, defaultSchemaTemplateService);
    }

    @Test
    @DisplayName("Should create new university with valid unique code")
    void testCreateUniversity() {
        University uni = University.builder()
                .code("apex_uni")
                .name("Apex Global University")
                .domain("apex.edu.in")
                .build();

        when(universityRepository.existsByCodeIgnoreCase("apex_uni")).thenReturn(false);
        when(universityRepository.save(any())).thenAnswer(inv -> {
            University u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        University created = universityService.createUniversity(uni);
        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("apex_uni", created.getCode());
    }

    @Test
    @DisplayName("Should throw exception when attempting to create university with duplicate code")
    void testDuplicateUniversityCodeThrows() {
        University uni = University.builder()
                .code("dypiu")
                .name("DYPIU Duplicate")
                .build();

        when(universityRepository.existsByCodeIgnoreCase("dypiu")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> universityService.createUniversity(uni));
    }

    @Test
    @DisplayName("Should update existing university branding and address")
    void testUpdateUniversity() {
        University existing = University.builder()
                .id(1L)
                .code("dypiu")
                .name("Old Name")
                .address("Old Address")
                .build();

        when(universityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(universityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        University updateReq = University.builder()
                .name("D Y Patil International University Akurdi Pune")
                .address("Sector 29, Pradhikaran, Akurdi, Pune")
                .primaryColor("#1e3a8a")
                .build();

        University updated = universityService.updateUniversity(1L, updateReq);
        assertEquals("D Y Patil International University Akurdi Pune", updated.getName());
        assertEquals("Sector 29, Pradhikaran, Akurdi, Pune", updated.getAddress());
        assertEquals("#1e3a8a", updated.getPrimaryColor());
    }
}
