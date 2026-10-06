package com.dev.csas.api.user;

import com.dev.csas.user.UserAdministrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdministrationService users;

    public AdminUserController(UserAdministrationService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserAccountResponse> list() {
        return users.findAll();
    }

    @GetMapping("/profiles")
    public UserProfileOptionsResponse profiles() {
        return users.findAvailableProfiles();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountResponse create(@Valid @RequestBody UserCreationRequest request) {
        return users.create(request);
    }

    @PatchMapping("/{userId}")
    public UserAccountResponse update(
            @PathVariable long userId,
            @Valid @RequestBody UserAccountPatchRequest request,
            Authentication authentication
    ) {
        return users.update(userId, request, authentication.getName());
    }

    @PutMapping("/{userId}/role")
    public UserAccountResponse changeRole(
            @PathVariable long userId,
            @Valid @RequestBody UserRoleChangeRequest request
    ) {
        return users.changeRole(userId, request.role(), request.profileId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long userId, Authentication authentication) {
        users.delete(userId, authentication.getName());
    }
}
