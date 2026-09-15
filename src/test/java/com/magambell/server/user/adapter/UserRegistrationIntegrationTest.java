package com.magambell.server.user.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magambell.server.user.app.port.in.dto.UserEmailDTO;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.VerificationStatus;
import com.magambell.server.user.domain.repository.UserEmailRepository;
import com.magambell.server.user.domain.repository.UserRepository;
import com.magambell.server.user.domain.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEmailRepository userEmailRepository;

    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;

    @BeforeEach
    void setUp() {
        userSocialAccountRepository.deleteAllInBatch();
        userEmailRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입 API가 가입 경로와 OTHER 상세 입력값을 DB에 저장한다.")
    @Test
    void registerThroughApiPersistsSignupSource() throws Exception {
        String email = "signup-source@test.com";
        String authCode = "test-auth-code";
        userEmailRepository.save(new UserEmailDTO(email, authCode, VerificationStatus.REGISTER).toUserEmail());

        String request = objectMapper.writeValueAsString(new RegisterRequest(
                email,
                "Qwer1234!!",
                "테스트 회원",
                "01012341234",
                authCode,
                "CUSTOMER",
                "OTHER",
                "  친구 소개  "
        ));

        mockMvc.perform(post("/api/v1/user/register")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        User user = userRepository.findAll().get(0);
        assertThat(user.getSignupSource()).isEqualTo(SignupSource.OTHER);
        assertThat(user.getSignupSourceDetail()).isEqualTo("친구 소개");
        assertThat(userEmailRepository.findAll()).isEmpty();
    }

    private record RegisterRequest(
            String email,
            String password,
            String name,
            String phoneNumber,
            String authCode,
            String userRole,
            String signupSource,
            String signupSourceDetail
    ) {
    }
}
