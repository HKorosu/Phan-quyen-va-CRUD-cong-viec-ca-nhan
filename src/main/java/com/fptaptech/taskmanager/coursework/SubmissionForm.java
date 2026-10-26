package com.fptaptech.taskmanager.coursework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmissionForm {
    @NotBlank(message = "Nội dung bài nộp không được để trống")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    private String content = "";

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
