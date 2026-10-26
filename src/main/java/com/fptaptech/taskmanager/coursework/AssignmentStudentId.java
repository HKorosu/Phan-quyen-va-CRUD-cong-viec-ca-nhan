package com.fptaptech.taskmanager.coursework;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentStudentId implements Serializable {
    private Long assignment;
    private Long student;

    public AssignmentStudentId() {
    }

    public AssignmentStudentId(Long assignment, Long student) {
        this.assignment = assignment;
        this.student = student;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssignmentStudentId that)) {
            return false;
        }
        return Objects.equals(assignment, that.assignment) && Objects.equals(student, that.student);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignment, student);
    }
}
