package com.fptaptech.taskmanager.coursework;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByAssignment_IdAndStudent_Username(Long assignmentId, String username);

    Optional<Submission> findByIdAndStudent_Username(Long id, String username);

    Optional<Submission> findByIdAndStudent_UsernameAndAssignment_Status(
            Long id, String username, AssignmentStatus status);

    Optional<Submission> findByIdAndAssignment_IdAndAssignment_Lecturer_Username(
            Long id, Long assignmentId, String lecturerUsername);

    Page<Submission> findByAssignment_IdAndAssignment_Lecturer_Username(
            Long assignmentId, String lecturerUsername, Pageable pageable);

    Page<Submission> findByStudent_Username(String username, Pageable pageable);

    long countByScoreIsNotNull();
}
