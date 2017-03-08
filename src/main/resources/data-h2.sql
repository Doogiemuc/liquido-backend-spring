-- Data script for H2 in memory DB.
-- Any SQL in this file will be executed after the applicatin has started.

-- tweak some created_at dates    => not necessary, solved with plain SQL in TestDataCreator
--UPDATE AREAS SET created_at = DATEADD('MONTH', -1, NOW()) WHERE title='Area 0'
