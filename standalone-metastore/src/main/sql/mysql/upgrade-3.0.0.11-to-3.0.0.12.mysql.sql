SELECT 'Upgrading MetaStore schema from 3.0.0.11 to 3.0.0.12' AS 'msg';


-- These lines need to be last.  Insert any changes above.
UPDATE VERSION SET SCHEMA_VERSION='3.0.0.12', VERSION_COMMENT='Hive release version 3.0.0.12' where VER_ID=1;
SELECT 'Finished upgrading MetaStore schema from 3.0.0.11 to 3.0.0.12' AS 'msg';
