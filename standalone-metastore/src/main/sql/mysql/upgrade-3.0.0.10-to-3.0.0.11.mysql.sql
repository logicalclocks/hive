SELECT 'Upgrading MetaStore schema from 3.0.0.10 to 3.0.0.11' AS 'msg';


-- These lines need to be last.  Insert any changes above.
UPDATE VERSION SET SCHEMA_VERSION='3.0.0.11', VERSION_COMMENT='Hive release version 3.0.0.11' where VER_ID=1;
SELECT 'Finished upgrading MetaStore schema from 3.0.0.10 to 3.0.0.11' AS 'msg';
