package com.fptaptech.taskmanager.coursework;

import com.fptaptech.taskmanager.user.AppUser;
import com.fptaptech.taskmanager.user.Role;
import com.fptaptech.taskmanager.user.RoleRepository;
import com.fptaptech.taskmanager.user.UserRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class AssignmentService {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AssignmentRepository assignments;
    private final AssignmentStudentRepository assignmentStudents;
    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final RoleRepository roles;
    private final Clock clock;

    public AssignmentService(AssignmentRepository assignments, AssignmentStudentRepository assignmentStudents,
                             SubmissionRepository submissions, UserRepository users, RoleRepository roles, Clock clock) {
        this.assignments = assignments;
        this.assignmentStudents = assignmentStudents;
        this.submissions = submissions;
        this.users = users;
        this.roles = roles;
        this.clock = clock;
    }

    private String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Pageable pageRequest(int page) {
        return PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Order.desc("id")));
    }

    private Instant deadline(AssignmentForm form) {
        return form.getDeadline().atZone(DISPLAY_ZONE).toInstant();
    }

    private AppUser currentUser() {
        return users.findByUsername(username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Assignment ownedAssignment(Long id) {
        return assignments.findByIdAndLecturer_Username(id, username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PreAuthorize("hasRole('LECTURER')")
    public Page<AssignmentView> lecturerAssignments(int page, AssignmentStatus status) {
        Page<Assignment> result = status == null
                ? assignments.findByLecturer_Username(username(), pageRequest(page))
                : assignments.findByLecturer_UsernameAndStatus(username(), status, pageRequest(page));
        return result.map(AssignmentView::from);
    }

    @PreAuthorize("hasRole('LECTURER')")
    public AssignmentForm editForm(Long id) {
        Assignment assignment = ownedAssignment(id);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ sửa được bài tập ở trạng thái DRAFT");
        }
        AssignmentForm form = new AssignmentForm();
        form.setTitle(assignment.getTitle());
        form.setDescription(assignment.getDescription());
        form.setDeadline(assignment.getDeadline().atZone(DISPLAY_ZONE).toLocalDateTime());
        return form;
    }

    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public Long create(@Valid AssignmentForm form) {
        Instant deadline = deadline(form);
        if (!deadline.isAfter(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn nộp phải ở tương lai");
        }
        Assignment assignment = new Assignment(currentUser(), form, deadline);
        return assignments.save(assignment).getId();
    }

    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void update(Long id, @Valid AssignmentForm form) {
        Assignment assignment = ownedAssignment(id);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ sửa được bài tập ở trạng thái DRAFT");
        }
        Instant deadline = deadline(form);
        if (!deadline.isAfter(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn nộp phải ở tương lai");
        }
        assignment.apply(form, deadline);
    }

    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void assignStudents(Long id, List<String> requestedUsernames) {
        Assignment assignment = ownedAssignment(id);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ giao được bài tập ở trạng thái DRAFT");
        }
        Role studentRole = roles.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT is missing from the database"));
        List<AppUser> students = requestedUsernames.stream()
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> users.findByUsername(name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Không tìm thấy học viên: " + name)))
                .toList();
        if (students.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần chọn ít nhất một học viên");
        }
        if (students.stream().anyMatch(student -> !student.getRoles().contains(studentRole))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể giao bài cho tài khoản STUDENT");
        }
        for (AppUser student : students) {
            if (!assignmentStudents.existsByAssignment_IdAndStudent_Username(id, student.getUsername())) {
                assignmentStudents.save(new AssignmentStudent(assignment, student));
            }
        }
    }

    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void open(Long id) {
        Assignment assignment = ownedAssignment(id);
        if (!assignment.getDeadline().isAfter(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể mở bài đã quá hạn");
        }
        if (assignmentStudents.findByAssignment_Id(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hãy giao bài cho học viên trước khi mở");
        }
        try {
            assignment.open();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void close(Long id) {
        Assignment assignment = ownedAssignment(id);
        try {
            assignment.close();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @PreAuthorize("hasRole('LECTURER')")
    public Page<SubmissionView> submissions(Long assignmentId, int page) {
        ownedAssignment(assignmentId);
        return submissions.findByAssignment_IdAndAssignment_Lecturer_Username(
                        assignmentId, username(), pageRequest(page))
                .map(SubmissionView::from);
    }

    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void grade(Long assignmentId, Long submissionId, @Valid GradeForm form) {
        Assignment assignment = ownedAssignment(assignmentId);
        if (assignment.getStatus() != AssignmentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ chấm điểm sau khi đóng bài");
        }
        Submission submission = submissions
                .findByIdAndAssignment_IdAndAssignment_Lecturer_Username(submissionId, assignmentId, username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (submission.getScore() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài nộp này đã được chấm");
        }
        submission.grade(form.getScore(), form.getFeedback());
    }

    @PreAuthorize("hasRole('STUDENT')")
    public Page<AssignmentView> openAssignments(int page) {
        return assignments.findDistinctByAssignedStudents_Student_UsernameAndStatus(
                        username(), AssignmentStatus.OPEN, pageRequest(page))
                .map(AssignmentView::from);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionForm submissionForm(Long assignmentId) {
        Assignment assignment = submitEligibleAssignment(assignmentId);
        SubmissionForm form = new SubmissionForm();
        submissions.findByAssignment_IdAndStudent_Username(assignmentId, username())
                .ifPresent(submission -> form.setContent(submission.getContent()));
        return form;
    }

    private Assignment submitEligibleAssignment(Long assignmentId) {
        if (!assignmentStudents.existsByAssignment_IdAndStudent_Username(assignmentId, username())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (assignment.getStatus() != AssignmentStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài tập đã đóng");
        }
        if (Instant.now(clock).isAfter(assignment.getDeadline())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã quá hạn nộp bài");
        }
        return assignment;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public void submit(Long assignmentId, @Valid SubmissionForm form) {
        Assignment assignment = submitEligibleAssignment(assignmentId);
        AppUser student = currentUser();
        Instant submittedAt = Instant.now(clock);
        Submission submission = submissions.findByAssignment_IdAndStudent_Username(assignmentId, username())
                .orElseGet(() -> new Submission(assignment, student, form.getContent(), submittedAt));
        submission.updateContent(form.getContent(), submittedAt);
        submissions.save(submission);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public Page<SubmissionView> myResults(int page) {
        return submissions.findByStudent_Username(username(), pageRequest(page)).map(SubmissionView::from);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionView myResult(Long id) {
        Submission submission = submissions.findByIdAndStudent_Username(id, username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return SubmissionView.from(submission);
    }
}
