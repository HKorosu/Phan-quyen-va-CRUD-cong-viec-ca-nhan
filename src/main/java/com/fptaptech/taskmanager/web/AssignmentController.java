package com.fptaptech.taskmanager.web;

import com.fptaptech.taskmanager.coursework.AssignmentForm;
import com.fptaptech.taskmanager.coursework.AssignmentService;
import com.fptaptech.taskmanager.coursework.AssignmentStatus;
import com.fptaptech.taskmanager.coursework.GradeForm;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
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
@RequestMapping("/lecturer/assignments")
public class AssignmentController {
    private final AssignmentService service;
    private final Clock clock;

    public AssignmentController(AssignmentService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @InitBinder("form")
    void assignmentFields(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "deadline");
    }

    @InitBinder("grade")
    void gradeFields(WebDataBinder binder) {
        binder.setAllowedFields("score", "feedback");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) AssignmentStatus status, Model model) {
        model.addAttribute("assignments", service.lecturerAssignments(page, status));
        model.addAttribute("statuses", AssignmentStatus.values());
        model.addAttribute("selectedStatus", status);
        return "lecturer/assignments";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        AssignmentForm form = new AssignmentForm();
        form.setDeadline(java.time.LocalDateTime.now(clock).plusDays(7).withSecond(0).withNano(0));
        model.addAttribute("form", form);
        model.addAttribute("assignmentId", null);
        model.addAttribute("actionUrl", "/lecturer/assignments");
        return "lecturer/assignment-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AssignmentForm form,
                         BindingResult errors, Model model) {
        checkFutureDeadline(form, errors);
        if (errors.hasErrors()) {
            model.addAttribute("assignmentId", null);
            model.addAttribute("actionUrl", "/lecturer/assignments");
            return "lecturer/assignment-form";
        }
        service.create(form);
        return "redirect:/lecturer/assignments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", service.editForm(id));
        model.addAttribute("assignmentId", id);
        model.addAttribute("actionUrl", "/lecturer/assignments/" + id);
        return "lecturer/assignment-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") AssignmentForm form,
                         BindingResult errors, Model model) {
        checkFutureDeadline(form, errors);
        if (errors.hasErrors()) {
            model.addAttribute("assignmentId", id);
            model.addAttribute("actionUrl", "/lecturer/assignments/" + id);
            return "lecturer/assignment-form";
        }
        service.update(id, form);
        return "redirect:/lecturer/assignments";
    }

    private void checkFutureDeadline(AssignmentForm form, BindingResult errors) {
        if (!errors.hasFieldErrors("deadline") && form.getDeadline() != null
                && !form.getDeadline().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().isAfter(clock.instant())) {
            errors.rejectValue("deadline", "assignment.deadline.future", "Hạn nộp phải ở tương lai");
        }
    }

    @PostMapping("/{id}/students")
    public String assignStudents(@PathVariable Long id, @RequestParam("usernames") String usernames) {
        service.assignStudents(id, java.util.Arrays.asList(usernames.split("[,\\r\\n]+")));
        return "redirect:/lecturer/assignments";
    }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id) {
        service.open(id);
        return "redirect:/lecturer/assignments";
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id) {
        service.close(id);
        return "redirect:/lecturer/assignments";
    }

    @GetMapping("/{id}/submissions")
    public String submissions(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("assignmentId", id);
        model.addAttribute("submissions", service.submissions(id, page));
        model.addAttribute("grade", new GradeForm());
        return "lecturer/submissions";
    }

    @PostMapping("/{assignmentId}/submissions/{submissionId}/grade")
    public String grade(@PathVariable Long assignmentId, @PathVariable Long submissionId,
                        @Valid @ModelAttribute("grade") GradeForm form, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("assignmentId", assignmentId);
            model.addAttribute("submissions", service.submissions(assignmentId, 0));
            model.addAttribute("grade", form);
            return "lecturer/submissions";
        }
        service.grade(assignmentId, submissionId, form);
        return "redirect:/lecturer/assignments/" + assignmentId + "/submissions";
    }
}
