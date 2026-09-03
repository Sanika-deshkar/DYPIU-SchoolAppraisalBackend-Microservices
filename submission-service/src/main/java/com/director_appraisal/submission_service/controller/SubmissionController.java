package com.director_appraisal.submission_service.controller;

import com.director_appraisal.submission_service.model.Submission;
import com.director_appraisal.submission_service.dto.UserDto;
import com.director_appraisal.submission_service.client.AuthUserClient;
import com.director_appraisal.submission_service.service.SubmissionService;

import com.director_appraisal.submission_service.util.SchoolUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;



import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@CrossOrigin
public class SubmissionController {


    private final SubmissionService submissionService;
    private final AuthUserClient authUserClient;
    private final jakarta.servlet.http.HttpServletRequest httpRequest;

    private String getCurrentUserEmail() {
        if (httpRequest != null) {
            String headerEmail = httpRequest.getHeader("X-User-Email");
            if (headerEmail != null && !headerEmail.isBlank()) {
                return headerEmail.trim().toLowerCase();
            }
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7).trim();
                    int firstDot = token.indexOf('.');
                    int secondDot = token.indexOf('.', firstDot + 1);
                    if (firstDot > 0 && secondDot > firstDot) {
                        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.substring(firstDot + 1, secondDot)), java.nio.charset.StandardCharsets.UTF_8);
                        com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                        if (jsonNode.has("sub")) {
                            return jsonNode.get("sub").asText().trim().toLowerCase();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return "iqac@dypiu.ac.in";
    }

