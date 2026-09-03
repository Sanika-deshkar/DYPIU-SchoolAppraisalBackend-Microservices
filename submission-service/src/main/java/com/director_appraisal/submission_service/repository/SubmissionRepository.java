package com.director_appraisal.submission_service.repository;

import com.director_appraisal.submission_service.model.Submission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findAllByEmailIgnoreCase(String email);
    Optional<Submission> findByEmailAndAuditType(String email, String auditType);
    Optional<Submission> findFirstByEmailAndAuditTypeAndStatusInOrderByIdDesc(String email, String auditType, List<String> statuses);
    Optional<Submission> findFirstByEmailAndAuditTypeOrderByIdDesc(String email, String auditType);
    Optional<Submission> findFirstByEmailAndAuditTypeAndAcademicYearAndStatusInOrderByIdDesc(String email, String auditType, String academicYear, List<String> statuses);
    Optional<Submission> findFirstByEmailAndAuditTypeAndAcademicYearOrderByIdDesc(String email, String auditType, String academicYear);
    Optional<Submission> findFirstByEmailAndAuditTypeAndAuditCycleOrderByIdDesc(String email, String auditType, String auditCycle);
    boolean existsByEmailAndAuditTypeAndAcademicYearAndVersion(String email, String auditType, String academicYear, Integer version);
    Optional<Submission> findByEmailAndAuditTypeAndAcademicYearAndVersion(String email, String auditType, String academicYear, Integer version);
    List<Submission> findByStatusIn(List<String> statuses);
    List<Submission> findByAuditTypeAndStatusIn(String auditType, List<String> statuses);
    List<Submission> findByUniversityIdAndStatusIn(Long universityId, List<String> statuses);
    List<Submission> findAllByUniversityId(Long universityId);
    Optional<Submission> findFirstByEmailAndAuditTypeAndAcademicYearAndUniversityIdOrderByIdDesc(String email, String auditType, String academicYear, Long universityId);
    Optional<Submission> findFirstByEmailAndAuditTypeAndAuditCycleAndUniversityIdOrderByIdDesc(String email, String auditType, String auditCycle, Long universityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Submission s where s.id = :id")
    Optional<Submission> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s from Submission s
            where s.id = :rootSubmissionId or s.rootSubmissionId = :rootSubmissionId
            order by s.version asc, s.id asc
            """)
    List<Submission> findLineage(@Param("rootSubmissionId") Long rootSubmissionId);

    @Query("""
            select coalesce(max(s.version), 0) from Submission s
            where s.id = :rootSubmissionId or s.rootSubmissionId = :rootSubmissionId
            """)
    Integer findMaxVersionInLineage(@Param("rootSubmissionId") Long rootSubmissionId);

    Optional<Submission> findByRootSubmissionIdAndVersion(Long rootSubmissionId, Integer version);
    Optional<Submission> findByParentSubmissionId(Long parentSubmissionId);

    @Query("""
            select s from Submission s
            where lower(s.auditType) = lower(:auditType)
            and (
                s.academicYear in (:yearLabels)
                or s.auditCycle in (:yearLabels)
            )
            order by s.version desc, s.id desc
            """)
    List<Submission> findSubmissionsByAuditTypeAndYearLabels(@Param("auditType") String auditType, @Param("yearLabels") List<String> yearLabels);

    @Query("""
            select s from Submission s
            where lower(s.email) = lower(:email)
            and lower(s.auditType) = lower(:auditType)
            and (
                s.academicYear in (:yearLabels)
                or s.auditCycle in (:yearLabels)
            )
            order by s.version desc, s.id desc
            """)
    List<Submission> findSubmissionsByEmailAndAuditTypeAndYearLabels(@Param("email") String email, @Param("auditType") String auditType, @Param("yearLabels") List<String> yearLabels);

    @Query("select distinct s.academicYear from Submission s where s.academicYear is not null")
    List<String> findDistinctAcademicYears();

    @Query("select distinct s.auditCycle from Submission s where s.auditCycle is not null")
    List<String> findDistinctAuditCycles();
}


