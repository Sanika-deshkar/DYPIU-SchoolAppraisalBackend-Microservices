-- V1: Initialize Clean Multi-Tenant Dynamic Form Schema

CREATE TABLE IF NOT EXISTS universities (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    address TEXT,
    establishment_act VARCHAR(255),
    logo_url VARCHAR(500),
    iqac_logo_url VARCHAR(500),
    primary_color VARCHAR(50),
    theme_branding TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS form_schemas (
    id BIGSERIAL PRIMARY KEY,
    university_id BIGINT NOT NULL,
    audit_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active_version_number INTEGER,
    active_version_id BIGINT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schema_versions (
    id BIGSERIAL PRIMARY KEY,
    schema_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    academic_year VARCHAR(50),
    title VARCHAR(255),
    owner_role VARCHAR(100),
    compiled_schema TEXT,
    published_by VARCHAR(255),
    published_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS form_sections (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    section_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    section_number VARCHAR(50),
    owner_role VARCHAR(100),
    description TEXT,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS form_tables (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    table_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    show_title BOOLEAN DEFAULT TRUE,
    is_repeatable BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    initial_rows TEXT,
    select_options TEXT,
    date_columns TEXT,
    number_columns TEXT,
    textarea_columns TEXT,
    textarea_max_lengths TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS form_fields (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL,
    table_id BIGINT,
    field_key VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    kind VARCHAR(50),
    is_required BOOLEAN DEFAULT FALSE,
    placeholder VARCHAR(255),
    default_value VARCHAR(255),
    validation_rules TEXT,
    options TEXT,
    attachment_rules TEXT,
    display_order INTEGER DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS university_schools (
    id BIGSERIAL PRIMARY KEY,
    university_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    group_name VARCHAR(50) DEFAULT 'general',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high-performance tenant querying
CREATE INDEX IF NOT EXISTS idx_form_schemas_university_id ON form_schemas(university_id);
CREATE INDEX IF NOT EXISTS idx_form_schemas_audit_type ON form_schemas(audit_type);
CREATE INDEX IF NOT EXISTS idx_schema_versions_schema_id ON schema_versions(schema_id);
CREATE INDEX IF NOT EXISTS idx_form_sections_version_id ON form_sections(version_id);
CREATE INDEX IF NOT EXISTS idx_form_tables_section_id ON form_tables(section_id);
CREATE INDEX IF NOT EXISTS idx_form_fields_section_id ON form_fields(section_id);
CREATE INDEX IF NOT EXISTS idx_form_fields_table_id ON form_fields(table_id);
CREATE INDEX IF NOT EXISTS idx_uni_schools_university_id ON university_schools(university_id);
CREATE INDEX IF NOT EXISTS idx_uni_schools_code ON university_schools(code);
