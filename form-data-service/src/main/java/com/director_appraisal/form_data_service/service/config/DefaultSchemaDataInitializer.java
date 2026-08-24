package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.dto.config.CompiledSchemaDto;
import com.director_appraisal.form_data_service.model.config.*;
import com.director_appraisal.form_data_service.repository.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSchemaDataInitializer implements CommandLineRunner {

    private final UniversityRepository universityRepository;
    private final FormSchemaRepository formSchemaRepository;
    private final SchemaVersionRepository schemaVersionRepository;
    private final FormSectionRepository formSectionRepository;
    private final FormTableRepository formTableRepository;
    private final FormFieldRepository formFieldRepository;
    private final SchemaCompilerService schemaCompilerService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            University dypiu = initDypiuUniversity();
            initAcademicSchema(dypiu);
            initAdministrativeSchema(dypiu);
        } catch (Exception e) {
            log.error("Failed to seed default schemas: {}", e.getMessage(), e);
        }
    }

    private University initDypiuUniversity() {
        Optional<University> existing = universityRepository.findByCodeIgnoreCase("dypiu");
        if (existing.isPresent()) {
            return existing.get();
        }

        University u = University.builder()
                .code("dypiu")
                .name("D Y Patil International University Akurdi Pune")
                .domain("dypiu.ac.in")
                .address("Sector 29, Pradhikaran, Akurdi, Pune - Maharashtra, INDIA 411044")
                .establishmentAct("Establishment by Maharashtra Act No. LXIII of 2017")
                .primaryColor("#1e3a8a")
                .status("ACTIVE")
                .build();
        University saved = universityRepository.save(u);
        log.info("Initialized default university: {}", saved.getName());
        return saved;
    }

    private void initAcademicSchema(University university) {
        Optional<FormSchema> existing = formSchemaRepository.findFirstByUniversityIdAndAuditTypeIgnoreCaseOrderByIdAsc(university.getId(), "academic");
        if (existing.isPresent() && existing.get().getActiveVersionId() != null) {
            return;
        }

        FormSchema schema = existing.orElseGet(() -> formSchemaRepository.save(
                FormSchema.builder()
                        .universityId(university.getId())
                        .auditType("academic")
                        .name("External Academic Audit")
                        .description("Comprehensive Annual Academic Audit for Schools & Departments")
                        .status("ACTIVE")
                        .build()
        ));

        List<SchemaVersion> versions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schema.getId());
        if (!versions.isEmpty() && schema.getActiveVersionId() != null) {
            return;
        }

        SchemaVersion v1 = SchemaVersion.builder()
                .schemaId(schema.getId())
                .versionNumber(1)
                .status("DRAFT")
                .academicYear("July, 2025 - June, 2026")
                .title("External Academic Audit")
                .ownerRole("director-schools")
                .publishedBy("system-init")
                .build();
        SchemaVersion savedV1 = schemaVersionRepository.save(v1);

        // Section 1: School / Department Information
        FormSection secInfo = createSection(savedV1.getId(), "school-department-information", "School / Department Information", "1", "director-schools", 1);
        createField(secInfo.getId(), null, "schoolName", "Name of the School / Department", "TEXT", true, 1);
        createField(secInfo.getId(), null, "establishmentYear", "Year of Establishment", "TEXT", false, 2);
        createField(secInfo.getId(), null, "address", "Address", "TEXTAREA", false, 3);
        createField(secInfo.getId(), null, "directorName", "Director's Name", "TEXT", false, 4);
        createField(secInfo.getId(), null, "directorEmail", "Director's Mail Id", "EMAIL", false, 5);
        createField(secInfo.getId(), null, "ugIntake", "UG Intake", "TEXT", false, 6);
        createField(secInfo.getId(), null, "pgIntake", "PG Intake", "TEXT", false, 7);
        createSelectField(secInfo.getId(), null, "academicCalendar", "Academic Calendar", List.of("Available", "Not Available"), 8);

        FormTable tblStudentStrength = createTable(secInfo.getId(), "studentStrength", "Student's Strength", 1, true, null);
        createColumn(secInfo.getId(), tblStudentStrength.getId(), "class", "Class", "TEXT", 1);
        createColumn(secInfo.getId(), tblStudentStrength.getId(), "sanctioned_intake", "Sanctioned Intake", "TEXT", 2);
        createColumn(secInfo.getId(), tblStudentStrength.getId(), "admitted_students", "No. of Students Admitted", "TEXT", 3);

        FormTable tblFacultyStrength = createTable(secInfo.getId(), "facultyStrength", "Faculty Strength", 2, true, null);
        createColumn(secInfo.getId(), tblFacultyStrength.getId(), "required", "Required", "TEXT", 1);
        createColumn(secInfo.getId(), tblFacultyStrength.getId(), "available", "Available", "TEXT", 2);

        // Section 2: Part A - Academic Activities
        FormSection secPartA = createSection(savedV1.getId(), "part-a-academic-activities", "Part A - Academic Activities", "A", "director-schools", 2);
        FormTable tblBos = createTable(secPartA.getId(), "boardOfStudies", "1. Board of Studies meetings conducted", 1, true, null);
        createColumn(secPartA.getId(), tblBos.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secPartA.getId(), tblBos.getId(), "date_of_meeting", "Date of the meeting", "DATE", 2);
        createColumn(secPartA.getId(), tblBos.getId(), "mom_link", "Link for MoM", "ATTACHMENT", 3);

        createField(secPartA.getId(), null, "stakeholderFeedback", "a. Stakeholder feedback", "TEXTAREA", false, 1);
        createField(secPartA.getId(), null, "feedbackAnalysis", "b. Analysis of the feedback", "TEXTAREA", false, 2);
        createField(secPartA.getId(), null, "actionTakenReport", "c. Action taken Report", "TEXTAREA", false, 3);

        FormTable tblSyllabus = createTable(secPartA.getId(), "syllabusRevision", "Syllabus revision feedback, analysis and ATR", 2, true, null);
        createColumn(secPartA.getId(), tblSyllabus.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secPartA.getId(), tblSyllabus.getId(), "category", "Category of Feedback", "TEXT", 2);
        createColumn(secPartA.getId(), tblSyllabus.getId(), "link", "Link for Analysis and ATR", "ATTACHMENT", 3);

        List<Map<String, Object>> obeRows = List.of(
                Map.of("Sr No", "1", "Particular", "Learning outcomes"),
                Map.of("Sr No", "2", "Particular", "Concurrent assessment"),
                Map.of("Sr No", "3", "Particular", "CO Coverage in Assessment"),
                Map.of("Sr No", "4", "Particular", "Course Exit Survey")
        );
        FormTable tblObe = createTable(secPartA.getId(), "obeImplementation", "3. Outcome based education implementation", 3, false, obeRows);
        createColumn(secPartA.getId(), tblObe.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secPartA.getId(), tblObe.getId(), "particular", "Particular", "TEXT", 2);
        createColumn(secPartA.getId(), tblObe.getId(), "document_link", "Link for the Document", "ATTACHMENT", 3);

        List<Map<String, Object>> nepRows = List.of(
                Map.of("SN", "1", "Check Points", "NEP Governance Structure"),
                Map.of("SN", "2", "Check Points", "Curriculum Alignment with NEP"),
                Map.of("SN", "3", "Check Points", "Multidisciplinary & Interdisciplinary Learning"),
                Map.of("SN", "4", "Check Points", "Academic Bank of Credits (ABC)"),
                Map.of("SN", "5", "Check Points", "Multiple Entry & Exit"),
                Map.of("SN", "6", "Check Points", "Skill & Vocational Education")
        );
        FormTable tblNep = createTable(secPartA.getId(), "nepStatus", "4. NEP 2020 implementation status", 4, false, nepRows);
        createColumn(secPartA.getId(), tblNep.getId(), "sn", "SN", "TEXT", 1);
        createColumn(secPartA.getId(), tblNep.getId(), "check_points", "Check Points", "TEXT", 2);
        createColumn(secPartA.getId(), tblNep.getId(), "availability", "Availability", "TEXT", 3);
        createColumn(secPartA.getId(), tblNep.getId(), "sdg_address", "SDG Address", "TEXTAREA", 4);
        createColumn(secPartA.getId(), tblNep.getId(), "document_link", "Link for the Document", "ATTACHMENT", 5);

        FormTable tblBestPractices = createTable(secPartA.getId(), "bestPractices", "5. Best Practices at School level", 5, true, null);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "title_of_practice", "Title of the Practice", "TEXT", 2);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "objectives", "Objectives of the practice", "TEXTAREA", 3);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "context", "The Context", "TEXTAREA", 4);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "practice", "The Practice", "TEXTAREA", 5);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "evidence_of_success", "Evidence of Success", "TEXTAREA", 6);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "problems_encountered", "Problems Encountered and Resources Required", "TEXTAREA", 7);
        createColumn(secPartA.getId(), tblBestPractices.getId(), "document_link", "Link for Document", "ATTACHMENT", 8);

        // Section 3: Part B - Student Development & Progression
        FormSection secPartB = createSection(savedV1.getId(), "part-b-student-progression", "Part B - Student Development & Progression", "B", "director-schools", 3);
        initPartBTables(secPartB);

        // Section 4: Part C - Faculty Development & Research Activities
        FormSection secPartC = createSection(savedV1.getId(), "part-c-faculty-research", "Part C - Faculty Development & Research Activities", "C", "director-schools", 4);
        initPartCTables(secPartC);

        // Section 5: Part D - SWOC Analysis
        FormSection secPartD = createSection(savedV1.getId(), "part-d-swoc-analysis", "Part D - SWOC Analysis", "D", "director-schools", 5);
        initPartDTables(secPartD);

        // Section 6: Part E - Observations & Recommendations
        FormSection secPartE = createSection(savedV1.getId(), "part-e-observations", "Part E - Observations & Recommendations of the Audit", "E", "director-schools", 6);
        createField(secPartE.getId(), null, "observations", "Observations of the Academic Audit Team", "TEXTAREA", false, 1);
        createField(secPartE.getId(), null, "recommendations", "Recommendations of the Audit Team", "TEXTAREA", false, 2);
        createField(secPartE.getId(), null, "signedReportUrl", "Upload Documentation (Signed Report PDF)", "ATTACHMENT", false, 3);

        // Publish and Compile V1
        CompiledSchemaDto compiled = schemaCompilerService.compile(savedV1.getId());
        try {
            savedV1.setCompiledSchema(objectMapper.writeValueAsString(compiled));
        } catch (Exception ignored) {}
        savedV1.setStatus("PUBLISHED");
        savedV1.setPublishedAt(LocalDateTime.now());
        schemaVersionRepository.save(savedV1);

        schema.setActiveVersionId(savedV1.getId());
        schema.setActiveVersionNumber(1);
        formSchemaRepository.save(schema);
        log.info("Initialized default Academic Form Schema Version 1 for DYPIU.");
    }

    private void initAdministrativeSchema(University university) {
        Optional<FormSchema> existing = formSchemaRepository.findFirstByUniversityIdAndAuditTypeIgnoreCaseOrderByIdAsc(university.getId(), "administrative");
        if (existing.isPresent() && existing.get().getActiveVersionId() != null) {
            return;
        }

        FormSchema schema = existing.orElseGet(() -> formSchemaRepository.save(
                FormSchema.builder()
                        .universityId(university.getId())
                        .auditType("administrative")
                        .name("Internal Administrative Audit")
                        .description("Collaborative Institutional Administrative Audit across Registrar, HR, DSW, and Placement")
                        .status("ACTIVE")
                        .build()
        ));

        List<SchemaVersion> versions = schemaVersionRepository.findBySchemaIdOrderByVersionNumberDesc(schema.getId());
        if (!versions.isEmpty() && schema.getActiveVersionId() != null) {
            return;
        }

        SchemaVersion v1 = SchemaVersion.builder()
                .schemaId(schema.getId())
                .versionNumber(1)
                .status("DRAFT")
                .academicYear("July, 2025 - June, 2026")
                .title("Internal Administrative Audit")
                .ownerRole("registrar")
                .publishedBy("system-init")
                .build();
        SchemaVersion savedV1 = schemaVersionRepository.save(v1);

        // Section A: University Information (Registrar)
        FormSection secA = createSection(savedV1.getId(), "section-a-university-information", "Section A - University Information", "A", "registrar", 1);
        createField(secA.getId(), null, "universityName", "Name of the University", "TEXT", false, 1);
        createField(secA.getId(), null, "establishmentYear", "Year of Establishment", "TEXT", false, 2);
        createField(secA.getId(), null, "address", "Address", "TEXTAREA", false, 3);
        createField(secA.getId(), null, "pinCode", "Pin Code", "TEXT", false, 4);
        createField(secA.getId(), null, "website", "Website", "TEXT", false, 5);
        createField(secA.getId(), null, "viceChancellor", "Name of the Vice Chancellor", "TEXT", false, 6);
        createField(secA.getId(), null, "vcEmail", "Vice Chancellor e-Mail Id", "EMAIL", false, 7);
        createField(secA.getId(), null, "registrar", "Name of Registrar", "TEXT", false, 8);
        createField(secA.getId(), null, "registrarEmail", "Registrar e-Mail Id", "EMAIL", false, 9);
        createField(secA.getId(), null, "accreditation", "Accreditation status & Agency", "TEXTAREA", false, 10);

        FormTable tblCourses = createTable(secA.getId(), "coursesOffered", "1. Courses Offered", 1, true, null);
        createColumn(secA.getId(), tblCourses.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secA.getId(), tblCourses.getId(), "program_name", "Name of the Program", "TEXT", 2);
        createColumn(secA.getId(), tblCourses.getId(), "level", "Level (UG/PG)", "TEXT", 3);
        createColumn(secA.getId(), tblCourses.getId(), "intake", "Intake", "TEXT", 4);
        createColumn(secA.getId(), tblCourses.getId(), "admitted", "No. of Students Admitted", "NUMBER", 5);
        createColumn(secA.getId(), tblCourses.getId(), "commencement_year", "Year of Commencement of the program", "TEXT", 6);
        createColumn(secA.getId(), tblCourses.getId(), "attachment", "Attachment (Attach List of the Students)", "ATTACHMENT", 7);

        List<Map<String, Object>> studentCatRows = List.of(
                Map.of("Sr No", "1", "Category", "SC"),
                Map.of("Sr No", "2", "Category", "ST"),
                Map.of("Sr No", "3", "Category", "OBC"),
                Map.of("Sr No", "4", "Category", "General")
        );
        FormTable tblStudentStats = createTable(secA.getId(), "studentStatistics", "2. Total Number of Students in the university", 2, false, studentCatRows);
        createColumn(secA.getId(), tblStudentStats.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secA.getId(), tblStudentStats.getId(), "category", "Category", "SELECT", 2);
        createColumn(secA.getId(), tblStudentStats.getId(), "ug", "U.G.", "TEXT", 3);
        createColumn(secA.getId(), tblStudentStats.getId(), "pg", "P.G.", "TEXT", 4);
        createColumn(secA.getId(), tblStudentStats.getId(), "phd", "Ph.D.", "TEXT", 5);
        createColumn(secA.getId(), tblStudentStats.getId(), "value_added", "Value added / skill Courses", "TEXT", 6);

        // Section B: Faculty and Staff Details (HR)
        FormSection secB = createSection(savedV1.getId(), "section-b-faculty-staff-details", "Section B - Faculty and Staff Details", "B", "hr", 2);
        FormTable tblFacultyInfo = createTable(secB.getId(), "facultyInformation", "1. Faculty Information", 1, true, null);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "school_name", "School / Department", "TEXT", 2);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "prof_req", "Prof. Req.", "NUMBER", 3);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "prof_avail", "Prof. Avail.", "NUMBER", 4);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "assoc_req", "Assoc. Prof. Req.", "NUMBER", 5);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "assoc_avail", "Assoc. Prof. Avail.", "NUMBER", 6);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "asst_req", "Asst. Prof. Req.", "NUMBER", 7);
        createColumn(secB.getId(), tblFacultyInfo.getId(), "asst_avail", "Asst. Prof. Avail.", "NUMBER", 8);

        FormTable tblStaff = createTable(secB.getId(), "supportingStaff", "4. Supporting Staff Details", 2, true, null);
        createColumn(secB.getId(), tblStaff.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secB.getId(), tblStaff.getId(), "staff_type", "Staff Type", "TEXT", 2);
        createColumn(secB.getId(), tblStaff.getId(), "sanctioned", "Sanctioned Posts", "NUMBER", 3);
        createColumn(secB.getId(), tblStaff.getId(), "filled", "Filled Posts", "NUMBER", 4);

        // Section C: Student Support and Welfare (DSW)
        FormSection secC = createSection(savedV1.getId(), "section-c-student-support-and-welfare", "Section C - Student Support and Welfare", "C", "dean-student-welfare", 3);
        FormTable tblSports = createTable(secC.getId(), "sportsFacilities", "1. Sports Facilities", 1, true, null);
        createColumn(secC.getId(), tblSports.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secC.getId(), tblSports.getId(), "facility", "Facility Name", "TEXT", 2);
        createColumn(secC.getId(), tblSports.getId(), "area", "Area / Capacity", "TEXT", 3);
        createColumn(secC.getId(), tblSports.getId(), "details", "Details / Remarks", "TEXTAREA", 4);

        // Section D: Training, Placement and Career Counseling (Dean Placement)
        FormSection secD = createSection(savedV1.getId(), "section-d-training-placement", "Section D - Training, Placement and Career Counseling", "D", "dean-placement", 4);
        FormTable tblTraining = createTable(secD.getId(), "trainingActivities", "1. Training Activities", 1, true, null);
        createColumn(secD.getId(), tblTraining.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secD.getId(), tblTraining.getId(), "program_title", "Program Title", "TEXT", 2);
        createColumn(secD.getId(), tblTraining.getId(), "trainer", "Trainer / Agency", "TEXT", 3);
        createColumn(secD.getId(), tblTraining.getId(), "beneficiaries", "No. of Beneficiaries", "NUMBER", 4);
        createColumn(secD.getId(), tblTraining.getId(), "proof", "Proof / Report", "ATTACHMENT", 5);

        // Section E: Central Infrastructure & Resources
        FormSection secE = createSection(savedV1.getId(), "section-e-infrastructure", "Section E - Central Infrastructure & Resources", "E", "registrar", 5);
        FormTable tblLibrary = createTable(secE.getId(), "libraryInfrastructure", "1. Library Infrastructure & Resources", 1, true, null);
        createColumn(secE.getId(), tblLibrary.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secE.getId(), tblLibrary.getId(), "resource_type", "Resource Type", "TEXT", 2);
        createColumn(secE.getId(), tblLibrary.getId(), "count_available", "Total Count", "TEXT", 3);
        createColumn(secE.getId(), tblLibrary.getId(), "annual_addition", "Added During Year", "TEXT", 4);

        // Publish and Compile V1
        CompiledSchemaDto compiled = schemaCompilerService.compile(savedV1.getId());
        try {
            savedV1.setCompiledSchema(objectMapper.writeValueAsString(compiled));
        } catch (Exception ignored) {}
        savedV1.setStatus("PUBLISHED");
        savedV1.setPublishedAt(LocalDateTime.now());
        schemaVersionRepository.save(savedV1);

        schema.setActiveVersionId(savedV1.getId());
        schema.setActiveVersionNumber(1);
        formSchemaRepository.save(schema);
        log.info("Initialized default Administrative Form Schema Version 1 for DYPIU.");
    }

    private void initPartBTables(FormSection sec) {
        FormTable tblMentoring = createTable(sec.getId(), "studentMentoring", "1. Student Mentoring System", 1, true, null);
        createColumn(sec.getId(), tblMentoring.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblMentoring.getId(), "mentor_name", "Name of the Mentor", "TEXT", 2);
        createColumn(sec.getId(), tblMentoring.getId(), "no_of_mentees", "No. of Mentees", "TEXT", 3);
        createColumn(sec.getId(), tblMentoring.getId(), "mom_link", "Link for MoM", "ATTACHMENT", 4);

        FormTable tblGraduating = createTable(sec.getId(), "graduatingStudents", "2. Total No of students graduating", 2, true, null);
        createColumn(sec.getId(), tblGraduating.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblGraduating.getId(), "program_name", "Name of the Program", "TEXT", 2);
        createColumn(sec.getId(), tblGraduating.getId(), "admitted_count", "Students Admitted in First Year", "TEXT", 3);
        createColumn(sec.getId(), tblGraduating.getId(), "graduated_count", "Students Graduated", "TEXT", 4);

        FormTable tblQualifying = createTable(sec.getId(), "qualifyingExams", "4. Students qualifying in state/national/international examinations", 4, true, null);
        createColumn(sec.getId(), tblQualifying.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblQualifying.getId(), "exam_name", "Exam Name (GATE/NET/CAT/GRE/etc.)", "TEXT", 2);
        createColumn(sec.getId(), tblQualifying.getId(), "student_name", "Name of the Student", "TEXT", 3);
        createColumn(sec.getId(), tblQualifying.getId(), "registration_no", "Registration / Roll No.", "TEXT", 4);
        createColumn(sec.getId(), tblQualifying.getId(), "proof_link", "Link for Document", "ATTACHMENT", 5);

        FormTable tblPlacements = createTable(sec.getId(), "studentPlacements", "6. Placement of outgoing students", 6, true, null);
        createColumn(sec.getId(), tblPlacements.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblPlacements.getId(), "student_name", "Name of Student Placed", "TEXT", 2);
        createColumn(sec.getId(), tblPlacements.getId(), "program_graduated", "Program Graduated From", "TEXT", 3);
        createColumn(sec.getId(), tblPlacements.getId(), "employer_name", "Name of the Employer", "TEXT", 4);
        createColumn(sec.getId(), tblPlacements.getId(), "package_lpa", "Package (in LPA)", "TEXT", 5);
        createColumn(sec.getId(), tblPlacements.getId(), "offer_letter_link", "Link for Offer Letter", "ATTACHMENT", 6);
    }

    private void initPartCTables(FormSection sec) {
        FormTable tblResearch = createTable(sec.getId(), "researchPublications", "2. Research publications in UGC CARE / Scopus / WoS journals", 1, true, null);
        createColumn(sec.getId(), tblResearch.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblResearch.getId(), "paper_title", "Title of paper", "TEXT", 2);
        createColumn(sec.getId(), tblResearch.getId(), "authors", "Name of the author/s", "TEXT", 3);
        createColumn(sec.getId(), tblResearch.getId(), "journal_name", "Name of journal", "TEXT", 4);
        createColumn(sec.getId(), tblResearch.getId(), "publication_year", "Year of publication", "TEXT", 5);
        createColumn(sec.getId(), tblResearch.getId(), "issn", "ISSN number", "TEXT", 6);
        createColumn(sec.getId(), tblResearch.getId(), "doi_link", "Link to website of journal / DOI", "URL", 7);
        createColumn(sec.getId(), tblResearch.getId(), "paper_link", "Link to paper / abstract", "ATTACHMENT", 8);

        FormTable tblBooks = createTable(sec.getId(), "booksChapters", "3. Books and chapters in edited volumes / books published", 2, true, null);
        createColumn(sec.getId(), tblBooks.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblBooks.getId(), "teacher_name", "Name of the teacher", "TEXT", 2);
        createColumn(sec.getId(), tblBooks.getId(), "book_title", "Title of the book/chapter published", "TEXT", 3);
        createColumn(sec.getId(), tblBooks.getId(), "paper_title", "Title of the paper", "TEXT", 4);
        createColumn(sec.getId(), tblBooks.getId(), "isbn", "ISBN of the proceeding", "TEXT", 5);
        createColumn(sec.getId(), tblBooks.getId(), "publisher_name", "Name of the publisher", "TEXT", 6);
        createColumn(sec.getId(), tblBooks.getId(), "doc_link", "Link of the document", "ATTACHMENT", 7);

        FormTable tblPatents = createTable(sec.getId(), "patentsCopyrights", "9. Patents / Copyrights published / awarded", 3, true, null);
        createColumn(sec.getId(), tblPatents.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblPatents.getId(), "teacher_name", "Name of the Teacher", "TEXT", 2);
        createColumn(sec.getId(), tblPatents.getId(), "patent_no", "Patent Number", "TEXT", 3);
        createColumn(sec.getId(), tblPatents.getId(), "patent_title", "Title of the patent", "TEXT", 4);
        createColumn(sec.getId(), tblPatents.getId(), "status", "Status (Published / Awarded)", "TEXT", 5);
        createColumn(sec.getId(), tblPatents.getId(), "award_year", "Year of Award", "TEXT", 6);
        createColumn(sec.getId(), tblPatents.getId(), "doc_link", "Link of the document", "ATTACHMENT", 7);
    }

    private void initPartDTables(FormSection sec) {
        FormTable tblStr = createTable(sec.getId(), "swocStrength", "1. Strengths", 1, true, null);
        createColumn(sec.getId(), tblStr.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblStr.getId(), "point", "Strength Details", "TEXTAREA", 2);

        FormTable tblWeak = createTable(sec.getId(), "swocWeaknesses", "2. Weaknesses", 2, true, null);
        createColumn(sec.getId(), tblWeak.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblWeak.getId(), "point", "Weakness Details", "TEXTAREA", 2);

        FormTable tblOpp = createTable(sec.getId(), "swocOpportunities", "3. Opportunities", 3, true, null);
        createColumn(sec.getId(), tblOpp.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblOpp.getId(), "point", "Opportunity Details", "TEXTAREA", 2);

        FormTable tblChal = createTable(sec.getId(), "swocChallenges", "4. Challenges", 4, true, null);
        createColumn(sec.getId(), tblChal.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(sec.getId(), tblChal.getId(), "point", "Challenge Details", "TEXTAREA", 2);
    }

    private FormSection createSection(Long versionId, String key, String title, String number, String role, int order) {
        return formSectionRepository.save(
                FormSection.builder()
                        .versionId(versionId)
                        .sectionKey(key)
                        .title(title)
                        .sectionNumber(number)
                        .ownerRole(role)
                        .displayOrder(order)
                        .build()
        );
    }

    private FormTable createTable(Long sectionId, String key, String title, int order, boolean repeatable, List<Map<String, Object>> initialRows) {
        String rowsJson = null;
        if (initialRows != null && !initialRows.isEmpty()) {
            try {
                rowsJson = objectMapper.writeValueAsString(initialRows);
            } catch (Exception ignored) {}
        }

        return formTableRepository.save(
                FormTable.builder()
                        .sectionId(sectionId)
                        .tableKey(key)
                        .title(title)
                        .isRepeatable(repeatable)
                        .displayOrder(order)
                        .initialRows(rowsJson)
                        .build()
        );
    }

    private void createField(Long sectionId, Long tableId, String key, String label, String type, boolean required, int order) {
        formFieldRepository.save(
                FormField.builder()
                        .sectionId(sectionId)
                        .tableId(tableId)
                        .fieldKey(key)
                        .label(label)
                        .fieldType(type)
                        .isRequired(required)
                        .displayOrder(order)
                        .build()
        );
    }

    private void createSelectField(Long sectionId, Long tableId, String key, String label, List<String> options, int order) {
        String optJson = null;
        try {
            optJson = objectMapper.writeValueAsString(options);
        } catch (Exception ignored) {}

        formFieldRepository.save(
                FormField.builder()
                        .sectionId(sectionId)
                        .tableId(tableId)
                        .fieldKey(key)
                        .label(label)
                        .fieldType("SELECT")
                        .options(optJson)
                        .isRequired(false)
                        .displayOrder(order)
                        .build()
        );
    }

    private void createColumn(Long sectionId, Long tableId, String key, String label, String type, int order) {
        formFieldRepository.save(
                FormField.builder()
                        .sectionId(sectionId)
                        .tableId(tableId)
                        .fieldKey(key)
                        .label(label)
                        .fieldType(type)
                        .isRequired(false)
                        .displayOrder(order)
                        .build()
        );
    }
}
