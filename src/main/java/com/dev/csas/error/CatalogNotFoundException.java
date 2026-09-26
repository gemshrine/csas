package com.dev.csas.error;

public class CatalogNotFoundException extends RuntimeException {

    public CatalogNotFoundException(String message) {
        super(message);
    }

    public static CatalogNotFoundException group(Long id) {
        return new CatalogNotFoundException("Группа с id " + id + " не найдена");
    }

    public static CatalogNotFoundException student(Long id) {
        return new CatalogNotFoundException("Студент с id " + id + " не найден");
    }

    public static CatalogNotFoundException teacher(Long id) {
        return new CatalogNotFoundException("Преподаватель с id " + id + " не найден");
    }

    public static CatalogNotFoundException discipline(Long id) {
        return new CatalogNotFoundException("Дисциплина с id " + id + " не найдена");
    }

    public static CatalogNotFoundException semester(Long id) {
        return new CatalogNotFoundException("Семестр с id " + id + " не найден");
    }
}
