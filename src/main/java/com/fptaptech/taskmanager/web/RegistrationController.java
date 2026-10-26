package com.fptaptech.taskmanager.web;

import com.fptaptech.taskmanager.user.RegistrationForm;
import com.fptaptech.taskmanager.user.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {
    private final RegistrationService registration;

    public RegistrationController(RegistrationService registration) {
        this.registration = registration;
    }

    @InitBinder("form")
    void registrationFields(WebDataBinder binder) {
        binder.setAllowedFields("username", "password");
    }

    @GetMapping("/register")
    public String form(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult errors) {
        if (!errors.hasErrors() && !registration.register(form)) {
            errors.rejectValue("username", "user.username.taken", "Tên đăng nhập đã được sử dụng");
        }
        return errors.hasErrors() ? "register" : "redirect:/login?registered";
    }
}
