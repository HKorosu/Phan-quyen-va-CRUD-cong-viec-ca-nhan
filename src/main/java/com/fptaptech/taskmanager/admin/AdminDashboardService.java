package com.fptaptech.taskmanager.admin;

import com.fptaptech.taskmanager.coursework.AssignmentRepository;
import com.fptaptech.taskmanager.coursework.AssignmentView;
import com.fptaptech.taskmanager.coursework.SubmissionRepository;
import com.fptaptech.taskmanager.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardService {
    private final UserRepository users;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;

    public AdminDashboardService(UserRepository users, AssignmentRepository assignments,
                                 SubmissionRepository submissions) {
        this.users = users;
        this.assignments = assignments;
        this.submissions = submissions;
    }

    public DashboardView dashboard(int page) {
        var pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Order.desc("id")));
        List<UserView> userViews = users.findAllByOrderByUsernameAsc().stream()
                .map(user -> new UserView(user.getUsername(),
                        user.getRoles().stream().map(role -> role.getName()).sorted().toList()))
                .toList();
        Page<AssignmentView> assignmentViews = assignments.findAll(pageable).map(AssignmentView::from);
        return new DashboardView(users.count(), assignments.count(), submissions.count(),
                submissions.countByScoreIsNotNull(), userViews, assignmentViews);
    }

    public record UserView(String username, List<String> roles) {
    }

    public record DashboardView(long userCount, long assignmentCount, long submissionCount,
                                long gradedCount, List<UserView> users, Page<AssignmentView> assignments) {
    }
}
