package com.dev.csas.semester;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    public List<SemesterResponse> findAll() {
        return semesterService.findAll();
    }

    @GetMapping("/{id}")
    public SemesterResponse findById(@PathVariable Long id) {
        return semesterService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SemesterResponse> create(@Valid @RequestBody SemesterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(semesterService.create(request));
    }

    @PutMapping("/{id}")
    public SemesterResponse update(@PathVariable Long id, @Valid @RequestBody SemesterRequest request) {
        return semesterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        semesterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
