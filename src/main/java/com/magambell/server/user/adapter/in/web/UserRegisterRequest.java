package com.magambell.server.user.adapter.in.web;

import com.magambell.server.user.app.port.in.request.RegisterServiceRequest;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        String name,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        String phoneNumber,

        @NotBlank(message = "인증번호를 입력해 주세요.")
        String authCode,

        @NotNull(message = "회원 형태를 선택해 주세요.")
        UserRole userRole,

        @NotNull(message = "가입 경로를 선택해 주세요.")
        SignupSource signupSource,

        @Size(max = 100, message = "가입 경로 상세 내용은 100자 이내로 입력해 주세요.")
        String signupSourceDetail

) {
    public RegisterServiceRequest toServiceRequest() {
        return new RegisterServiceRequest(email, password, name, phoneNumber, userRole, authCode,
                signupSource, signupSourceDetail);
    }
}
