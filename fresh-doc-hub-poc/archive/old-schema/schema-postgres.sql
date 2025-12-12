-- PostgreSQL Schema for Document Hub POC
-- Compatible with PostgreSQL 12+

CREATE SCHEMA IF NOT EXISTS document_hub;

CREATE TABLE IF NOT EXISTS document_hub.master_template_definition
(
    master_template_id uuid NOT NULL,
    template_version integer NOT NULL,
    legacy_template_id varchar,
    template_name varchar NOT NULL,
    template_description varchar,
    line_of_business varchar,
    template_category varchar,
    template_type_old varchar,
    language_code varchar,
    owning_dept varchar,
    notification_needed boolean NOT NULL DEFAULT false,
    regulatory_flag boolean NOT NULL DEFAULT false,
    message_center_doc_flag boolean NOT NULL DEFAULT false,
    document_channel_old jsonb,
    template_variables jsonb,
    start_date bigint,
    end_date bigint,
    created_by varchar NOT NULL,
    created_timestamp timestamp NOT NULL DEFAULT now(),
    updated_by varchar,
    updated_timestamp timestamp,
    archive_indicator boolean NOT NULL DEFAULT false,
    archive_timestamp timestamp,
    version_number bigint NOT NULL DEFAULT 0,
    record_status varchar NOT NULL DEFAULT '1',
    legacy_template_name varchar,
    display_name varchar,
    template_type varchar,
    active_flag boolean,
    shared_document_flag boolean,
    data_extraction_config jsonb,
    access_control jsonb,
    required_fields jsonb,
    template_config jsonb,
    sharing_scope varchar,
    CONSTRAINT master_template_definition_pkey PRIMARY KEY (master_template_id, template_version)
);

CREATE TABLE IF NOT EXISTS document_hub.storage_index
(
    storage_index_id uuid NOT NULL,
    master_template_id uuid NOT NULL,
    storage_vendor varchar,
    reference_key varchar,
    reference_key_type varchar,
    account_key uuid,
    customer_key uuid,
    storage_document_key uuid,
    file_name varchar,
    doc_creation_date bigint,
    accessible_flag boolean NOT NULL DEFAULT true,
    doc_metadata jsonb,
    valid_from bigint,
    valid_until bigint,
    created_by varchar NOT NULL,
    created_timestamp timestamp NOT NULL DEFAULT now(),
    updated_by varchar,
    updated_timestamp timestamp,
    archive_indicator boolean NOT NULL DEFAULT false,
    archive_timestamp timestamp,
    version_number bigint NOT NULL DEFAULT 0,
    record_status varchar NOT NULL DEFAULT '1',
    template_version integer,
    template_type varchar,
    shared_flag boolean NOT NULL DEFAULT false,
    generation_vendor_id uuid,
    CONSTRAINT storage_index_pkey PRIMARY KEY (storage_index_id)
);

CREATE TABLE IF NOT EXISTS document_hub.template_vendor_mapping
(
    template_vendor_id uuid NOT NULL,
    master_template_id uuid NOT NULL,
    vendor varchar NOT NULL,
    vendor_template_key varchar,
    reference_key_type varchar,
    consumer_id uuid,
    template_content bytea,
    start_date bigint,
    end_date bigint,
    vendor_mapping_version integer NOT NULL DEFAULT 1,
    primary_flag boolean NOT NULL DEFAULT false,
    schema_info jsonb,
    active_flag boolean NOT NULL DEFAULT true,
    created_by varchar NOT NULL,
    created_timestamp timestamp NOT NULL DEFAULT now(),
    updated_by varchar,
    updated_timestamp timestamp,
    archive_indicator boolean NOT NULL DEFAULT false,
    archive_timestamp timestamp,
    record_status varchar NOT NULL DEFAULT '1',
    version_number bigint,
    template_version integer,
    vendor_template_name varchar,
    template_fields jsonb,
    vendor_config jsonb,
    api_config jsonb,
    template_status varchar,
    CONSTRAINT template_vendor_mapping_pkey PRIMARY KEY (template_vendor_id)
);

ALTER TABLE document_hub.storage_index
    DROP CONSTRAINT IF EXISTS storageindex_mastertemplatedef_fkey;

ALTER TABLE document_hub.storage_index
    ADD CONSTRAINT storageindex_mastertemplatedef_fkey
    FOREIGN KEY (master_template_id, template_version)
    REFERENCES document_hub.master_template_definition (master_template_id, template_version);

ALTER TABLE document_hub.template_vendor_mapping
    DROP CONSTRAINT IF EXISTS templatevendmapping_mastertemplatedef_fkey;

ALTER TABLE document_hub.template_vendor_mapping
    ADD CONSTRAINT templatevendmapping_mastertemplatedef_fkey
    FOREIGN KEY (master_template_id, template_version)
    REFERENCES document_hub.master_template_definition (master_template_id, template_version);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_storage_index_template
    ON document_hub.storage_index(master_template_id, template_version);

CREATE INDEX IF NOT EXISTS idx_storage_index_reference_key
    ON document_hub.storage_index(reference_key, reference_key_type);

CREATE INDEX IF NOT EXISTS idx_storage_index_account
    ON document_hub.storage_index(account_key);

CREATE INDEX IF NOT EXISTS idx_template_type
    ON document_hub.master_template_definition(template_type);
