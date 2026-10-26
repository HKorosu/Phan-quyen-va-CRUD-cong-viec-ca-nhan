package com.fptaptech.taskmanager.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Size(min = 3, max = 100, message = "Tên đăng nhập phải dài từ 3 đến 100 ký tự")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
    private String username = "";

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 8, max = 72, message = "Mật khẩu phải dài từ 8 đến 72 ký tự")
    private String password = "";

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
