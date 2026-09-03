-- V1: Initialize Clean Multi-Tenant Submissions Schema

CREATE TABLE IF NOT EXISTS submissions (
    id BIGSERIAL PRIMARY KEY,
    audit_type VARCHAR(50) NOT NULL,
    academic_year VARCHAR(50) NOT NULL,
    school_id VARCHAR(100),
    post_id VARCHAR(100),
    user_id VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    university_id BIGINT,
    university_code VARCHAR(50),
    schema_version_id BIGINT,
    values_data TEXT,
    tables_data TEXT,
    attachments TEXT,
    auditor_remarks TEXT,
    auditor_status VARCHAR(50),
    auditor_completed_at TIMESTAMP WITHOUT TIME ZONE,
    auditor_id VARCHAR(100),
    auditor_name VARCHAR(255),
    director_sign_off TEXT,
    auditor_sign_off TEXT,
    iqac_sign_off TEXT,
    vc_sign_off TEXT,
    submitted_at TIMESTAMP WITHOUT TIME ZONE,
    approved_at TIMESTAMP WITHOUT TIME ZONE,
    root_submission_id BIGINT,
    is_latest_cycle BOOLEAN DEFAULT TRUE,
    cycle_number INTEGER DEFAULT 1,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snapshots (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    snapshot_type VARCHAR(50) NOT NULL,
    snapshot_data TEXT NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS submission_auditor_assignments (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    auditor_id VARCHAR(100) NOT NULL,
    auditor_name VARCHAR(255),
    auditor_email VARCHAR(255),
    school_id VARCHAR(100),
    post_id VARCHAR(100),
    status VARCHAR(50) DEFAULT 'ASSIGNED',
    remarks TEXT,
    assigned_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS academic_years (
    id BIGSERIAL PRIMARY KEY,
    academic_year VARCHAR(50) NOT NULL UNIQUE,
    is_current BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_submissions_lookup ON submissions(audit_type, academic_year, school_id, post_id);
CREATE INDEX IF NOT EXISTS idx_submissions_university_id ON submissions(university_id);
CREATE INDEX IF NOT EXISTS idx_submissions_university_code ON submissions(university_code);
CREATE INDEX IF NOT EXISTS idx_submissions_schema_version_id ON submissions(schema_version_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_submission_id ON snapshots(submission_id);
CREATE INDEX IF NOT EXISTS idx_auditor_assignments_sub_id ON submission_auditor_assignments(submission_id);
