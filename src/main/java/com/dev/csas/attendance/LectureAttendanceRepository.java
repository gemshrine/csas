package com.dev.csas.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureAttendanceRepository extends JpaRepository<LectureAttendanceJPAEntity, Long> {

    boolean existsByStudent_Id(Long studentId);
}
