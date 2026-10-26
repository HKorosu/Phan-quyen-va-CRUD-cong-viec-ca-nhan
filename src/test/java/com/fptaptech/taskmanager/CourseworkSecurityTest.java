package com.fptaptech.taskmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fptaptech.taskmanager.coursework.Assignment;
import com.fptaptech.taskmanager.coursework.AssignmentForm;
import com.fptaptech.taskmanager.coursework.AssignmentRepository;
import com.fptaptech.taskmanager.coursework.AssignmentStatus;
import com.fptaptech.taskmanager.coursework.AssignmentStudent;
import com.fptaptech.taskmanager.coursework.AssignmentStudentRepository;
import com.fptaptech.taskmanager.coursework.Submission;
import com.fptaptech.taskmanager.coursework.SubmissionRepository;
import com.fptaptech.taskmanager.user.AppUser;
import com.fptaptech.taskmanager.user.Role;
import com.fptaptech.taskmanager.user.RoleRepository;
import com.fptaptech.taskmanager.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseworkSecurityTest {
    private static final Instant NOW = Instant.parse("2026-10-26T00:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired AssignmentRepository assignments;
    @Autowired AssignmentStudentRepository assignmentStudents;
    @Autowired SubmissionRepository submissions;

    AppUser studentA;
    AppUser studentB;
    AppUser lecturerA;
    AppUser lecturerB;
    AppUser admin;
    Assignment openForA;
    Assignment openForB;
    Assignment closedForA;
    Submission submissionB;

    @BeforeEach
    void setUp() {
        studentA = userWithRole("student_a", "ROLE_STUDENT");
        studentB = userWithRole("student_b", "ROLE_STUDENT");
        lecturerA = userWithRole("lecturer_a", "ROLE_LECTURER");
        lecturerB = userWithRole("lecturer_b", "ROLE_LECTURER");
        admin = userWithRole("admin", "ROLE_ADMIN");
        openForA = assignment("Open for A", lecturerA, AssignmentStatus.OPEN, NOW.plusSeconds(3600));
        assign(openForA, studentA);
        openForB = assignment("Open for B", lecturerB, AssignmentStatus.OPEN, NOW.plusSeconds(3600));
        assign(openForB, studentB);
        closedForA = assignment("Closed for A", lecturerA, AssignmentStatus.CLOSED, NOW.plusSeconds(3600));
        assign(closedForA, studentA);
        submissionB = submissions.saveAndFlush(new Submission(openForB, studentB, "Bob's answer", NOW));
    }

    private AppUser userWithRole(String username, String roleName) {
        AppUser user = new AppUser(username, "{noop}unused-password");
        Role role = roles.findByName(roleName).orElseThrow();
        user.getRoles().add(role);
        return users.saveAndFlush(user);
    }

    private Assignment assignment(String title, AppUser lecturer, AssignmentStatus status, Instant deadline) {
        AssignmentForm form = new AssignmentForm();
        form.setTitle(title);
        form.setDescription("Description");
        Assignment assignment = new Assignment(lecturer, form, deadline);
        if (status == AssignmentStatus.OPEN) {
            assignment.open();
        } else if (status == AssignmentStatus.CLOSED) {
            assignment.open();
            assignment.close();
        }
        return assignments.saveAndFlush(assignment);
    }

    private void assign(Assignment assignment, AppUser student) {
        assignmentStudents.saveAndFlush(new AssignmentStudent(assignment, student));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submissionPost(
            Long assignmentId, String username) {
        return post("/student/assignments/{id}/submit", assignmentId)
                .with(user(username).roles("STUDENT"))
                .with(csrf())
                .param("content", "Answer");
    }

    @Test
    void anonymousMustAuthenticateAndRoleRoutesAreRestricted() throws Exception {
        mvc.perform(get("/student/assignments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/lecturer/assignments").with(user("student_a").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("lecturer_a").roles("LECTURER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotReadOrOverwriteAnotherStudentsSubmission() throws Exception {
        mvc.perform(get("/student/results/{id}", submissionB.getId())
                        .with(user("student_a").roles("STUDENT")))
                .andExpect(status().isNotFound());
        mvc.perform(submissionPost(openForB.getId(), "student_a"))
                .andExpect(status().isNotFound());
        assertEquals(1, submissions.count());
        assertEquals("Bob's answer", submissions.findById(submissionB.getId()).orElseThrow().getContent());
    }

    @Test
    void lecturerCannotEditOrGradeAnotherLecturersAssignment() throws Exception {
        mvc.perform(post("/lecturer/assignments/{id}", openForB.getId())
                        .with(user("lecturer_a").roles("LECTURER"))
                        .with(csrf())
                        .param("title", "Taken over")
                        .param("description", "not allowed")
                        .param("deadline", "2026-10-27T12:00"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/lecturer/assignments/{aid}/submissions/{sid}/grade",
                                openForB.getId(), submissionB.getId())
                        .with(user("lecturer_a").roles("LECTURER"))
                        .with(csrf())
                        .param("score", "9")
                        .param("feedback", "No"))
                .andExpect(status().isNotFound());
        assertEquals(null, submissions.findById(submissionB.getId()).orElseThrow().getScore());
    }

    @Test
    void unassignedStudentCannotSubmitEvenWithKnownAssignmentId() throws Exception {
        mvc.perform(submissionPost(closedForA.getId(), "student_b"))
                .andExpect(status().isNotFound());
        assertEquals(1, submissions.count());
    }

    @Test
    void closedOrExpiredAssignmentsRejectButDeadlineInstantIsAccepted() throws Exception {
        mvc.perform(submissionPost(closedForA.getId(), "student_a"))
                .andExpect(status().isConflict());
        Assignment expired = assignment("Expired", lecturerA, AssignmentStatus.OPEN, NOW.minusSeconds(1));
        assign(expired, studentA);
        mvc.perform(submissionPost(expired.getId(), "student_a"))
                .andExpect(status().isConflict());
        Assignment dueNow = assignment("Due now", lecturerA, AssignmentStatus.OPEN, NOW);
        assign(dueNow, studentA);
        mvc.perform(submissionPost(dueNow.getId(), "student_a"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/results"));
        assertTrue(submissions.findByAssignment_IdAndStudent_Username(dueNow.getId(), "student_a").isPresent());
    }

    @Test
    void csrfIsRequiredForEveryWrite() throws Exception {
        mvc.perform(post("/student/assignments/{id}/submit", openForA.getId())
                        .with(user("student_a").roles("STUDENT"))
                        .param("content", "Answer"))
                .andExpect(status().isForbidden());
        assertTrue(submissions.findByAssignment_IdAndStudent_Username(openForA.getId(), "student_a").isEmpty());
    }

    @Test
    void resubmissionUpdatesTheSameRecordAndIgnoresClientStudentId() throws Exception {
        mvc.perform(submissionPost(openForA.getId(), "student_a").param("studentId", studentB.getId().toString()))
                .andExpect(status().is3xxRedirection());
        Long firstId = submissions.findByAssignment_IdAndStudent_Username(openForA.getId(), "student_a")
                .orElseThrow().getId();
        mvc.perform(post("/student/assignments/{id}/submit", openForA.getId())
                        .with(user("student_a").roles("STUDENT"))
                        .with(csrf())
                        .param("content", "Updated answer")
                        .param("studentId", studentB.getId().toString()))
                .andExpect(status().is3xxRedirection());
        var saved = submissions.findByAssignment_IdAndStudent_Username(openForA.getId(), "student_a").orElseThrow();
        assertEquals(firstId, saved.getId());
        assertEquals(studentA.getId(), saved.getStudent().getId());
        assertEquals("Updated answer", saved.getContent());
        assertEquals(2, submissions.count());
    }

    @Test
    void invalidScoresAreRejectedAndNeverPersisted() throws Exception {
        Submission ownedSubmission = submissions.saveAndFlush(
                new Submission(closedForA, studentA, "Answer to grade", NOW));
        for (String score : Set.of("-0.01", "10.01", "1.234")) {
            mvc.perform(post("/lecturer/assignments/{aid}/submissions/{sid}/grade",
                            closedForA.getId(), ownedSubmission.getId())
                            .with(user("lecturer_a").roles("LECTURER"))
                            .with(csrf())
                            .param("score", score)
                            .param("feedback", "Review"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("grade", "score"));
            assertEquals(null, submissions.findById(ownedSubmission.getId()).orElseThrow().getScore());
        }
    }

    @Test
    void lecturerCanGradeClosedAssignmentOnlyOnce() throws Exception {
        Submission ownedSubmission = submissions.saveAndFlush(
                new Submission(closedForA, studentA, "Answer to grade", NOW));
        mvc.perform(post("/lecturer/assignments/{aid}/submissions/{sid}/grade",
                        closedForA.getId(), ownedSubmission.getId())
                        .with(user("lecturer_a").roles("LECTURER"))
                        .with(csrf())
                        .param("score", "8.50")
                        .param("feedback", "Tốt"))
                .andExpect(status().is3xxRedirection());
        assertEquals(0, submissions.findById(ownedSubmission.getId()).orElseThrow()
                .getScore().compareTo(new java.math.BigDecimal("8.50")));
        mvc.perform(post("/lecturer/assignments/{aid}/submissions/{sid}/grade",
                        closedForA.getId(), ownedSubmission.getId())
                        .with(user("lecturer_a").roles("LECTURER"))
                        .with(csrf())
                        .param("score", "9")
                        .param("feedback", "Second attempt"))
                .andExpect(status().isConflict());
    }

    @Test
    void assignmentCreationRequiresFutureDeadline() throws Exception {
        mvc.perform(post("/lecturer/assignments")
                        .with(user("lecturer_a").roles("LECTURER"))
                        .with(csrf())
                        .param("title", "Past deadline")
                        .param("description", "Should not persist")
                        .param("deadline", "2026-10-25T12:00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "deadline"));
        assertEquals(3, assignments.count());
    }

    @Test
    void studentAndAdminCannotInvokeGradingEndpoint() throws Exception {
        for (String username : Set.of("student_a", "admin")) {
            String role = username.equals("admin") ? "ADMIN" : "STUDENT";
            mvc.perform(post("/lecturer/assignments/{aid}/submissions/{sid}/grade",
                            closedForA.getId(), submissionB.getId())
                            .with(user(username).roles(role))
                            .with(csrf())
                            .param("score", "8")
                            .param("feedback", "Review"))
                    .andExpect(status().isForbidden());
        }
        assertEquals(null, submissions.findById(submissionB.getId()).orElseThrow().getScore());
    }

    @Test
    void userSuppliedHtmlIsRenderedAsText() throws Exception {
        Assignment scriptTitle = assignment("<script>alert(1)</script>", lecturerA, AssignmentStatus.DRAFT,
                NOW.plusSeconds(86400));
        mvc.perform(get("/lecturer/assignments").with(user("lecturer_a").roles("LECTURER")))
                .andExpect(status().isOk())
                .andExpect(view().name("lecturer/assignments"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
        assertTrue(assignments.existsById(scriptTitle.getId()));
    }

    @Test
    void authenticatedRolePagesRenderTheirThymeleafViews() throws Exception {
        mvc.perform(get("/student/assignments").with(user("student_a").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/assignments"));
        mvc.perform(get("/student/assignments/{id}/submit", openForA.getId())
                        .with(user("student_a").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/submission-form"));
        mvc.perform(get("/student/results/{id}", submissionB.getId())
                        .with(user("student_b").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/result"));
        mvc.perform(get("/lecturer/assignments/new").with(user("lecturer_a").roles("LECTURER")))
                .andExpect(status().isOk())
                .andExpect(view().name("lecturer/assignment-form"));
        mvc.perform(get("/lecturer/assignments/{id}/submissions", closedForA.getId())
                        .with(user("lecturer_a").roles("LECTURER")))
                .andExpect(status().isOk())
                .andExpect(view().name("lecturer/submissions"));
        mvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    void publicRegistrationAlwaysCreatesStudentEvenIfRequestIncludesAdminRole() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "new_student")
                        .param("password", "strong-password-123")
                        .param("roles", "ROLE_ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
        AppUser registered = users.findByUsername("new_student").orElseThrow();
        assertEquals(Set.of("ROLE_STUDENT"), registered.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
        assertTrue(registered.getPassword().startsWith("{bcrypt}"));
    }
}
