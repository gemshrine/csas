package com.dev.csas.discipline;

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
@RequestMapping("/api/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService disciplineService;

    @GetMapping
    public List<DisciplineResponse> findAll() {
        return disciplineService.findAll();
    }

    @GetMapping("/{id}")
    public DisciplineResponse findById(@PathVariable Long id) {
        return disciplineService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DisciplineResponse> create(@Valid @RequestBody DisciplineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disciplineService.create(request));
    }

    @PutMapping("/{id}")
    public DisciplineResponse update(@PathVariable Long id, @Valid @RequestBody DisciplineRequest request) {
        return disciplineService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        disciplineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
