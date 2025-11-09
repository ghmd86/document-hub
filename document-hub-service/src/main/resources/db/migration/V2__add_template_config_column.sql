-- Add template_config JSONB column to master_template_definition table
-- This column stores operational configurations including vendor preferences and upload settings

ALTER TABLE master_template_definition
ADD COLUMN IF NOT EXISTS template_config JSONB;

-- Add comment to document the column purpose
COMMENT ON COLUMN master_template_definition.template_config IS
'Operational configuration for the template including: defaultPrintVendor, defaultEmailVendor, printVendorFailover, uploadReferenceKeyField';

-- Create index for efficient JSONB queries (optional but recommended)
CREATE INDEX IF NOT EXISTS idx_template_config_default_print_vendor
ON master_template_definition USING gin ((template_config -> 'defaultPrintVendor'));

CREATE INDEX IF NOT EXISTS idx_template_config_default_email_vendor
ON master_template_definition USING gin ((template_config -> 'defaultEmailVendor'));

-- Example: Update existing templates with default configuration (optional)
-- UPDATE master_template_definition
-- SET template_config = '{"defaultPrintVendor": "SMARTCOMM", "defaultEmailVendor": "SENDGRID"}'::jsonb
-- WHERE template_config IS NULL;
