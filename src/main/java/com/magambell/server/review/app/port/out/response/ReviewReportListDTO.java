package com.magambell.server.review.app.port.out.response;


import com.magambell.server.user.domain.enums.UserRole;

public record ReviewReportListDTO(
        Long userId,
        String nickName,
        UserRole userRole
) {
}
