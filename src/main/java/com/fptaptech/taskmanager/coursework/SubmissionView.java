package com.fptaptech.taskmanager.coursework;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record SubmissionView(Long id, Long assignmentId, String assignmentTitle, String studentUsername,
                             String content, String submittedAt, BigDecimal score, String feedback) {
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    static SubmissionView from(Submission submission) {
        return new SubmissionView(submission.getId(), submission.getAssignment().getId(),
                submission.getAssignment().getTitle(), submission.getStudent().getUsername(),
                submission.getContent(), DISPLAY_TIME.format(submission.getSubmittedAt()),
                submission.getScore(), submission.getFeedback());
    }
}
