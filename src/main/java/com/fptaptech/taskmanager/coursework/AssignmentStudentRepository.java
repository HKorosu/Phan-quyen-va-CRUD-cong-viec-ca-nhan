package com.fptaptech.taskmanager.coursework;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentStudentRepository extends JpaRepository<AssignmentStudent, AssignmentStudentId> {
    boolean existsByAssignment_IdAndStudent_Username(Long assignmentId, String username);

    List<AssignmentStudent> findByAssignment_Id(Long assignmentId);
}
