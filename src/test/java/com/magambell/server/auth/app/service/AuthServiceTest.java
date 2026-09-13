package com.magambell.server.auth.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.magambell.server.auth.app.port.in.request.SocialLoginServiceRequest;
import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.auth.domain.model.JwtToken;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.user.app.dto.OAuthUserInfo;
import com.magambell.server.user.app.port.in.dto.UserSocialAccountDTO;
import com.magambell.server.user.app.port.out.OAuthClient;
import com.magambell.server.user.app.port.out.UserCommandPort;
import com.magambell.server.user.app.port.out.UserQueryPort;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.repository.UserRepository;
import com.magambell.server.user.domain.repository.UserSocialAccountRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserQueryPort userQueryPort;

    @Autowired
    private UserCommandPort userCommandPort;

    private AuthService testService;

    @BeforeEach
    void setUp() {

        userSocialAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        OAuthClient kakaoOAuthClient = mock(OAuthClient.class);
        OAuthUserInfo userInfo = new OAuthUserInfo("testId", "test@test.com", ProviderType.KAKAO);
        when(kakaoOAuthClient.getProviderType()).thenReturn(ProviderType.KAKAO);
        when(kakaoOAuthClient.getUserInfo(anyString())).thenReturn(userInfo);

        List<OAuthClient> kakaoOAuthClients = Collections.singletonList(kakaoOAuthClient);
        testService = new AuthService(
                kakaoOAuthClients,
                userQueryPort,
                userCommandPort,
                jwtService
        );
    }

    @DisplayName("계정이 없으면 각 가입 경로 값으로 회원가입 처리한다.")
    @ParameterizedTest
    @EnumSource(SignupSource.class)
    void kakaoSignUp(final SignupSource signupSource) {
        // given
        String signupSourceDetail = signupSource == SignupSource.OTHER ? "친구 소개" : null;
        SocialLoginServiceRequest socialLoginServiceRequest = new SocialLoginServiceRequest(ProviderType.KAKAO, "test",
                "이름", "닉네임", "01012341234", UserRole.CUSTOMER, signupSource, signupSourceDetail);

        // when
        testService.loginOrSignUp(socialLoginServiceRequest);

        // then
        User user = userRepository.findAll().get(0);
        assertThat(user).extracting("email", "name")
                .contains(
                        "test@test.com",
                        "이름");
        assertThat(user.getSignupSource()).isEqualTo(signupSource);
        assertThat(user.getSignupSourceDetail()).isEqualTo(signupSourceDetail);
    }

    @DisplayName("신규 소셜 회원가입 시 가입 경로가 누락되면 예외가 발생한다.")
    @Test
    void kakaoSignUpWithoutSignupSource() {
        SocialLoginServiceRequest request = new SocialLoginServiceRequest(ProviderType.KAKAO, "test",
                "이름", "닉네임", "01012341234", UserRole.CUSTOMER, null, null);

        assertThatThrownBy(() -> testService.loginOrSignUp(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE.getMessage());
    }

    @DisplayName("신규 소셜 회원가입 시 OTHER 상세 입력이 누락되면 예외가 발생한다.")
    @Test
    void kakaoSignUpOtherWithoutDetail() {
        SocialLoginServiceRequest request = new SocialLoginServiceRequest(ProviderType.KAKAO, "test",
                "이름", "닉네임", "01012341234", UserRole.CUSTOMER, SignupSource.OTHER, null);

        assertThatThrownBy(() -> testService.loginOrSignUp(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE_DETAIL.getMessage());
    }

    @DisplayName("신규 소셜 회원가입 시 OTHER가 아닌데 상세 입력이 있으면 예외가 발생한다.")
    @Test
    void kakaoSignUpNonOtherWithDetail() {
        SocialLoginServiceRequest request = new SocialLoginServiceRequest(ProviderType.KAKAO, "test",
                "이름", "닉네임", "01012341234", UserRole.CUSTOMER, SignupSource.SEARCH, "검색");

        assertThatThrownBy(() -> testService.loginOrSignUp(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE_DETAIL.getMessage());
    }

    @DisplayName("이미 계정이 있으면 로그인 처리한다.")
    @Test
    void kakaoLogin() {
        // given
        UserSocialAccountDTO userSocialAccountDTO = new UserSocialAccountDTO("test@test.com", "테스트이름", "닉네임",
                "01012341234",
                ProviderType.KAKAO,
                "testId", UserRole.CUSTOMER);
        User saveUser = userSocialAccountDTO.toUser();
        saveUser.addUserSocialAccount(userSocialAccountDTO.toUserSocialAccount());
        userRepository.save(saveUser);

        SocialLoginServiceRequest socialLoginServiceRequest = new SocialLoginServiceRequest(ProviderType.KAKAO, "test",
                "이름", "닉네임", "01012341234", UserRole.CUSTOMER);

        // when
        JwtToken jwtToken = testService.loginOrSignUp(socialLoginServiceRequest);

        // then
        Long userId = jwtService.getJwtUserId(jwtToken.accessToken());
        User user = userRepository.findUserBySocial(ProviderType.KAKAO, "testId")
                .orElse(null);

        assertThat(user).extracting("email", "name")
                .contains(
                        "test@test.com",
                        "테스트이름");
        assertThat(user.getUserSocialAccounts().get(0)).extracting("providerType", "providerId")
                .contains(
                        ProviderType.KAKAO,
                        "testId"
                );
    }
}
