package com.dev.csas.control.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ControlSheetEventRepository extends JpaRepository<ControlSheetEventJPAEntity, Long> {
}
