package com.dev.csas.api.control;

import jakarta.validation.constraints.Size;

public record ReasonRequest(@Size(max = 500) String reason) {
}
