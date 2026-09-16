-- 로그인 실패/잠금 이력을 남기기 위한 테이블을 새로 만든다.
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '30s';

CREATE TABLE public.tb_login_history (
    history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hosp_div_id VARCHAR NOT NULL,
    emp_id VARCHAR NOT NULL,
    event_cd VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_login_history_event CHECK (event_cd IN ('FAIL', 'LOCKED'))
);

CREATE INDEX ix_login_history_emp
    ON public.tb_login_history (hosp_div_id, emp_id, created_at DESC);

COMMENT ON TABLE public.tb_login_history IS
    '로그인 실패(FAIL) 및 잠금 발생(LOCKED) 이력';
COMMENT ON COLUMN public.tb_login_history.event_cd IS
    '이벤트 종류: FAIL(로그인 실패), LOCKED(연속 실패로 계정 잠금 발생)';
COMMIT;
