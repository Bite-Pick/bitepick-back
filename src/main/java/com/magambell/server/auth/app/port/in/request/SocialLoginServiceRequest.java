package com.magambell.server.auth.app.port.in.request;

import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;

public record SocialLoginServiceRequest(
        ProviderType providerType,
        String authCode,
        String name,
        String nickName,
        String phoneNumber,
        UserRole userRole,
        SignupSource signupSource,
        String signupSourceDetail
) {

    public SocialLoginServiceRequest(final ProviderType providerType, final String authCode, final String name,
                                     final String nickName, final String phoneNumber, final UserRole userRole) {
        this(providerType, authCode, name, nickName, phoneNumber, userRole, null, null);
    }
}
