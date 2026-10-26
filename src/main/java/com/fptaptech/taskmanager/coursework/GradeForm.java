package com.fptaptech.taskmanager.coursework;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class GradeForm {
    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0.00", message = "Điểm thấp nhất là 0")
    @DecimalMax(value = "10.00", message = "Điểm cao nhất là 10")
    @Digits(integer = 2, fraction = 2, message = "Điểm có tối đa 2 chữ số thập phân")
    private BigDecimal score;

    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    private String feedback = "";

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
