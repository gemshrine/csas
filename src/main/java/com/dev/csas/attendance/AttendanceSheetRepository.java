package com.dev.csas.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSheetRepository extends JpaRepository<AttendanceSheetJPAEntity, Long> {
}
