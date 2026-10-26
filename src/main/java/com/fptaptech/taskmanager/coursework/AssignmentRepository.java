package com.fptaptech.taskmanager.coursework;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Page<Assignment> findByLecturer_Username(String username, Pageable pageable);

    Page<Assignment> findByLecturer_UsernameAndStatus(String username, AssignmentStatus status, Pageable pageable);

    Optional<Assignment> findByIdAndLecturer_Username(Long id, String username);

    Page<Assignment> findDistinctByAssignedStudents_Student_UsernameAndStatus(
            String username, AssignmentStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "lecturer")
    Page<Assignment> findAll(Pageable pageable);

    long countByStatus(AssignmentStatus status);
}
