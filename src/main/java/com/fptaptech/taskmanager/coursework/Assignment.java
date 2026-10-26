package com.fptaptech.taskmanager.coursework;

import com.fptaptech.taskmanager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false, updatable = false)
    private AppUser lecturer;

    @OneToMany(mappedBy = "assignment")
    private Set<AssignmentStudent> assignedStudents = new HashSet<>();

    protected Assignment() {
    }

    public Assignment(AppUser lecturer, AssignmentForm form, Instant deadline) {
        this.lecturer = lecturer;
        apply(form, deadline);
    }

    public void apply(AssignmentForm form, Instant deadline) {
        this.title = form.getTitle().strip();
        this.description = form.getDescription().strip();
        this.deadline = deadline;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public AppUser getLecturer() {
        return lecturer;
    }

    public void open() {
        if (status != AssignmentStatus.DRAFT) {
            throw new IllegalStateException("Only draft assignments can be opened");
        }
        status = AssignmentStatus.OPEN;
    }

    public void close() {
        if (status != AssignmentStatus.OPEN) {
            throw new IllegalStateException("Only open assignments can be closed");
        }
        status = AssignmentStatus.CLOSED;
    }
}
