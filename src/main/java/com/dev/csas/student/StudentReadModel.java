package com.dev.csas.student;

public record StudentReadModel(long id, String firstName, String lastName, String patronymic) {
    public String displayName() {
        return lastName + " " + firstName + (patronymic == null || patronymic.isBlank() ? "" : " " + patronymic);
    }
}
