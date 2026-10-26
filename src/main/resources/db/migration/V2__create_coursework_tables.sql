INSERT INTO users_roles (user_id, role_id)
SELECT ur.user_id, student.id
FROM users_roles ur
JOIN roles old_role ON old_role.id = ur.role_id AND old_role.name = 'ROLE_USER'
JOIN roles student ON student.name = 'ROLE_STUDENT'
WHERE NOT EXISTS (
    SELECT 1 FROM users_roles existing
    WHERE existing.user_id = ur.user_id AND existing.role_id = student.id
);

DELETE FROM users_roles
WHERE role_id IN (SELECT id FROM roles WHERE name = 'ROLE_USER');
DELETE FROM roles WHERE name = 'ROLE_USER';

CREATE TABLE assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    deadline TIMESTAMP(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    lecturer_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_assignments_lecturer FOREIGN KEY (lecturer_id) REFERENCES users (id),
    CONSTRAINT ck_assignments_status CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED'))
) ENGINE=InnoDB;

CREATE INDEX idx_assignments_lecturer_status ON assignments (lecturer_id, status);

CREATE TABLE assignment_students (
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (assignment_id, student_id),
    CONSTRAINT fk_assignment_students_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_assignment_students_student FOREIGN KEY (student_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE submissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content VARCHAR(5000) NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL,
    score DECIMAL(4,2) NULL,
    feedback VARCHAR(2000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_submissions_assignment_student UNIQUE (assignment_id, student_id),
    CONSTRAINT fk_submissions_assignment_student FOREIGN KEY (assignment_id, student_id)
        REFERENCES assignment_students (assignment_id, student_id),
    CONSTRAINT ck_submissions_score CHECK (score IS NULL OR score BETWEEN 0 AND 10)
) ENGINE=InnoDB;

CREATE INDEX idx_submissions_student ON submissions (student_id, submitted_at);
