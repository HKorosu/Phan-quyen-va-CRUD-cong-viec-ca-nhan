package com.fptaptech.taskmanager.coursework;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record AssignmentView(Long id, String title, String description, String deadline,
                             AssignmentStatus status, String lecturerUsername) {
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public static AssignmentView from(Assignment assignment) {
        return new AssignmentView(assignment.getId(), assignment.getTitle(), assignment.getDescription(),
                DISPLAY_TIME.format(assignment.getDeadline()), assignment.getStatus(),
                assignment.getLecturer().getUsername());
    }
}
