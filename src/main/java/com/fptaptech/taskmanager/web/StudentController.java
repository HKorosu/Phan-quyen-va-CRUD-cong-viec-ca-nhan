package com.fptaptech.taskmanager.web;

import com.fptaptech.taskmanager.coursework.AssignmentService;
import com.fptaptech.taskmanager.coursework.SubmissionForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/student")
public class StudentController {
    private final AssignmentService service;

    public StudentController(AssignmentService service) {
        this.service = service;
    }

    @InitBinder("form")
    void submissionFields(WebDataBinder binder) {
        binder.setAllowedFields("content");
    }

    @GetMapping("/assignments")
    public String assignments(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("assignments", service.openAssignments(page));
        return "student/assignments";
    }

    @GetMapping("/assignments/{id}/submit")
    public String submissionForm(@PathVariable Long id, Model model) {
        model.addAttribute("assignmentId", id);
        model.addAttribute("form", service.submissionForm(id));
        return "student/submission-form";
    }

    @PostMapping("/assignments/{id}/submit")
    public String submit(@PathVariable Long id, @Valid @ModelAttribute("form") SubmissionForm form,
                         BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("assignmentId", id);
            return "student/submission-form";
        }
        service.submit(id, form);
        return "redirect:/student/results";
    }

    @GetMapping("/results")
    public String results(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("results", service.myResults(page));
        return "student/results";
    }

    @GetMapping("/results/{id}")
    public String result(@PathVariable Long id, Model model) {
        model.addAttribute("result", service.myResult(id));
        return "student/result";
    }
}
