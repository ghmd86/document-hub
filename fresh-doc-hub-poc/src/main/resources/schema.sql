-- H2 Database Schema for Document Hub POC
-- This is compatible with H2 in-memory database for testing

DROP TABLE IF EXISTS storage_index CASCADE;
DROP TABLE IF EXISTS master_template_definition CASCADE;

CREATE TABLE master_template_definition (
  master_template_id UUID,
  template_version INTEGER,
  legacy_template_id VARCHAR(255),
  legacy_template_name VARCHAR(255),
  display_name VARCHAR(255),
  template_name VARCHAR(255),
  template_description VARCHAR(500),
  template_category VARCHAR(100),
  line_of_business VARCHAR(100),
  template_type VARCHAR(100),
  language_code VARCHAR(20),
  owning_dept VARCHAR(100),
  notification_needed BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT FALSE,
  is_regulatory BOOLEAN DEFAULT FALSE,
  is_message_center_doc BOOLEAN DEFAULT FALSE,
  is_shared_document BOOLEAN DEFAULT FALSE,
  sharing_scope VARCHAR(50),
  data_extraction_config VARCHAR(4000),  -- JSON as VARCHAR for H2
  access_control VARCHAR(4000),          -- JSON as VARCHAR for H2
  channels VARCHAR(2000),                 -- JSON as VARCHAR for H2
  required_fields VARCHAR(2000),          -- JSON as VARCHAR for H2
  template_config VARCHAR(4000),          -- JSON as VARCHAR for H2
  start_date BIGINT,
  end_date BIGINT,
  created_by VARCHAR(100),
  created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(100),
  updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  archive_indicator BOOLEAN,
  archive_timestamp TIMESTAMP,
  version_number BIGINT,
  record_status BIGINT,
  -- Constraints
  PRIMARY KEY (master_template_id, template_version)
);

CREATE TABLE storage_index (
  storage_index_id UUID PRIMARY KEY,
  master_template_id UUID NOT NULL,
  template_version INTEGER NOT NULL,
  template_type VARCHAR(100),
  reference_key VARCHAR(255),
  reference_key_type VARCHAR(100),
  is_shared BOOLEAN,
  account_key UUID,
  customer_key UUID,
  storage_vendor VARCHAR(100),
  storage_document_key UUID,
  generation_vendor VARCHAR(100),
  file_name VARCHAR(500),
  doc_creation_date BIGINT,
  is_accessible INTEGER,
  doc_metadata VARCHAR(4000),            -- JSON as VARCHAR for H2
  created_by VARCHAR(100),
  created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(100),
  updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  archive_indicator BOOLEAN,
  archive_timestamp TIMESTAMP,
  version_number BIGINT,
  record_status BIGINT
);

-- Create indexes for performance
CREATE INDEX idx_template_active ON master_template_definition(is_active, start_date, end_date);
CREATE INDEX idx_template_type ON master_template_definition(template_type, template_version);
CREATE INDEX idx_storage_account ON storage_index(account_key, is_shared);
CREATE INDEX idx_storage_template ON storage_index(master_template_id, template_version);
CREATE INDEX idx_storage_type ON storage_index(template_type, template_version);
