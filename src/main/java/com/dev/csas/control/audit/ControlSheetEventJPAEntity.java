package com.dev.csas.control.audit;

import com.dev.csas.control.PassFail;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.student.StudentJPAEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "control_sheet_events")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ControlSheetEventJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_sheet_id", nullable = false)
    private ControlSheetJPAEntity controlSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private StudentJPAEntity student;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ControlActorType actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ControlEventType eventType;

    @Column(name = "old_numeric_score")
    private Integer oldNumericScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_pass_fail", length = 10)
    private PassFail oldPassFail;

    @Column(name = "new_numeric_score")
    private Integer newNumericScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_pass_fail", length = 10)
    private PassFail newPassFail;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
