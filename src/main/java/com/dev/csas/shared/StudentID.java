package com.dev.csas.shared;

import java.util.Objects;

public record StudentID (
        long id
) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StudentID studentID = (StudentID) o;
        return id == studentID.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
