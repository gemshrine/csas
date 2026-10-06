package com.dev.csas.web;

import com.dev.csas.discipline.DisciplineService;
import com.dev.csas.group.GroupService;
import com.dev.csas.semester.SemesterService;
import com.dev.csas.student.StudentService;
import com.dev.csas.teacher.TeacherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CatalogPageController {

    private final GroupService groups;
    private final StudentService students;
    private final TeacherService teachers;
    private final DisciplineService disciplines;
    private final SemesterService semesters;

    public CatalogPageController(
            GroupService groups,
            StudentService students,
            TeacherService teachers,
            DisciplineService disciplines,
            SemesterService semesters
    ) {
        this.groups = groups;
        this.students = students;
        this.teachers = teachers;
        this.disciplines = disciplines;
        this.semesters = semesters;
    }

    @GetMapping("/groups")
    public String groups(Model model) {
        return list("groups", groups.findAll(), model);
    }

    @GetMapping("/groups/{id}")
    public String group(@PathVariable Long id, Model model) {
        return detail("groups", groups.findById(id), model);
    }

    @GetMapping("/students")
    public String students(Model model) {
        return list("students", students.findAll(), model);
    }

    @GetMapping("/students/{id}")
    public String student(@PathVariable Long id, Model model) {
        return detail("students", students.findById(id), model);
    }

    @GetMapping("/teachers")
    public String teachers(Model model) {
        return list("teachers", teachers.findAll(), model);
    }

    @GetMapping("/teachers/{id}")
    public String teacher(@PathVariable Long id, Model model) {
        return detail("teachers", teachers.findById(id), model);
    }

    @GetMapping("/disciplines")
    public String disciplines(Model model) {
        return list("disciplines", disciplines.findAll(), model);
    }

    @GetMapping("/disciplines/{id}")
    public String discipline(@PathVariable Long id, Model model) {
        return detail("disciplines", disciplines.findById(id), model);
    }

    @GetMapping("/semesters")
    public String semesters(Model model) {
        return list("semesters", semesters.findAll(), model);
    }

    @GetMapping("/semesters/{id}")
    public String semester(@PathVariable Long id, Model model) {
        return detail("semesters", semesters.findById(id), model);
    }

    private String list(String type, List<?> items, Model model) {
        model.addAttribute("catalogType", type);
        model.addAttribute("items", items);
        model.addAttribute("item", null);
        return "catalog";
    }

    private String detail(String type, Object item, Model model) {
        model.addAttribute("catalogType", type);
        model.addAttribute("items", List.of());
        model.addAttribute("item", item);
        return "catalog";
    }
}
