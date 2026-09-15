package com.magambell.server.user.app.port.in.request;

import com.magambell.server.user.app.port.in.dto.UserDTO;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;

public record RegisterServiceRequest(
        String email,
        String password,
        String name,
        String phoneNumber,
        UserRole userRole,
        String authCode,
        SignupSource signupSource,
        String signupSourceDetail) {

    public UserDTO toCreateUserDTO(final String password) {
        String normalizedSignupSourceDetail = signupSource == SignupSource.OTHER && signupSourceDetail != null
                ? signupSourceDetail.trim()
                : null;
        return new UserDTO(this.email, password, this.name, this.phoneNumber, this.userRole, this.signupSource,
                normalizedSignupSourceDetail);
    }
}
