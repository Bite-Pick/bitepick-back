package com.magambell.server.user.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magambell.server.auth.app.service.JwtService;
import com.magambell.server.user.adapter.in.web.UserRegisterRequest;
import com.magambell.server.user.app.port.in.UserUseCase;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserUseCase userUseCase;

    @MockBean
    private JwtService jwtService;

    @DisplayName("회원가입을 진행한다.")
    @Test
    void register() throws Exception {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@test.com",
                "qwer1234!!",
                "홍길동",
                "01012341234",
                "test",
                UserRole.CUSTOMER,
                SignupSource.INSTAGRAM,
                null);

        // when // then
        mockMvc.perform(
                        post("/api/v1/user/register")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @DisplayName("회원가입 요청에서 가입 경로를 누락하면 400을 반환한다.")
    @Test
    void registerWithoutSignupSource() throws Exception {
        String request = """
                {
                  "email": "test@test.com",
                  "password": "Qwer1234!!",
                  "name": "홍길동",
                  "phoneNumber": "01012341234",
                  "authCode": "test",
                  "userRole": "CUSTOMER"
                }
                """;

        mockMvc.perform(post("/api/v1/user/register")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userUseCase);
    }

    @DisplayName("회원가입 요청의 가입 경로 코드가 올바르지 않으면 400을 반환한다.")
    @Test
    void registerWithUnknownSignupSource() throws Exception {
        String request = """
                {
                  "email": "test@test.com",
                  "password": "Qwer1234!!",
                  "name": "홍길동",
                  "phoneNumber": "01012341234",
                  "authCode": "test",
                  "userRole": "CUSTOMER",
                  "signupSource": "UNKNOWN"
                }
                """;

        mockMvc.perform(post("/api/v1/user/register")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userUseCase);
    }
}