    private UserDto safeGetUserByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        try {
            return authUserClient.getUserByEmail(email.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private UserDto getCurrentUserDetails() {
        String email = getCurrentUserEmail();
        String roleFromContext = null;
        String schoolFromContext = null;
        String nameFromContext = null;
        String postFromContext = null;
        String categoryFromContext = null;
        Long universityIdFromContext = null;
        String universityCodeFromContext = null;

        if (httpRequest != null) {
            String headerRole = httpRequest.getHeader("X-User-Role");
            if (headerRole != null && !headerRole.isBlank()) roleFromContext = headerRole.trim();
            String headerSchool = httpRequest.getHeader("X-User-School");
            if (headerSchool != null && !headerSchool.isBlank()) schoolFromContext = headerSchool.trim();
            String headerName = httpRequest.getHeader("X-User-Name");
            if (headerName != null && !headerName.isBlank()) nameFromContext = headerName.trim();
            String headerUniId = httpRequest.getHeader("X-University-Id");
            if (headerUniId != null && !headerUniId.isBlank()) {
                try {
                    universityIdFromContext = Long.parseLong(headerUniId.trim());
                } catch (Exception ignored) {}
            }
            String headerUniCode = httpRequest.getHeader("X-University-Code");
            if (headerUniCode != null && !headerUniCode.isBlank()) {
                universityCodeFromContext = headerUniCode.trim();
            }

            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7).trim();
                    int firstDot = token.indexOf('.');
                    int secondDot = token.indexOf('.', firstDot + 1);
                    if (firstDot > 0 && secondDot > firstDot) {
                        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.substring(firstDot + 1, secondDot)), java.nio.charset.StandardCharsets.UTF_8);
                        com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                        if (jsonNode.has("role") && (roleFromContext == null || roleFromContext.isBlank())) {
                            roleFromContext = jsonNode.get("role").asText();
                        }
                        if (jsonNode.has("school") && (schoolFromContext == null || schoolFromContext.isBlank())) {
                            schoolFromContext = jsonNode.get("school").asText();
                        }
                        if (jsonNode.has("name") && (nameFromContext == null || nameFromContext.isBlank())) {
                            nameFromContext = jsonNode.get("name").asText();
                        }
                        if (jsonNode.has("post") && (postFromContext == null || postFromContext.isBlank())) {
                            postFromContext = jsonNode.get("post").asText();
                        }
                        if (jsonNode.has("category") && (categoryFromContext == null || categoryFromContext.isBlank())) {
                            categoryFromContext = jsonNode.get("category").asText();
                        }
                        if (jsonNode.has("universityId") && universityIdFromContext == null) {
                            try {
                                universityIdFromContext = jsonNode.get("universityId").asLong();
                            } catch (Exception ignored) {}
                        }
                        if (jsonNode.has("universityCode") && (universityCodeFromContext == null || universityCodeFromContext.isBlank())) {
                            universityCodeFromContext = jsonNode.get("universityCode").asText();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (email != null && !email.isBlank()) {
            UserDto u = safeGetUserByEmail(email);
            if (u != null) {
                if ((u.getRole() == null || u.getRole().isBlank()) && roleFromContext != null) {
                    u.setRole(roleFromContext);
                }
                if ((u.getSchool() == null || u.getSchool().isBlank()) && schoolFromContext != null) {
                    u.setSchool(schoolFromContext);
                }
                if ((u.getName() == null || u.getName().isBlank()) && nameFromContext != null) {
                    u.setName(nameFromContext);
                }
                if ((u.getPost() == null || u.getPost().isBlank()) && postFromContext != null) {
                    u.setPost(postFromContext);
                }
                if ((u.getCategory() == null || u.getCategory().isBlank()) && categoryFromContext != null) {
                    u.setCategory(categoryFromContext);
                }
                if (universityIdFromContext != null) {
                    u.setUniversityId(universityIdFromContext);
                }
                if (universityCodeFromContext != null && !universityCodeFromContext.isBlank()) {
                    u.setUniversityCode(universityCodeFromContext);
                }
                return u;
            }
        }

        return UserDto.builder()
                .email(email)
                .name(nameFromContext != null ? nameFromContext : "User")
                .role(roleFromContext != null ? roleFromContext : "director")
                .school(schoolFromContext)
                .post(postFromContext)
                .category(categoryFromContext)
                .universityId(universityIdFromContext != null ? universityIdFromContext : 1L)
                .universityCode(universityCodeFromContext != null && !universityCodeFromContext.isBlank() ? universityCodeFromContext : "dypiu")
                .build();
    }




    @GetMapping("/my-draft")
    public ResponseEntity<?> getMyDraft(
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String auditCycle,
            @RequestParam(required = false) String cycleId,
            @RequestParam(required = false, defaultValue = "false") boolean includeSubmitted,
            @RequestParam(required = false, defaultValue = "false") boolean includeApproved,
            @RequestParam(required = false, defaultValue = "false") boolean includeHistorical,
            @RequestParam(required = false, defaultValue = "false") boolean shared) {
        try {
            UserDto user = getCurrentUserDetails();
            String normalizedAuditType = resolveAuditTypeForCaller(user, auditType);
            String requestedYear = firstNonBlank(academicYear, auditCycle, cycleId);

            boolean includeNonDrafts = includeSubmitted || includeApproved || includeHistorical;

            log.info("getMyDraft: email={}, role={}, post={}, auditType={}, requestedYear={}, shared={}",
                    user.getEmail(), user.getRole(), user.getPost(), normalizedAuditType, requestedYear, shared);

            Submission draft = submissionService.getDraftForUser(
                    user,
                    normalizedAuditType,
                    requestedYear,
                    includeNonDrafts,
                    shared
            );
            if (draft != null) {
                submissionService.populatePermissions(draft, user);
            }
            return ResponseEntity.ok(draft);
        } catch (Exception e) {
            log.error("Error in getMyDraft: {}", e.getMessage(), e);
            throw e;
        }
    }


    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank() && !"null".equalsIgnoreCase(v.trim()) && !"undefined".equalsIgnoreCase(v.trim())) {
                return v.trim();
            }
        }
        return null;
    }

    @GetMapping("/administrative/{cycleId}/status")
    public ResponseEntity<Object> getAdministrativeStatus(@PathVariable String cycleId) {
        Submission submission = submissionService.getOrCreateSharedAdministrativeDraftForCycle(cycleId);
        return ResponseEntity.ok(submission.getSubmittedByForJson());
    }

    @PostMapping("/administrative/{cycleId}/submit")
    public ResponseEntity<Submission> submitAdministrativePart(@PathVariable String cycleId) {
        UserDto caller = getCurrentUserDetails();
        Submission submitted = submissionService.submitAdministrativePart(cycleId, caller);
        return ResponseEntity.ok(submitted);
    }

    private String resolveAuditTypeForCaller(UserDto user, String requestAuditType) {
        if (requestAuditType != null && !requestAuditType.isBlank() && !"null".equalsIgnoreCase(requestAuditType.trim()) && !"undefined".equalsIgnoreCase(requestAuditType.trim())) {
            return requestAuditType.trim().toLowerCase();
        }
        if (user != null) {
            String role = user.getRole() != null ? user.getRole().trim().toLowerCase() : "";
            if ("director".equals(role)) {
                return "academic";
            }
            if ("administrative".equals(role)) {
                return "administrative";
            }
            if (user.getCategory() != null && !user.getCategory().isBlank()) {
                return user.getCategory().trim().toLowerCase();
            }
        }
        return "academic";
    }

    private void validateAuditTypeForRole(String role, String auditType) {
        if (role == null || auditType == null) {
            return;
        }
        String roleLower = role.trim().toLowerCase();
        String typeLower = auditType.trim().toLowerCase();
        
        if (roleLower.contains("auditor")) {
            if (roleLower.contains("academic") && !"academic".equals(typeLower)) {
                throw new IllegalArgumentException("Academic auditors can only audit academic forms");
            }
            if (roleLower.contains("administrative") && !"administrative".equals(typeLower)) {
                throw new IllegalArgumentException("Administrative auditors can only audit administrative forms");
            }
            return;
        }
        
        if ("director".equals(roleLower) && !"academic".equals(typeLower)) {
            throw new IllegalArgumentException("Academic Directors can only submit academic audits");
        }
        if ("administrative".equals(roleLower) && !"administrative".equals(typeLower)) {
            throw new IllegalArgumentException("Administrative users can only submit administrative audits");
        }
        if (List.of("vice-chancellor", "iqac").contains(roleLower)) {
            throw new IllegalArgumentException("Reviewers (VC & IQAC) cannot create or submit audits");
        }
    }

    @PostMapping("/save-draft")
    public ResponseEntity<Submission> saveDraft(@RequestBody(required = false) FormSubmissionRequest request) {
        String email = getCurrentUserEmail();
        UserDto user = getCurrentUserDetails();
        String auditType = resolveAuditTypeForCaller(user, request != null ? request.getAuditType() : null);
        validateAuditTypeForRole(user.getRole(), auditType);
        if (request != null && request.isSharedAdministrativeForm() && "administrative".equals(auditType)) {
            return ResponseEntity.ok(submissionService.saveSharedAdministrativeContribution(user, request.getContributorPost(),
                    request.getSections(), request.getValuesData(), request.getTablesData(), request.getAttachments(), false));
        }
        Submission saved = submissionService.saveDraft(
                email,
                auditType,
                user.getSchool(),
                user.getName(),
                request != null ? request.getValuesData() : null,
                request != null ? request.getTablesData() : null,
                request != null ? request.getAttachments() : null
        );
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/submit")
    public ResponseEntity<Submission> submitForm(@RequestBody(required = false) FormSubmissionRequest request) {
        String email = getCurrentUserEmail();
        UserDto user = getCurrentUserDetails();
        String auditType = resolveAuditTypeForCaller(user, request != null ? request.getAuditType() : null);
        validateAuditTypeForRole(user.getRole(), auditType);
        if (request != null && request.isSharedAdministrativeForm() && "administrative".equals(auditType)) {
            return ResponseEntity.ok(submissionService.saveSharedAdministrativeContribution(user, request.getContributorPost(),
                    request.getSections(), request.getValuesData(), request.getTablesData(), request.getAttachments(), true));
        }
        Submission submitted = submissionService.submitForm(
                email,
                auditType,
                user.getSchool(),
                user.getName(),
                request != null ? request.getValuesData() : null,
                request != null ? request.getTablesData() : null,
                request != null ? request.getAttachments() : null
        );
        return ResponseEntity.ok(submitted);
    }

    @PutMapping("/save-draft")
    public ResponseEntity<Submission> updateDraft(@RequestBody(required = false) FormSubmissionRequest request) {
        return saveDraft(request);
    }

    @PutMapping("/submit")
    public ResponseEntity<Submission> updateAndSubmitForm(@RequestBody(required = false) FormSubmissionRequest request) {
        return submitForm(request);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Submission> updateSubmission(@PathVariable Long id, @RequestBody FormSubmissionRequest request) {
        UserDto user = getCurrentUserDetails();
        if (request.getAuditType() != null && !List.of("vice-chancellor", "iqac").contains(user.getRole().toLowerCase())) {
            validateAuditTypeForRole(user.getRole(), request.getAuditType());
        }
        if (request.isSharedAdministrativeForm() && "administrative".equalsIgnoreCase(request.getAuditType())) {
            Submission updated = submissionService.updateSharedAdministrativeContribution(
                    id,
                    user,
                    request.getAction(),
                    request.getContributorPost(),
                    request.getSections(),
                    request.getValuesData(),
                    request.getTablesData(),
                    request.getAttachments()
            );
            return ResponseEntity.ok(updated);
        }
        List<String> selectedKeys = request.getCorrectionAssignmentKeys();
        if (selectedKeys == null || selectedKeys.isEmpty()) {
            selectedKeys = request.getAuditorAssignmentKeys();
        }
        if (selectedKeys == null || selectedKeys.isEmpty()) {
            selectedKeys = request.getAssignmentKeys();
        }
        if (selectedKeys == null || selectedKeys.isEmpty()) {
            selectedKeys = request.getReturnedAuditorAssignmentKeys();
        }

        log.info("PUT /api/submissions/{} called by {}: status={}, forwardedAuditorType={}, effectiveIds={}, effectiveEmails={}",
                id, user.getEmail(), request.getStatus(), request.getEffectiveAuditorType(), request.getEffectiveAuditorIds(), request.getEffectiveAuditorEmails());

        Submission updated = submissionService.updateSubmission(
                id,
                user,
                request.getStatus(),
                request.getEffectiveAuditorType(),
                request.getEffectiveAuditCategory(),
                request.getEffectiveAuditorIds(),
                request.getEffectiveAuditorNames(),
                request.getEffectiveAuditorEmails(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments(),
                request.getForwardedAdministrativePosts(),
                request.getForwardedToAuditorPosts(),
                request.getAuditorCorrectionRequested(),
                request.getCorrectionRequestedForAuditor(),
                request.getRequiresAuditorResubmission(),
                request.getAuditorCorrectionMessage(),
                request.getAuditorCorrectionRequestedBy(),
                request.getAuditorCorrectionRequestedByRole(),
                request.getAuditorCorrectionRequestedOn(),
                request.getAuditorResubmittedAt(),
                request.getRemarks(),
                selectedKeys
        );
        return ResponseEntity.ok(updated);
    }


    @PostMapping("/{id}/auditor-submit")
public ResponseEntity<?> submitAuditorReview(@PathVariable Long id, @RequestBody AuditorSubmitRequest request) {
        UserDto user = getCurrentUserDetails();
        Object response = submissionService.submitAuditorReview(id, user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
public ResponseEntity<List<Submission>> getAllSubmissions(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String auditCycle,
            @RequestParam(required = false) String cycleId) {
        UserDto user = getCurrentUserDetails();
        String requestedYear = firstNonBlank(academicYear, auditCycle, cycleId);
        List<Submission> submissions = submissionService.getAllSubmissionsForUser(user, requestedYear);
        submissions.forEach(sub -> submissionService.populatePermissions(sub, user));
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/previous-reports")
public ResponseEntity<List<Submission>> getPreviousReports(@RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(submissionService.getPreviousReports(getCurrentUserDetails(), academicYear));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmissionById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        UserDto user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        if (!"APPROVED".equalsIgnoreCase(submission.getStatus()) && !"FINAL".equalsIgnoreCase(submission.getStatus())) {
            if (submission.getEmail() != null) {
                java.util.Optional<UserDto> submitter = java.util.Optional.ofNullable(safeGetUserByEmail(submission.getEmail().trim().toLowerCase()));
                if (submitter.isPresent() && Boolean.TRUE.equals(submitter.get().getDeleted())) {

                    String currentYear = submissionService.getCurrentAcademicYearLabel();
                    if (submissionService.isSameAcademicYear(currentYear, submission.getAcademicYear()) || submissionService.isSameAcademicYear(currentYear, submission.getAuditCycle())) {
                        throw new IllegalArgumentException("Submission not found with ID: " + id);
                    }
                }
            }
        }

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        boolean isAdministrativeContributor = "administrative".equalsIgnoreCase(user.getRole())
                && "administrative".equalsIgnoreCase(submission.getAuditType());
        
        boolean isAssignedAuditor = isAuditor && (submissionService.isAuditorAssigned(user, submission) || submissionService.isAuditorFallbackMatch(user, submission));

        if (isVc) {
            boolean statusAllowed = List.of("AUDITOR_COMPLETED", "APPROVED", "FINAL").contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                return ResponseEntity.status(403).build();
            }
        }

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor && !isAdministrativeContributor) {
            return ResponseEntity.status(403).build();
        }

        submissionService.populatePermissions(submission, user);
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/{id}/review")
public ResponseEntity<Submission> reviewSubmission(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        UserDto reviewer = getCurrentUserDetails();
        Submission updated = submissionService.reviewSubmission(
                id,
                request.getStatus(),
                request.getRemarks(),
                request.getReportCategory(),
                request.getAuditCycle(),
                request.getVersion(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments(),
                reviewer
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/next-cycle")
public ResponseEntity<Submission> createNextCycle(
            @PathVariable Long id,
            @RequestBody NextCycleRequest request) {
        UserDto caller = getCurrentUserDetails();
        Submission nextSubmission = submissionService.createNextCycle(
                id,
                caller,
                request.isPreserveApprovedVersion(),
                request.getPreviousApprovedSubmissionId(),
                request.getNextVersion(),
                request.getNextAuditorType()
        );
        return ResponseEntity.ok(nextSubmission);
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<Map<String, Object>> getSnapshots(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        UserDto user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        boolean isAdministrativeContributor = "administrative".equalsIgnoreCase(user.getRole())
                && "administrative".equalsIgnoreCase(submission.getAuditType());
        
        boolean isAssignedAuditor = isAuditor && (submissionService.isAuditorAssigned(user, submission) || submissionService.isAuditorFallbackMatch(user, submission));

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor && !isAdministrativeContributor) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(Map.of("data", submissionService.getVersionHistoryForSubmission(id)));
    }

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class FormSubmissionRequest {
        private String auditType;
        private String academicYear;
        private String auditCycle;
        private String cycleId;
        private String valuesData;
        private String tablesData;
        private String attachments;
        private String status;
        private boolean sharedAdministrativeForm;
        private String action;
        private String contributorPost;
        private List<String> sections;
        private String forwardedAuditorType;
        private String auditorType;
        private String forwardedAuditCategory;
        private String auditCategory;
        private List<Long> forwardedToAuditorIds;
        private List<Long> auditorIds;
        private Long forwardedToAuditorId;
        private Long auditorId;
        private List<String> forwardedToAuditorNames;
        private List<String> auditorNames;
        private String forwardedToAuditorName;
        private String auditorName;
        private List<String> forwardedToAuditorEmails;
        private List<String> auditorEmails;
        private String forwardedToAuditorEmail;
        private String auditorEmail;
        private List<String> forwardedAdministrativePosts;
        private List<String> forwardedToAuditorPosts;
        private List<String> assignmentKeys;
        private List<String> auditorAssignmentKeys;
        private List<String> correctionAssignmentKeys;
        private List<String> returnedAuditorAssignmentKeys;
        private Boolean auditorCorrectionRequested;
        private Boolean correctionRequestedForAuditor;
        private Boolean requiresAuditorResubmission;
        private String auditorCorrectionMessage;
        private String auditorCorrectionRequestedBy;
        private String auditorCorrectionRequestedByRole;
        private String auditorCorrectionRequestedOn;
        private String auditorResubmittedAt;
        private String remarks;

        public List<Long> getEffectiveAuditorIds() {
            List<Long> ids = new java.util.ArrayList<>();
            if (forwardedToAuditorIds != null) ids.addAll(forwardedToAuditorIds);
            if (auditorIds != null) ids.addAll(auditorIds);
            if (forwardedToAuditorId != null) ids.add(forwardedToAuditorId);
            if (auditorId != null) ids.add(auditorId);
            return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        }

        public List<String> getEffectiveAuditorEmails() {
            List<String> emails = new java.util.ArrayList<>();
            if (forwardedToAuditorEmails != null) emails.addAll(forwardedToAuditorEmails);
            if (auditorEmails != null) emails.addAll(auditorEmails);
            if (forwardedToAuditorEmail != null && !forwardedToAuditorEmail.isBlank()) emails.add(forwardedToAuditorEmail);
            if (auditorEmail != null && !auditorEmail.isBlank()) emails.add(auditorEmail);
            return emails.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
        }

        public List<String> getEffectiveAuditorNames() {
            List<String> names = new java.util.ArrayList<>();
            if (forwardedToAuditorNames != null) names.addAll(forwardedToAuditorNames);
            if (auditorNames != null) names.addAll(auditorNames);
            if (forwardedToAuditorName != null && !forwardedToAuditorName.isBlank()) names.add(forwardedToAuditorName);
            if (auditorName != null && !auditorName.isBlank()) names.add(auditorName);
            return names.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
        }

        public String getEffectiveAuditorType() {
            if (forwardedAuditorType != null && !forwardedAuditorType.isBlank()) return forwardedAuditorType.trim();
            if (auditorType != null && !auditorType.isBlank()) return auditorType.trim();
            return "internal";
        }

        public String getEffectiveAuditCategory() {
            if (forwardedAuditCategory != null && !forwardedAuditCategory.isBlank()) return forwardedAuditCategory.trim();
            if (auditCategory != null && !auditCategory.isBlank()) return auditCategory.trim();
            if (auditType != null && !auditType.isBlank()) return auditType.trim();
            return "academic";
        }
    }


    @Data
    public static class ReviewRequest {
        private String status; // APPROVED/FINAL, UNDER_REVIEW
        private String remarks;
        private String reportCategory;
        private String auditCycle;
        private Integer version;
        private String valuesData;
        private String tablesData;
        private String attachments;
    }

    @Data
    public static class NextCycleRequest {
        private boolean preserveApprovedVersion = true;
        private Long previousApprovedSubmissionId;
        private Integer nextVersion;
        private String nextAuditorType;
    }

    public void downloadAttachments(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        downloadAttachments(id, false, response);
    }

    @GetMapping("/{id}/attachments/download")
public void downloadAttachments(@PathVariable Long id,
                                    @RequestParam(required = false, defaultValue = "false") boolean includeAllContributors,
                                    jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        UserDto user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new com.director_appraisal.submission_service.exception.NotFoundException("Submission not found"));

        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        if (!isIqac && !isVc) {
            throw new SecurityException("Only IQAC or VC may download attachments");
        }

        if (isVc) {
            boolean statusAllowed = List.of("AUDITOR_COMPLETED", "APPROVED", "FINAL").contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                throw new SecurityException("Unauthorized access to submission");
            }
        } else {
            // IQAC
            boolean statusAllowed = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", "APPROVED", "FINAL")
                    .contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                throw new SecurityException("Unauthorized access to submission");
            }
        }

        // submission.attachments remains primary, but table/value payloads may contain section-specific attachments.
        List<ExtractedAttachment> attachments = new java.util.ArrayList<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        try {
            if (submission.getAttachments() != null && !submission.getAttachments().isBlank()) {
                collectAttachments(mapper.readTree(submission.getAttachments()), attachments);
            }
            if (submission.getTablesData() != null && !submission.getTablesData().isBlank()) {
                collectAttachments(mapper.readTree(submission.getTablesData()), attachments);
            }
            if (submission.getValuesData() != null && !submission.getValuesData().isBlank()) {
                collectAttachments(mapper.readTree(submission.getValuesData()), attachments);
            }
        } catch (Exception e) {
            // Ignore parse errors, just use what we can parse
        }
        attachments = deduplicateAttachments(attachments);

        if (attachments.isEmpty()) {
            throw new com.director_appraisal.submission_service.exception.NotFoundException("No attachments found for this submission");
        }

        String zipFileName = getZipFileName(submission);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        java.util.Set<String> usedPaths = new java.util.HashSet<>();
        List<String> missingFiles = new java.util.ArrayList<>();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
            for (ExtractedAttachment att : attachments) {
                if (att.url == null || att.url.isBlank()) {
                    continue;
                }

                String folderPath = getZipFolderPath(att, submission.getAuditType());
                String sanitizedName = sanitizeFilename(att.fileName);
                String zipEntryPath = folderPath + sanitizedName;

                if (usedPaths.contains(zipEntryPath)) {
                    int dotIndex = sanitizedName.lastIndexOf('.');
                    String namePart = dotIndex >= 0 ? sanitizedName.substring(0, dotIndex) : sanitizedName;
                    String extPart = dotIndex >= 0 ? sanitizedName.substring(dotIndex) : "";
                    int counter = 1;
                    String newEntryPath;
                    do {
                        newEntryPath = folderPath + namePart + "_" + counter + extPart;
                        counter++;
                    } while (usedPaths.contains(newEntryPath));
                    zipEntryPath = newEntryPath;
                }
                try (java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(new byte[0])) {
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(zipEntryPath);
                    zos.putNextEntry(entry);
                    zos.closeEntry();
                } catch (Exception e) {
                    System.err.println("Skipping inaccessible attachment: " + att.url + " - " + e.getMessage());
                    missingFiles.add("File: " + att.fileName + ", URL: " + att.url + ", Error: " + e.getMessage());
                }
            }

            if (!missingFiles.isEmpty()) {
                java.util.zip.ZipEntry missingEntry = new java.util.zip.ZipEntry("missing-files.txt");
                zos.putNextEntry(missingEntry);
                String content = String.join("\n", missingFiles);
                zos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }

    private void collectAttachments(com.fasterxml.jackson.databind.JsonNode node, List<ExtractedAttachment> list) {
        collectAttachments(node, list, null);
    }

    private void collectAttachments(com.fasterxml.jackson.databind.JsonNode node, List<ExtractedAttachment> list, String sectionContext) {
        if (node == null) return;
        if (node.isObject()) {
            String currentSection = sectionContext;
            String url = null;
            if (node.has("url") && node.get("url").isTextual()) {
                url = node.get("url").asText();
            } else if (node.has("publicUrl") && node.get("publicUrl").isTextual()) {
                url = node.get("publicUrl").asText();
            } else if (node.has("downloadUrl") && node.get("downloadUrl").isTextual()) {
                url = node.get("downloadUrl").asText();
            } else if (node.has("fileUrl") && node.get("fileUrl").isTextual()) {
                url = node.get("fileUrl").asText();
            }

            if (url != null && !url.isBlank()) {
                String lowerUrl = url.toLowerCase();
                boolean isAttachment = lowerUrl.contains("/uploads/")
                        || lowerUrl.contains("/attachments/")
                        || lowerUrl.startsWith("users/")
                        || lowerUrl.contains("/users/")
                        || lowerUrl.contains("storage.googleapis.com")
                        || lowerUrl.endsWith(".pdf")
                        || lowerUrl.endsWith(".docx")
                        || lowerUrl.endsWith(".xlsx")
                        || lowerUrl.endsWith(".png")
                        || lowerUrl.endsWith(".jpg")
                        || lowerUrl.endsWith(".jpeg")
                        || lowerUrl.endsWith(".doc")
                        || lowerUrl.endsWith(".xls")
                        || lowerUrl.endsWith(".zip");

                if (isAttachment) {
                    ExtractedAttachment att = new ExtractedAttachment();
                    att.url = url;
                    att.objectKey = extractObjectKey(url);
                    att.sectionId = currentSection;
                    
                    if (node.has("fileName") && node.get("fileName").isTextual()) {
                        att.fileName = node.get("fileName").asText();
                    } else if (node.has("name") && node.get("name").isTextual()) {
                        att.fileName = node.get("name").asText();
                    } else {
                        int lastSlash = url.lastIndexOf('/');
                        att.fileName = lastSlash >= 0 ? url.substring(lastSlash + 1) : "attachment.pdf";
                    }
                    
                    if (node.has("sectionId") && node.get("sectionId").isTextual()) {
                        att.sectionId = node.get("sectionId").asText();
                    }
                    if (node.has("tableId") && node.get("tableId").isTextual()) {
                        att.tableId = node.get("tableId").asText();
                    }
                    if (node.has("rowIndex") && node.get("rowIndex").isNumber()) {
                        att.rowIndex = node.get("rowIndex").asInt();
                    }
                    if (node.has("column") && node.get("column").isTextual()) {
                        att.column = node.get("column").asText();
                    }
                    if (node.has("id")) {
                        att.id = node.get("id").asText();
                    } else if (node.has("attachmentId")) {
                        att.id = node.get("attachmentId").asText();
                    }
                    if (node.has("objectKey")) {
                        att.objectKey = node.get("objectKey").asText();
                    } else if (node.has("storageObjectKey")) {
                        att.objectKey = node.get("storageObjectKey").asText();
                    }
                    if (node.has("checksum")) {
                        att.checksum = node.get("checksum").asText();
                    } else if (node.has("sha256")) {
                        att.checksum = node.get("sha256").asText();
                    }
                    if (node.has("size")) {
                        att.size = node.get("size").asText();
                    } else if (node.has("fileSize")) {
                        att.size = node.get("fileSize").asText();
                    }
                    list.add(att);
                }
            }
            node.fields().forEachRemaining(entry ->
                    collectAttachments(entry.getValue(), list, resolveAttachmentSectionContext(entry.getKey(), currentSection)));
        } else if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                collectAttachments(item, list, sectionContext);
            }
        }
    }

    private String resolveAttachmentSectionContext(String key, String currentSection) {
        if (key == null || key.isBlank()) {
            return currentSection;
        }
        String normalized = key.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.contains("scholarshipsummary")
                || normalized.contains("scholarshipdetails")
                || normalized.contains("scholarshipstudents")
                || normalized.contains("scholarshipstudentdetails")
                || normalized.contains("coursesoffered")
                || normalized.contains("studentstatistics")
                || normalized.contains("statutory")
                || normalized.contains("auditrecords")) {
            return "registrar-part-a";
        }
        if (normalized.contains("infrastructure")
                || normalized.contains("library") || normalized.contains("eresource")
                || normalized.contains("researchresource")) {
            return "registrar-part-c";
        }
        if (normalized.contains("faculty") || normalized.contains("staff") || normalized.contains("bogmom")) {
            return "hr-part-b";
        }
        if (normalized.contains("hackathon")
                || normalized.contains("ideation")
                || normalized.contains("cultural")
                || normalized.contains("sportsactivities")
                || normalized.contains("sportsclubs")
                || normalized.contains("community")
                || normalized.contains("adminstudentawards")
                || normalized.contains("awardsprizesrecognitions")) {
            return "dean-student-welfare-part-d";
        }
        if (normalized.contains("parte") || normalized.contains("parteschools")
                || normalized.contains("placement") || normalized.contains("trainingactivities")
                || normalized.contains("industrycollaboration")) {
            return "dean-placement-part-e";
        }
        return currentSection;
    }

    private List<ExtractedAttachment> deduplicateAttachments(List<ExtractedAttachment> attachments) {
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        List<ExtractedAttachment> deduped = new java.util.ArrayList<>();
        List<ExtractedAttachment> noKey = new java.util.ArrayList<>();
        for (ExtractedAttachment attachment : attachments) {
            List<String> keys = attachmentIdentityKeys(attachment);
            if (keys.isEmpty()) {
                noKey.add(attachment);
                continue;
            }
            String matchedKey = keys.stream().filter(seenKeys::contains).findFirst().orElse(null);
            if (matchedKey != null) {
                System.err.println("Skipping duplicate attachment in ZIP: " + matchedKey);
                continue;
            }
            seenKeys.addAll(keys);
            deduped.add(attachment);
        }
        deduped.addAll(noKey);
        return deduped;
    }

    private List<String> attachmentIdentityKeys(ExtractedAttachment attachment) {
        List<String> keys = new java.util.ArrayList<>();
        if (notBlank(attachment.id)) {
            keys.add("id:" + attachment.id.trim());
        }
        if (notBlank(attachment.objectKey)) {
            keys.add("key:" + normalizeAttachmentUrl(attachment.objectKey));
        }
        if (notBlank(attachment.url)) {
            keys.add("url:" + normalizeAttachmentUrl(attachment.url));
        }
        if (notBlank(attachment.checksum)) {
            keys.add("checksum:" + attachment.checksum.trim().toLowerCase());
        }
        if (notBlank(attachment.fileName) && notBlank(attachment.size)) {
            keys.add("name-size:" + attachment.fileName.trim().toLowerCase() + ":" + attachment.size.trim());
        }
        return keys;
    }

    private String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.contains("users/")) {
            int idx = url.indexOf("users/");
            return url.substring(idx);
        }
        if (url.contains("/uploads/")) {
            int idx = url.indexOf("/uploads/");
            return url.substring(idx + "/uploads/".length());
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            if (path.contains("users/")) {
                int idx = path.indexOf("users/");
                return path.substring(idx);
            }
            if (path.contains("/uploads/")) {
                int idx = path.indexOf("/uploads/");
                return path.substring(idx + "/uploads/".length());
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (Exception e) {
            return url;
        }
    }

    private String normalizeAttachmentUrl(String value) {
        String normalized = value == null ? "" : value.trim().replace("\\", "/");
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String getZipFileName(Submission submission) {
        String type = "academic".equalsIgnoreCase(submission.getAuditType()) ? "Academic" : "Administrative";
        String entityName = "Unknown";
        if ("academic".equalsIgnoreCase(submission.getAuditType())) {
            entityName = SchoolUtils.canonicalizeSchool(submission.getSchool());
        } else {
            entityName = Optional.ofNullable(safeGetUserByEmail(submission.getEmail()))
                    .map(UserDto::getPost)

                    .orElse(submission.getAdministrativePost());
            entityName = formatAdministrativePost(entityName);
        }
        if (entityName == null || entityName.isBlank()) {
            entityName = "Unknown";
        }
        entityName = entityName.replaceAll("[^A-Za-z0-9._-]", "_");
        String cycle = submission.getAuditCycle() != null ? submission.getAuditCycle() : submission.getAcademicYear();
        if (cycle == null || cycle.isBlank()) {
            cycle = "2025-2026";
        }
        cycle = cycle.replaceAll("[^A-Za-z0-9._-]", "_");
        return type + "_" + entityName + "_" + cycle + ".zip";
    }

    private String formatAdministrativePost(String post) {
        if (post == null || post.isBlank()) {
            return "Unknown";
        }
        return switch (post.trim().toLowerCase()) {
            case "registrar" -> "Registrar";
            case "hr" -> "HR";
            case "dean-student-welfare" -> "Dean_Student_Welfare";
            case "dean-placement" -> "Dean_Placement";
            default -> post;
        };
    }

    private String getZipFolderPath(ExtractedAttachment att, String auditType) {
        String sec = att.sectionId != null ? att.sectionId.trim().toLowerCase() : "";
        if ("academic".equalsIgnoreCase(auditType)) {
            if (sec.contains("part-a") || sec.contains("parta")) {
                return "Part-A/";
            } else if (sec.contains("part-b") || sec.contains("partb")) {
                return "Part-B/";
            } else if (sec.contains("part-c") || sec.contains("partc")) {
                return "Part-C/";
            } else if (sec.contains("part-d") || sec.contains("partd")) {
                return "Part-D/";
            }
            return "Other-Attachments/";
        } else {
            if (sec.contains("registrar-part-a")) {
                return "Registrar/Part-A/";
            } else if (sec.contains("registrar-part-c")) {
                return "Registrar/Part-C/";
            } else if (sec.contains("hr-part-b")) {
                return "HR/Part-B/";
            } else if (sec.contains("dean-student-welfare-part-d")) {
                return "Dean-Student-Welfare/Part-D/";
            } else if (sec.contains("dean-placement-part-e")) {
                return "Dean-Placement/Part-E/";
            }
            if (sec.contains("section-a") || sec.contains("sectiona") || sec.contains("part-a") || sec.contains("parta")) {
                return "Section-A/";
            } else if (sec.contains("section-b") || sec.contains("sectionb") || sec.contains("part-b") || sec.contains("partb")) {
                return "Section-B/";
            } else if (sec.contains("section-c") || sec.contains("sectionc") || sec.contains("part-c") || sec.contains("partc")) {
                return "Section-C/";
            }
            return "Other-Attachments/";
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file.pdf";
        }
        filename = filename.replace("\\", "/");
        int lastSlash = filename.lastIndexOf('/');
        String base = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        base = base.replace("..", "_");
        String clean = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.isBlank() ? "file.pdf" : clean;
    }

    @Data
    public static class ExtractedAttachment {
        private String fileName;
        private String url;
        private String sectionId;
        private String tableId;
        private Integer rowIndex;
        private String column;
        private String id;
        private String objectKey;
        private String checksum;
        private String size;
    }

    @Data
    public static class AuditorSubmitRequest {
        private Long auditorId;
        private String auditorName;
        private String auditorEmail;
        private String auditorType;
        private String auditCategory;
        private List<String> postsSubmitted;
        private List<String> submittedPosts;
        private List<String> administrativePosts;
        private List<String> assignedPosts;
        private List<String> posts;
        private List<String> assignmentKeys;
        private String submittedAt;
        private String reviewStatus;
        private String valuesData;
        private String tablesData;
        private String attachments;
        private Boolean auditorCorrectionRequested;
        private Boolean correctionRequestedForAuditor;
        private Boolean requiresAuditorResubmission;
    }
}
