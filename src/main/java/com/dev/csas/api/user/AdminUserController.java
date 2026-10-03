package com.dev.csas.api.user;

import com.dev.csas.user.UserAdministrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PutMapping("/{userId}/role")
    public UserAccountResponse changeRole(
            @PathVariable long userId,
            @Valid @RequestBody UserRoleChangeRequest request
    ) {
        return users.changeRole(userId, request.role());
    }
}
