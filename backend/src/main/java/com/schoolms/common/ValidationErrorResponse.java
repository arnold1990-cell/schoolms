package com.schoolms.common;

import java.util.Map;

public record ValidationErrorResponse(
        boolean success,
        String message,
        Map<String, String> errors
) {
}
