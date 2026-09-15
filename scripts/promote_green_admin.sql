-- 개발 환경 전용: 일반 회원가입을 마친 계정을 GREEN 관리자로 승급합니다.
-- 비밀번호나 RDS 접속 정보는 이 파일에 기록하지 않습니다.
--
-- 1) 웹에서 관리자용 이메일로 회원가입합니다.
-- 2) 아래 @admin_email 값을 해당 이메일로 바꿉니다.
-- 3) AWS 담당자 또는 DB 권한 보유자가 dev RDS에서 실행합니다.

SET @admin_email = 'replace-with-admin-email@example.com';

START TRANSACTION;

UPDATE members
SET grade = 'GREEN'
WHERE email = @admin_email;

SELECT id, email, name, grade
FROM members
WHERE email = @admin_email;

COMMIT;
