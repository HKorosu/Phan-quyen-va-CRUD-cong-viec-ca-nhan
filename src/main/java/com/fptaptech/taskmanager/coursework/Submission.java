package com.fptaptech.taskmanager.coursework;

import com.fptaptech.taskmanager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "submissions", uniqueConstraints =
        @UniqueConstraint(name = "uk_submissions_assignment_student", columnNames = {"assignment_id", "student_id"}))
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(length = 2000)
    private String feedback;

    protected Submission() {
    }

    public Submission(Assignment assignment, AppUser student, String content, Instant submittedAt) {
        this.assignment = assignment;
        this.student = student;
        updateContent(content, submittedAt);
    }

    public void updateContent(String content, Instant submittedAt) {
        this.content = content.strip();
        this.submittedAt = submittedAt;
    }

    public void grade(BigDecimal score, String feedback) {
        if (this.score != null) {
            throw new IllegalStateException("A submission can only be graded once");
        }
        this.score = score;
        this.feedback = feedback == null ? "" : feedback.strip();
    }

    public Long getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public AppUser getStudent() {
        return student;
    }

    public String getContent() {
        return content;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}
