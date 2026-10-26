-- Register these six accounts through /register first:
-- admin, lecturer1, lecturer2, student1, student2, student3.
-- This trusted local-only script assigns roles; public registration cannot do so.

DELETE ur FROM users_roles ur
JOIN users u ON u.id = ur.user_id
WHERE u.username IN ('admin', 'lecturer1', 'lecturer2', 'student1', 'student2', 'student3');

INSERT IGNORE INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE (u.username = 'admin' AND r.name = 'ROLE_ADMIN')
   OR (u.username IN ('lecturer1', 'lecturer2') AND r.name = 'ROLE_LECTURER')
   OR (u.username IN ('student1', 'student2', 'student3') AND r.name = 'ROLE_STUDENT');

INSERT INTO assignments (title, description, deadline, status, lecturer_id)
SELECT 'Bản nháp: thiết kế cơ sở dữ liệu', 'Bài mẫu trạng thái DRAFT', DATE_ADD(UTC_TIMESTAMP(), INTERVAL 14 DAY),
       'DRAFT', u.id
FROM users u
WHERE u.username = 'lecturer1'
  AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.title = 'Bản nháp: thiết kế cơ sở dữ liệu');

INSERT INTO assignments (title, description, deadline, status, lecturer_id)
SELECT 'Bài tập đang mở: Spring MVC', 'Bài mẫu OPEN, hạn nộp trong tương lai', DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 DAY),
       'OPEN', u.id
FROM users u
WHERE u.username = 'lecturer1'
  AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.title = 'Bài tập đang mở: Spring MVC');

INSERT INTO assignments (title, description, deadline, status, lecturer_id)
SELECT 'Bài tập đã đóng: JPA', 'Bài mẫu CLOSED đã chấm điểm', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 DAY),
       'CLOSED', u.id
FROM users u
WHERE u.username = 'lecturer2'
  AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.title = 'Bài tập đã đóng: JPA');

INSERT IGNORE INTO assignment_students (assignment_id, student_id)
SELECT a.id, u.id FROM assignments a CROSS JOIN users u
WHERE a.title = 'Bài tập đang mở: Spring MVC' AND u.username IN ('student1', 'student2');

INSERT IGNORE INTO assignment_students (assignment_id, student_id)
SELECT a.id, u.id FROM assignments a CROSS JOIN users u
WHERE a.title = 'Bài tập đã đóng: JPA' AND u.username IN ('student2', 'student3');

INSERT INTO submissions (assignment_id, student_id, content, submitted_at, score, feedback)
SELECT a.id, u.id, 'Bài nộp demo; chỉ dùng nội dung văn bản.', UTC_TIMESTAMP(), 8.50, 'Trình bày tốt.'
FROM assignments a CROSS JOIN users u
WHERE a.title = 'Bài tập đã đóng: JPA' AND u.username = 'student2'
  AND NOT EXISTS (
      SELECT 1 FROM submissions s WHERE s.assignment_id = a.id AND s.student_id = u.id
  );
