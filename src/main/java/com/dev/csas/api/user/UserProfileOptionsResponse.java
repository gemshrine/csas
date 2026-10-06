package com.dev.csas.api.user;

import java.util.List;

public record UserProfileOptionsResponse(List<ProfileOption> teachers, List<ProfileOption> students) {

    public record ProfileOption(long id, String name) {
    }
}
