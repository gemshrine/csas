package com.dev.csas.api.user;

import com.dev.csas.audit.UserActionResponse;
import com.dev.csas.audit.UserActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-events")
public class AdminUserActionController {

    private final UserActionService userActions;

    public AdminUserActionController(UserActionService userActions) {
        this.userActions = userActions;
    }

    @GetMapping
    public List<UserActionResponse> list() {
        return userActions.findRecent();
    }
}
