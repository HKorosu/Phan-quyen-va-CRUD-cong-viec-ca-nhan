package com.fptaptech.taskmanager.coursework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class AssignmentForm {
    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    private String title = "";

    @NotBlank(message = "Vui lòng nhập mô tả")
    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description = "";

    @NotNull(message = "Vui lòng chọn hạn nộp")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadline;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}
