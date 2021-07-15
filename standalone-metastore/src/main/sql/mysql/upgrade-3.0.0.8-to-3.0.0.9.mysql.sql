SELECT 'Upgrading MetaStore schema from 3.0.0.8 to 3.0.0.9' AS 'msg';


-- These lines need to be last.  Insert any changes above.
UPDATE VERSION SET SCHEMA_VERSION='3.0.0.9', VERSION_COMMENT='Hive release version 3.0.0.9' where VER_ID=1;
SELECT 'Finished upgrading MetaStore schema from 3.0.0.8 to 3.0.0.9' AS 'msg';
