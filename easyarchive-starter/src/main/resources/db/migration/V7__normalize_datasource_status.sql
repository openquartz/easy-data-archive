UPDATE ea_archive_datasource
SET status = 2
WHERE status IN (0, 3);
