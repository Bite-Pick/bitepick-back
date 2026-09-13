package com.magambell.server.user.app.port.in.dto;

import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.entity.UserSocialAccount;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;

public record UserSocialAccountDTO(
        String email,
        String name,
        String nickName,
        String phoneNumber,
        ProviderType providerType,
        String providerId,
        UserRole userRole,
        SignupSource signupSource,
        String signupSourceDetail
) {

    public UserSocialAccountDTO(final String email, final String name, final String nickName,
                                final String phoneNumber,
                                final ProviderType providerType,
                                final String providerId,
                                final UserRole userRole,
                                final SignupSource signupSource,
                                final String signupSourceDetail) {
        this.email = validateEmail(email);
        this.name = name;
        this.nickName = nickName;
        this.phoneNumber = validatePhone(phoneNumber);
        this.providerType = providerType;
        this.providerId = providerId;
        this.userRole = userRole;
        this.signupSource = signupSource;
        this.signupSourceDetail = signupSourceDetail;
    }

    public UserSocialAccountDTO(final String email, final String name, final String nickName,
                                final String phoneNumber, final ProviderType providerType,
                                final String providerId, final UserRole userRole) {
        this(email, name, nickName, phoneNumber, providerType, providerId, userRole, null, null);
    }

    private String validateEmail(final String email) {
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new InvalidRequestException(ErrorCode.USER_VALID_EMAIL);
        }
        return email;
    }

    private String validatePhone(final String phoneNumber) {
        if (!phoneNumber.matches("^(?!.*-)[0-9]{10,11}$")) {
//            throw new InvalidRequestException(ErrorCode.USER_VALID_PHONE);
        }
        return phoneNumber;
    }

    public User toUser() {
        return User.createBySocial(this);
    }

    public UserSocialAccount toUserSocialAccount() {
        return UserSocialAccount.create(this);
    }
}
