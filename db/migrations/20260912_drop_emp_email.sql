-- User-requested removal of the unused email column.
-- RESTRICT (the default) prevents removal of dependent views or other objects.
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE tb_emp DROP COLUMN IF EXISTS emp_email;
COMMIT;
