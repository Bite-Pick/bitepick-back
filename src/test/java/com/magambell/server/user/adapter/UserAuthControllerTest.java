package com.magambell.server.user.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
