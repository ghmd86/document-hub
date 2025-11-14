CREATE TABLE `document_events_ledger` (
  `doc_event_id` UUID DEFAULT gen_random_uuid(),
  `storage_index_id` UUID,
  `actor_id` UUID,
  `actor_type` varchar(50),
  `account_id` UUID,
  `event_timestamp` TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `event_type` varchar(50) CHECK (event_type in ('CREATED', 'VIEWED', 'DELETED', 'SIGNED', 'RESTORED', 'SHARED', 'PRINTED', 'REPRINTED'),
  `event_data ` JSONB NOT NULL DEFAULT '{}'::jsonb,
  `source_app` varchar(100),
  `device_type` varchar(100),
  `event_code` varchar(100),
  `location` varchar(255),
  `master_template_id` UUID,
  PRIMARY KEY (`doc_event_id`)
);

