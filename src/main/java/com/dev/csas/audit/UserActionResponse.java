package com.dev.csas.audit;

import java.time.Instant;

public record UserActionResponse(
        long id,
        String actorUsername,
        String actorRole,
        UserActionType actionType,
        String entityType,
        Long entityId,
        String details,
        Instant createdAt
) {
    public static UserActionResponse from(UserActionJPAEntity event) {
        return new UserActionResponse(
                event.getId(),
                event.getActorUsername(),
                event.getActorRole(),
                event.getActionType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }
}
