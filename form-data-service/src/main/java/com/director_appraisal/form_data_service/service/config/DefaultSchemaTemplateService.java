package com.director_appraisal.form_data_service.service.config;

import com.director_appraisal.form_data_service.dto.config.CompiledSchemaDto;
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
public class DefaultSchemaTemplateService {

    private final FormSchemaRepository formSchemaRepository;
    private final SchemaVersionRepository schemaVersionRepository;
    private final FormSectionRepository formSectionRepository;
    private final FormTableRepository formTableRepository;
    private final FormFieldRepository formFieldRepository;
    private final SchemaCompilerService schemaCompilerService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void seedDefaultTemplatesForUniversity(University university) {
        // No-op: Users create their own custom schemas dynamically in Appraisal Form Studio
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
                .publishedBy("system-provision")
                .build();
        SchemaVersion savedV1 = schemaVersionRepository.save(v1);

        // Section 1: School / Department Information
        FormSection secInfo = createSection(savedV1.getId(), "school-department-information", "School / Department Information", "1", "director-schools", 1);
        createField(secInfo.getId(), null, "schoolName", "Name of the School / Department", "TEXT", true, 1);
        createField(secInfo.getId(), null, "establishmentYear", "Year of Establishment", "TEXT", false, 2);
        createField(secInfo.getId(), null, "address", "Address", "TEXTAREA", false, 3);
        createField(secInfo.getId(), null, "directorName", "Director's Name", "TEXT", false, 4);

        // Part A: Curriculum Aspects
        FormSection secA = createSection(savedV1.getId(), "part-a-curriculum-aspects", "Part A - Curriculum Aspects", "A", "director-schools", 2);
        FormTable tblCourses = createTable(secA.getId(), "coursesOffered", "1. Courses Offered", 1, true, null);
        createColumn(secA.getId(), tblCourses.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secA.getId(), tblCourses.getId(), "program_name", "Name of the Program", "TEXT", 2);
        createColumn(secA.getId(), tblCourses.getId(), "level", "Level (UG/PG)", "TEXT", 3);
        createColumn(secA.getId(), tblCourses.getId(), "intake", "Intake", "TEXT", 4);
        createColumn(secA.getId(), tblCourses.getId(), "admitted", "No. of Students Admitted", "NUMBER", 5);
        createColumn(secA.getId(), tblCourses.getId(), "commencement_year", "Year of Commencement", "TEXT", 6);
        createColumn(secA.getId(), tblCourses.getId(), "attachment", "Attachment (Attach List of the Students)", "ATTACHMENT", 7);

        FormTable tblRevision = createTable(secA.getId(), "syllabusRevision", "2. Revision of Syllabus (UG/PG)", 2, true, null);
        createColumn(secA.getId(), tblRevision.getId(), "program_name", "Program Name", "TEXT", 1);
        createColumn(secA.getId(), tblRevision.getId(), "revision_year", "Year of Revision", "TEXT", 2);
        createColumn(secA.getId(), tblRevision.getId(), "content_added", "Content Added", "TEXTAREA", 3);
        createColumn(secA.getId(), tblRevision.getId(), "percentage_change", "% of Content Changed", "NUMBER", 4);
        createColumn(secA.getId(), tblRevision.getId(), "attachment", "Attach Course Structure & Syllabus", "ATTACHMENT", 5);

        FormTable tblBos = createTable(secA.getId(), "boardOfStudies", "3. Board of Studies (BOS) Details", 3, true, null);
        createColumn(secA.getId(), tblBos.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secA.getId(), tblBos.getId(), "meeting_date", "Date of BOS Meeting", "TEXT", 2);
        createColumn(secA.getId(), tblBos.getId(), "total_members", "Total Members Present", "NUMBER", 3);
        createColumn(secA.getId(), tblBos.getId(), "attachment", "Attach BOS Minutes", "ATTACHMENT", 4);

        FormTable tblNep = createTable(secA.getId(), "nepStatus", "4. Implementation of NEP 2020", 4, false, null);
        createColumn(secA.getId(), tblNep.getId(), "indicator", "NEP 2020 Dimension", "TEXT", 1);
        createColumn(secA.getId(), tblNep.getId(), "status", "Status / Compliance Level", "TEXT", 2);
        createColumn(secA.getId(), tblNep.getId(), "remarks", "Remarks & Evidence", "TEXTAREA", 3);

        FormTable tblObe = createTable(secA.getId(), "obeImplementation", "5. Outcome Based Education (OBE) Status", 5, false, null);
        createColumn(secA.getId(), tblObe.getId(), "program", "Program", "TEXT", 1);
        createColumn(secA.getId(), tblObe.getId(), "pos_defined", "POs & PSOs Defined", "SELECT", 2);
        createColumn(secA.getId(), tblObe.getId(), "co_po_mapping", "CO-PO Mapping Complete", "SELECT", 3);
        createColumn(secA.getId(), tblObe.getId(), "attainment_calculated", "Attainment Calculated", "SELECT", 4);

        FormTable tblValueAdded = createTable(secA.getId(), "valueAddedCourses", "6. Value Added / Skill Development Courses", 6, true, null);
        createColumn(secA.getId(), tblValueAdded.getId(), "course_name", "Course Name", "TEXT", 1);
        createColumn(secA.getId(), tblValueAdded.getId(), "contact_hours", "Contact Hours", "NUMBER", 2);
        createColumn(secA.getId(), tblValueAdded.getId(), "enrolled", "Enrolled Students", "NUMBER", 3);
        createColumn(secA.getId(), tblValueAdded.getId(), "completed", "Students Completed", "NUMBER", 4);

        // Part B: Teaching, Learning and Evaluation
        FormSection secB = createSection(savedV1.getId(), "part-b-teaching-learning-evaluation", "Part B - Teaching, Learning & Evaluation", "B", "director-schools", 3);
        FormTable tblFacultyStrength = createTable(secB.getId(), "facultyStrength", "1. Faculty Strength", 1, true, null);
        createColumn(secB.getId(), tblFacultyStrength.getId(), "designation", "Designation", "TEXT", 1);
        createColumn(secB.getId(), tblFacultyStrength.getId(), "sanctioned", "Sanctioned Posts", "NUMBER", 2);
        createColumn(secB.getId(), tblFacultyStrength.getId(), "filled", "Filled Posts", "NUMBER", 3);
        createColumn(secB.getId(), tblFacultyStrength.getId(), "phd_count", "Faculty with Ph.D.", "NUMBER", 4);

        FormTable tblFacultySpecialization = createTable(secB.getId(), "facultySpecialization", "2. Faculty Details with Specialization", 2, true, null);
        createColumn(secB.getId(), tblFacultySpecialization.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secB.getId(), tblFacultySpecialization.getId(), "faculty_name", "Name of Faculty", "TEXT", 2);
        createColumn(secB.getId(), tblFacultySpecialization.getId(), "designation", "Designation", "TEXT", 3);
        createColumn(secB.getId(), tblFacultySpecialization.getId(), "qualification", "Highest Qualification", "TEXT", 4);
        createColumn(secB.getId(), tblFacultySpecialization.getId(), "specialization", "Specialization", "TEXT", 5);
        createColumn(secB.getId(), tblFacultySpecialization.getId(), "experience_years", "Total Experience (Years)", "NUMBER", 6);

        FormTable tblStudentStrength = createTable(secB.getId(), "studentStrength", "3. Student Strength (Category-wise)", 3, false, null);
        createColumn(secB.getId(), tblStudentStrength.getId(), "program", "Program", "TEXT", 1);
        createColumn(secB.getId(), tblStudentStrength.getId(), "total_male", "Male", "NUMBER", 2);
        createColumn(secB.getId(), tblStudentStrength.getId(), "total_female", "Female", "NUMBER", 3);
        createColumn(secB.getId(), tblStudentStrength.getId(), "total_students", "Total", "NUMBER", 4);

        FormTable tblGraduating = createTable(secB.getId(), "graduatingStudents", "4. Graduating Students & Success Rate", 4, true, null);
        createColumn(secB.getId(), tblGraduating.getId(), "program", "Program Name", "TEXT", 1);
        createColumn(secB.getId(), tblGraduating.getId(), "final_year_appeared", "Students Appeared", "NUMBER", 2);
        createColumn(secB.getId(), tblGraduating.getId(), "final_year_passed", "Students Passed", "NUMBER", 3);
        createColumn(secB.getId(), tblGraduating.getId(), "pass_percentage", "Pass Percentage (%)", "NUMBER", 4);

        FormTable tblMentoring = createTable(secB.getId(), "studentMentoring", "5. Student Mentoring System", 5, false, null);
        createColumn(secB.getId(), tblMentoring.getId(), "total_students", "Total Students", "NUMBER", 1);
        createColumn(secB.getId(), tblMentoring.getId(), "total_mentors", "Total Mentors Assigned", "NUMBER", 2);
        createColumn(secB.getId(), tblMentoring.getId(), "mentor_ratio", "Mentor : Student Ratio", "TEXT", 3);
        createColumn(secB.getId(), tblMentoring.getId(), "meetings_conducted", "Average Meetings Conducted", "NUMBER", 4);

        // Part C: Research, Innovations and Extension
        FormSection secC = createSection(savedV1.getId(), "part-c-research-innovations-extension", "Part C - Research, Innovations & Extension", "C", "director-schools", 4);
        FormTable tblResearchPub = createTable(secC.getId(), "researchPublications", "1. Research Publications (Journals)", 1, true, null);
        createColumn(secC.getId(), tblResearchPub.getId(), "title", "Paper Title", "TEXT", 1);
        createColumn(secC.getId(), tblResearchPub.getId(), "authors", "Authors", "TEXT", 2);
        createColumn(secC.getId(), tblResearchPub.getId(), "journal_name", "Journal Name", "TEXT", 3);
        createColumn(secC.getId(), tblResearchPub.getId(), "indexing", "Indexing (Scopus / WoS / UGC-CARE)", "SELECT", 4);
        createColumn(secC.getId(), tblResearchPub.getId(), "doi", "DOI / Link", "TEXT", 5);
        createColumn(secC.getId(), tblResearchPub.getId(), "attachment", "Attach First Page", "ATTACHMENT", 6);

        FormTable tblBooks = createTable(secC.getId(), "booksChapters", "2. Books & Book Chapters Published", 2, true, null);
        createColumn(secC.getId(), tblBooks.getId(), "book_title", "Book / Chapter Title", "TEXT", 1);
        createColumn(secC.getId(), tblBooks.getId(), "author_name", "Author(s)", "TEXT", 2);
        createColumn(secC.getId(), tblBooks.getId(), "publisher", "Publisher", "TEXT", 3);
        createColumn(secC.getId(), tblBooks.getId(), "isbn", "ISBN Number", "TEXT", 4);

        FormTable tblPatents = createTable(secC.getId(), "patentsCopyrights", "3. Patents & Copyrights", 3, true, null);
        createColumn(secC.getId(), tblPatents.getId(), "patent_title", "Title", "TEXT", 1);
        createColumn(secC.getId(), tblPatents.getId(), "inventors", "Inventors", "TEXT", 2);
        createColumn(secC.getId(), tblPatents.getId(), "status", "Status (Filed / Published / Granted)", "SELECT", 3);
        createColumn(secC.getId(), tblPatents.getId(), "patent_number", "Application / Grant Number", "TEXT", 4);

        FormTable tblResearchFunds = createTable(secC.getId(), "researchFunds", "4. Research Grants & Funded Projects", 4, true, null);
        createColumn(secC.getId(), tblResearchFunds.getId(), "project_title", "Project Title", "TEXT", 1);
        createColumn(secC.getId(), tblResearchFunds.getId(), "pi_name", "Principal Investigator", "TEXT", 2);
        createColumn(secC.getId(), tblResearchFunds.getId(), "funding_agency", "Funding Agency", "TEXT", 3);
        createColumn(secC.getId(), tblResearchFunds.getId(), "amount_in_lakhs", "Amount (in Lakhs)", "NUMBER", 4);

        FormTable tblConsultancy = createTable(secC.getId(), "consultancy", "5. Consultancy & Corporate Training", 5, true, null);
        createColumn(secC.getId(), tblConsultancy.getId(), "client_name", "Organization / Client Name", "TEXT", 1);
        createColumn(secC.getId(), tblConsultancy.getId(), "project_name", "Project Title", "TEXT", 2);
        createColumn(secC.getId(), tblConsultancy.getId(), "revenue_generated", "Revenue Generated (INR)", "NUMBER", 3);

        FormTable tblMous = createTable(secC.getId(), "functionalMous", "6. Functional MOUs with Industry/Institutions", 6, true, null);
        createColumn(secC.getId(), tblMous.getId(), "partner_org", "Partner Institution / Industry", "TEXT", 1);
        createColumn(secC.getId(), tblMous.getId(), "signing_date", "Date of Signing", "TEXT", 2);
        createColumn(secC.getId(), tblMous.getId(), "activities_conducted", "List of Activities Conducted", "TEXTAREA", 3);

        // Part D: Student Progression and Support
        FormSection secD = createSection(savedV1.getId(), "part-d-student-progression-support", "Part D - Student Progression & Support", "D", "director-schools", 5);
        FormTable tblPlacements = createTable(secD.getId(), "studentPlacements", "1. Student Placement Details", 1, true, null);
        createColumn(secD.getId(), tblPlacements.getId(), "company_name", "Name of Employer", "TEXT", 1);
        createColumn(secD.getId(), tblPlacements.getId(), "package_lpa", "CTC Package (LPA)", "NUMBER", 2);
        createColumn(secD.getId(), tblPlacements.getId(), "placed_count", "Number of Students Placed", "NUMBER", 3);

        FormTable tblHigherStudies = createTable(secD.getId(), "higherStudies", "2. Progression to Higher Studies", 2, true, null);
        createColumn(secD.getId(), tblHigherStudies.getId(), "student_name", "Student Name", "TEXT", 1);
        createColumn(secD.getId(), tblHigherStudies.getId(), "institution_joined", "Institution Joined", "TEXT", 2);
        createColumn(secD.getId(), tblHigherStudies.getId(), "program_admitted", "Program Admitted", "TEXT", 3);

        FormTable tblStudentAwards = createTable(secD.getId(), "studentAwards", "3. Student Awards & Achievements", 3, true, null);
        createColumn(secD.getId(), tblStudentAwards.getId(), "student_name", "Student Name", "TEXT", 1);
        createColumn(secD.getId(), tblStudentAwards.getId(), "event_name", "Event / Competition", "TEXT", 2);
        createColumn(secD.getId(), tblStudentAwards.getId(), "level", "Level (National / International)", "SELECT", 3);
        createColumn(secD.getId(), tblStudentAwards.getId(), "award_position", "Position / Award Won", "TEXT", 4);

        // Part E: Institutional Strengths & SWOC
        FormSection secE = createSection(savedV1.getId(), "part-e-institutional-strengths-swoc", "Part E - Institutional Strengths & SWOC", "E", "director-schools", 6);
        createField(secE.getId(), null, "swocStrength", "1. Key Strengths of the School / Department", "TEXTAREA", true, 1);
        createField(secE.getId(), null, "swocWeaknesses", "2. Key Weaknesses & Areas for Improvement", "TEXTAREA", true, 2);
        createField(secE.getId(), null, "swocOpportunities", "3. Opportunities for Growth", "TEXTAREA", true, 3);
        createField(secE.getId(), null, "swocChallenges", "4. Challenges Faced", "TEXTAREA", true, 4);
        createField(secE.getId(), null, "bestPractices", "5. Best Practices Implemented", "TEXTAREA", true, 5);

        // Auto-compile snapshot and publish V1
        publishAndCompileVersion(savedV1, schema);
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
                .publishedBy("system-provision")
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

        // Section C: Infrastructure and Learning Resources (Estate)
        FormSection secC = createSection(savedV1.getId(), "section-c-infrastructure-resources", "Section C - Infrastructure and Learning Resources", "C", "estate-officer", 3);
        FormTable tblBuilding = createTable(secC.getId(), "buildingInfrastructure", "1. Building Infrastructure", 1, false, List.of(
                Map.of("Sr No", "1", "Particulars", "Academic Area (sq.m.)"),
                Map.of("Sr No", "2", "Particulars", "Administrative Area (sq.m.)"),
                Map.of("Sr No", "3", "Particulars", "Amenities Area (sq.m.)")
        ));
        createColumn(secC.getId(), tblBuilding.getId(), "sr_no", "Sr No", "TEXT", 1);
        createColumn(secC.getId(), tblBuilding.getId(), "particulars", "Particulars", "TEXT", 2);
        createColumn(secC.getId(), tblBuilding.getId(), "available_area", "Available Area", "TEXT", 3);
        createColumn(secC.getId(), tblBuilding.getId(), "remarks", "Remarks", "TEXTAREA", 4);

        FormTable tblIt = createTable(secC.getId(), "itInfrastructure", "2. IT Infrastructure", 2, true, null);
        createColumn(secC.getId(), tblIt.getId(), "item_name", "IT Resource", "TEXT", 1);
        createColumn(secC.getId(), tblIt.getId(), "quantity", "Quantity / Count", "NUMBER", 2);
        createColumn(secC.getId(), tblIt.getId(), "bandwidth", "Internet Bandwidth (Mbps)", "TEXT", 3);

        // Section D: Student Support and Activities (DSW)
        FormSection secD = createSection(savedV1.getId(), "section-d-student-support-activities", "Section D - Student Support and Activities", "D", "dsw", 4);
        FormTable tblScholarship = createTable(secD.getId(), "scholarshipStudents", "1. Scholarship & Financial Support", 1, true, null);
        createColumn(secD.getId(), tblScholarship.getId(), "scheme_name", "Name of Scheme / Agency", "TEXT", 1);
        createColumn(secD.getId(), tblScholarship.getId(), "beneficiary_count", "Number of Beneficiaries", "NUMBER", 2);
        createColumn(secD.getId(), tblScholarship.getId(), "amount_disbursed", "Amount Disbursed (INR)", "NUMBER", 3);

        FormTable tblSports = createTable(secD.getId(), "sportsActivities", "2. Sports & Cultural Events Conducted", 2, true, null);
        createColumn(secD.getId(), tblSports.getId(), "event_name", "Event Name", "TEXT", 1);
        createColumn(secD.getId(), tblSports.getId(), "date_conducted", "Date Conducted", "TEXT", 2);
        createColumn(secD.getId(), tblSports.getId(), "participants", "Total Participants", "NUMBER", 3);

        // Auto-compile snapshot and publish V1
        publishAndCompileVersion(savedV1, schema);
    }

