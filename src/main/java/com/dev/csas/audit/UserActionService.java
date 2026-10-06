package com.dev.csas.audit;

import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.user.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserActionService {

    private final UserActionRepository actions;
    private final UserRepository users;

    public UserActionService(UserActionRepository actions, UserRepository users) {
        this.actions = actions;
        this.users = users;
    }

    @Transactional
    public void record(UserActionType actionType, String entityType, Long entityId, String details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authenticated(authentication);
        UserJPAEntity actor = isAuthenticated ? users.findByUsername(authentication.getName()).orElse(null) : null;
        String username = isAuthenticated ? authentication.getName() : "SYSTEM";
        String role = isAuthenticated ? role(authentication) : "SYSTEM";

        actions.save(UserActionJPAEntity.builder()
                .actor(actor)
                .actorUsername(username)
                .actorRole(role)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public List<UserActionResponse> findRecent() {
        return actions.findTop200ByOrderByCreatedAtDescIdDesc().stream()
                .map(UserActionResponse::from)
                .toList();
    }

    private boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("UNKNOWN");
    }
}
