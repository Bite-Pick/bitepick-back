package com.magambell.server.auth.adapter.in.web;

import com.magambell.server.auth.app.port.in.request.SocialLoginServiceRequest;
import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
        @NotNull(message = "잘못된 형식 입니다.")
        ProviderType providerType,

        @NotBlank(message = "인증번호를 입력해 주세요.")
        String authCode,

        String name,

        String nickName,

        String phoneNumber,

        UserRole userRole,

        SignupSource signupSource,

        String signupSourceDetail
) {
    public SocialLoginServiceRequest toServiceRequest() {
        return new SocialLoginServiceRequest(providerType, authCode, name, nickName, phoneNumber, userRole,
                signupSource, signupSourceDetail);
    }
}