    private void publishAndCompileVersion(SchemaVersion v1, FormSchema schema) {
        CompiledSchemaDto compiled = schemaCompilerService.compile(v1.getId());
        try {
            v1.setCompiledSchema(objectMapper.writeValueAsString(compiled));
            v1.setStatus("PUBLISHED");
            v1.setPublishedAt(LocalDateTime.now());
            schemaVersionRepository.save(v1);

            schema.setActiveVersionId(v1.getId());
            formSchemaRepository.save(schema);
        } catch (Exception e) {
            log.error("Failed to compile starter snapshot for version {}: {}", v1.getId(), e.getMessage());
        }
    }

    private FormSection createSection(Long versionId, String sectionKey, String title, String code, String role, int order) {
        return formSectionRepository.save(
                FormSection.builder()
                        .versionId(versionId)
                        .sectionKey(sectionKey)
                        .title(title)
                        .sectionNumber(code)
                        .ownerRole(role)
                        .displayOrder(order)
                        .build()
        );
    }

    private FormTable createTable(Long sectionId, String tableKey, String title, int order, boolean dynamicRows, List<Map<String, Object>> defaultRows) {
        String defaultRowsJson = null;
        if (defaultRows != null && !defaultRows.isEmpty()) {
            try {
                defaultRowsJson = objectMapper.writeValueAsString(defaultRows);
            } catch (Exception e) {
                log.warn("Failed to serialize default rows for table {}: {}", tableKey, e.getMessage());
            }
        }
        return formTableRepository.save(
                FormTable.builder()
                        .sectionId(sectionId)
                        .tableKey(tableKey)
                        .title(title)
                        .displayOrder(order)
                        .isRepeatable(dynamicRows)
                        .initialRows(defaultRowsJson)
                        .build()
        );
    }

    private void createField(Long sectionId, Long tableId, String fieldKey, String label, String type, boolean req, int order) {
        formFieldRepository.save(
                FormField.builder()
                        .sectionId(sectionId)
                        .tableId(tableId)
                        .fieldKey(fieldKey)
                        .label(label)
                        .fieldType(type)
                        .isRequired(req)
                        .displayOrder(order)
                        .build()
        );
    }

    private void createColumn(Long sectionId, Long tableId, String columnKey, String header, String type, int order) {
        createField(sectionId, tableId, columnKey, header, type, false, order);
    }
}
