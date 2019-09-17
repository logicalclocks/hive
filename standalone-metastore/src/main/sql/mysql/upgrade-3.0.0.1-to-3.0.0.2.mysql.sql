SELECT 'Upgrading MetaStore schema from 3.0.0.1 to 3.0.0.2' AS ' ';

-- These lines need to be last.  Insert any changes above.
UPDATE VERSION SET SCHEMA_VERSION='3.0.0.2', VERSION_COMMENT='Hive release version 3.0.0.2' where VER_ID=1;
SELECT 'Finished upgrading MetaStore schema from 3.0.0.1 to 3.0.0.2' AS ' ';
