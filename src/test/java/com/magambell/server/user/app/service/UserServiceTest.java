package com.magambell.server.user.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.magambell.server.auth.app.service.JwtService;
import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.common.exception.NotEqualException;
import com.magambell.server.common.exception.NotFoundException;
import com.magambell.server.common.security.CustomUserDetails;
import com.magambell.server.goods.adapter.in.web.GoodsImagesRegister;
import com.magambell.server.goods.app.port.in.dto.RegisterGoodsDTO;
import com.magambell.server.goods.domain.entity.Goods;
import com.magambell.server.goods.domain.repository.GoodsRepository;
import com.magambell.server.order.app.port.in.dto.CreateOrderDTO;
import com.magambell.server.order.domain.entity.Order;
import com.magambell.server.order.domain.repository.OrderGoodsRepository;
import com.magambell.server.order.domain.repository.OrderRepository;
import com.magambell.server.payment.domain.repository.PaymentRepository;
import com.magambell.server.stock.domain.repository.StockHistoryRepository;
import com.magambell.server.stock.domain.repository.StockRepository;
import com.magambell.server.store.app.port.in.dto.RegisterStoreDTO;
import com.magambell.server.store.domain.enums.Approved;
import com.magambell.server.store.domain.enums.Bank;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.store.domain.repository.StoreRepository;
import com.magambell.server.user.adapter.out.persistence.UserInfoResponse;
import com.magambell.server.user.app.port.in.dto.UserEmailDTO;
import com.magambell.server.user.app.port.in.dto.UserSocialAccountDTO;
import com.magambell.server.user.app.port.in.request.RegisterServiceRequest;
import com.magambell.server.user.app.port.out.dto.MyPageStatsDTO;
import com.magambell.server.user.domain.enums.UserRole;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.VerificationStatus;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.repository.UserEmailRepository;
import com.magambell.server.user.domain.repository.UserRepository;
import com.magambell.server.user.domain.repository.UserSocialAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserEmailRepository userEmailRepository;
    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private OrderGoodsRepository orderGoodsRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private StockHistoryRepository stockHistoryRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        stockHistoryRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
        orderGoodsRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        goodsRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        userSocialAccountRepository.deleteAllInBatch();
        userEmailRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입")
    @Test
    void register() {
        // given
        String authCode = "testCode";
        String email = "test@test.com";
        UserEmailDTO userEmailDTO = new UserEmailDTO(email, authCode, VerificationStatus.REGISTER);
        userEmailRepository.save(userEmailDTO.toUserEmail());

        RegisterServiceRequest request = new RegisterServiceRequest(email, "Qwer1234!!", "test", "01012341234",
                UserRole.CUSTOMER, authCode, SignupSource.SEARCH, null);

        // when
        userService.register(request);

        // then
        User user = userRepository.findAll().get(0);
        assertThat(user).isNotNull();
        assertThat(user).extracting("email", "name", "phoneNumber")
                .contains(
                        "test@test.com",
                        "test",
                        "01012341234"
                );
        assertThat(user.getSignupSource()).isEqualTo(SignupSource.SEARCH);
        assertThat(user.getSignupSourceDetail()).isNull();
    }

    @DisplayName("회원가입 시 OTHER가 아닌 가입 경로를 저장한다.")
    @ParameterizedTest
    @EnumSource(value = SignupSource.class, names = {"INSTAGRAM", "THREAD", "REFERRAL", "SEARCH"})
    void registerWithSignupSource(final SignupSource signupSource) {
        String email = "test@test.com";
        String authCode = "testCode";
        userEmailRepository.save(new UserEmailDTO(email, authCode, VerificationStatus.REGISTER).toUserEmail());

        userService.register(new RegisterServiceRequest(email, "Qwer1234!!", "test", "01012341234",
                UserRole.CUSTOMER, authCode, signupSource, null));

        User user = userRepository.findAll().get(0);
        assertThat(user.getSignupSource()).isEqualTo(signupSource);
        assertThat(user.getSignupSourceDetail()).isNull();
    }

    @DisplayName("회원가입 시 OTHER와 상세 입력값을 저장하고 앞뒤 공백을 제거한다.")
    @Test
    void registerWithOtherSignupSourceDetail() {
        String email = "test@test.com";
        String authCode = "testCode";
        userEmailRepository.save(new UserEmailDTO(email, authCode, VerificationStatus.REGISTER).toUserEmail());

        userService.register(new RegisterServiceRequest(email, "Qwer1234!!", "test", "01012341234",
                UserRole.CUSTOMER, authCode, SignupSource.OTHER, "  친구 소개  "));

        User user = userRepository.findAll().get(0);
        assertThat(user.getSignupSource()).isEqualTo(SignupSource.OTHER);
        assertThat(user.getSignupSourceDetail()).isEqualTo("친구 소개");
    }

    @DisplayName("회원가입 시 가입 경로가 없으면 예외가 발생한다.")
    @Test
    void registerWithoutSignupSource() {
        RegisterServiceRequest request = new RegisterServiceRequest("test@test.com", "Qwer1234!!", "test",
                "01012341234", UserRole.CUSTOMER, "testCode", null, null);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE.getMessage());
    }

    @DisplayName("OTHER 선택 후 상세 입력이 없으면 예외가 발생한다.")
    @Test
    void registerOtherWithoutSignupSourceDetail() {
        RegisterServiceRequest request = new RegisterServiceRequest("test@test.com", "Qwer1234!!", "test",
                "01012341234", UserRole.CUSTOMER, "testCode", SignupSource.OTHER, null);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE_DETAIL.getMessage());
    }

    @DisplayName("OTHER 선택 후 상세 입력이 공백뿐이면 예외가 발생한다.")
    @Test
    void registerOtherWithBlankSignupSourceDetail() {
        RegisterServiceRequest request = new RegisterServiceRequest("test@test.com", "Qwer1234!!", "test",
                "01012341234", UserRole.CUSTOMER, "testCode", SignupSource.OTHER, "   ");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE_DETAIL.getMessage());
    }

    @DisplayName("OTHER가 아닌 가입 경로에 상세 입력값이 오면 예외가 발생한다.")
    @Test
    void registerNonOtherWithSignupSourceDetail() {
        RegisterServiceRequest request = new RegisterServiceRequest("test@test.com", "Qwer1234!!", "test",
                "01012341234", UserRole.CUSTOMER, "testCode", SignupSource.SEARCH, "네이버 검색");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_SIGNUP_SOURCE_DETAIL.getMessage());
    }

    @DisplayName("기존 회원의 가입 경로와 상세 값이 NULL이어도 조회할 수 있다.")
    @Test
    void findLegacyUserWithNullSignupSource() {
        UserSocialAccountDTO legacyUserDTO = new UserSocialAccountDTO("legacy@test.com", "기존 회원", "기존닉네임",
                "01012341234", ProviderType.KAKAO, "legacy-provider-id", UserRole.CUSTOMER);
        User legacyUser = legacyUserDTO.toUser();
        legacyUser.addUserSocialAccount(legacyUserDTO.toUserSocialAccount());
        User savedUser = userRepository.save(legacyUser);

        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(foundUser.getSignupSource()).isNull();
        assertThat(foundUser.getSignupSourceDetail()).isNull();
    }

    @DisplayName("회원가입시 인증번호가 다르면 예외가 발생한다.")
    @Test
    void registerWithNotEqualAuthCode() {
        // given
        String authCode = "testCode";
        String email = "test@test.com";
        UserEmailDTO userEmailDTO = new UserEmailDTO(email, "testCode2", VerificationStatus.REGISTER);
        userEmailRepository.save(userEmailDTO.toUserEmail());

        RegisterServiceRequest request = new RegisterServiceRequest(email, "Qwer1234!!", "test", "01012341234",
                UserRole.CUSTOMER, authCode, SignupSource.SEARCH, null);

        // when // then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(NotEqualException.class)
                .hasMessage(ErrorCode.USER_EMAIL_AUTH_CODE_NOT_EQUALS.getMessage());
    }

    @DisplayName("회원가입시 인증번호가 존재하지 않으면 예외가 발생한다.")
    @Test
    void registerWithNotFoundAuthCode() {
        // given
        String authCode = "testCode";
        String email = "test@test.com";
        RegisterServiceRequest request = new RegisterServiceRequest(email, "Qwer1234!!", "test", "01012341234",
                UserRole.CUSTOMER, authCode, SignupSource.SEARCH, null);

        // when // then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorCode.USER_EMAIL_NOT_FOUND.getMessage());
    }

    @DisplayName("회원가입시 비밀번호에 대문자, 소문자, 숫자, 특수문자 모두 포함되지 않으면 오류가 발생한다.")
    @ValueSource(strings = {
            "qwer1234!!",     // 대문자 없음
            "QWER1234!!",     // 소문자 없음
            "Qwerasdf!!",     // 숫자 없음
            "Qwer1234",       // 특수문자 없음
            "Q1!",            // 길이 부족
            "Qwer1234!!Qwer1234!!" // 길이 초과
    })
    @ParameterizedTest
    void registerWithInvalidRequestPassword(String password) {
        // given
        String authCode = "testCode";
        String email = "test@test.com";
        UserEmailDTO userEmailDTO = new UserEmailDTO(email, authCode, VerificationStatus.REGISTER);
        userEmailRepository.save(userEmailDTO.toUserEmail());

        RegisterServiceRequest request = new RegisterServiceRequest(email, password, "test", "01012341234",
                UserRole.CUSTOMER, authCode, SignupSource.SEARCH, null);

        // when // then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.USER_VALID_PASSWORD.getMessage());
    }

    @DisplayName("JWT accessToken으로 유저 조회")
    @Test
    void getUserInfo() {
        // given
        UserSocialAccountDTO userSocialAccountDTO = new UserSocialAccountDTO(
                "test@test.com",
                "테스트 이름",
                "닉네임",
                "01012341234",
                ProviderType.KAKAO,
                "testId",
                UserRole.CUSTOMER
        );
        User user = userSocialAccountDTO.toUser();
        user.addUserSocialAccount(userSocialAccountDTO.toUserSocialAccount());
        User saveUser = userRepository.save(user);
        CustomUserDetails customUserDetails = new CustomUserDetails(saveUser.getId(), saveUser.getUserRole());

        // when
        UserInfoResponse userInfo = userService.getUserInfo(customUserDetails);

        // then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.email()).isEqualTo("test@test.com");
    }

    @DisplayName("JWT accessToken으로 유저 조회시 사장님이면 approved 값이 없으면 오류처리")
    @Test
    void getUserInfoByOwner() {
        // given
        UserSocialAccountDTO userSocialAccountDTO = new UserSocialAccountDTO(
                "test@test.com",
                "테스트 이름",
                "닉네임",
                "01012341234",
                ProviderType.KAKAO,
                "testId",
                UserRole.OWNER
        );
        User user = userSocialAccountDTO.toUser();
        user.addUserSocialAccount(userSocialAccountDTO.toUserSocialAccount());
        User saveUser = userRepository.save(user);

        CustomUserDetails customUserDetails = new CustomUserDetails(saveUser.getId(), saveUser.getUserRole());

        // when // then
        assertThatThrownBy(() -> userService.getUserInfo(customUserDetails))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorCode.STORE_NOT_FOUND.getMessage());
    }

    @DisplayName("마이페이지 이용횟수, 탄소, 절약한 금액 조회")
    @Test
    void getMyPage() {
        // given
        UserSocialAccountDTO ownerAccountDTO = new UserSocialAccountDTO(
                "test@test.com", "사장님", "사장님닉네임", "01077771111",
                ProviderType.KAKAO,
                "123974",
                UserRole.OWNER
        );
        User owner = ownerAccountDTO.toUser();
        owner.addUserSocialAccount(ownerAccountDTO.toUserSocialAccount());

        // 매장 생성
        RegisterStoreDTO registerStoreDTO = new RegisterStoreDTO(
                "테스트매장",
                "서울시",
                1.0, 2.0,
                "대표",
                "01099998888",
                "123123",
                Bank.KB국민,
                "9876543210",
                List.of(),
                Approved.APPROVED,
                owner,
                null,
                "주차장");
        Store store = registerStoreDTO.toEntity();

        // 상품 생성
        RegisterGoodsDTO registerGoodsDTO = new RegisterGoodsDTO(
                "상품명",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2),
                120, 10000, 10, 9000,
                store,
                List.of(new GoodsImagesRegister(0, "test", "https://test.com/test.jpg", "상품명"))
        );
        Goods goods = Goods.create(registerGoodsDTO);
        store.addGoods(goods);

        owner.addStore(store);
        userRepository.save(owner);

        UserSocialAccountDTO userSocialAccountDTO = new UserSocialAccountDTO(
                "test@test.com",
                "테스트 이름",
                "닉네임",
                "01012341234",
                ProviderType.KAKAO,
                "testId",
                UserRole.OWNER
        );
        User user = userSocialAccountDTO.toUser();
        user.addUserSocialAccount(userSocialAccountDTO.toUserSocialAccount());
        User saveUser = userRepository.save(user);
        Order order = createOrder(saveUser, goods, 1);
        orderRepository.save(order);

        // when
        MyPageStatsDTO dto = userService.getMyPage(saveUser.getId());

        // then
        assertThat(dto.purchaseCount()).isEqualTo(1);
        assertThat(dto.savedKg()).isEqualTo(2.7);
        assertThat(dto.savedPrice()).isEqualTo(1000);
    }

    private Order createOrder(User user, Goods goods, int i) {

        CreateOrderDTO createOrderDTO = new CreateOrderDTO(user, goods, i, 9000, LocalDateTime.of(2025, 6, 30, 17, 30),
                "test");
        Order createOrder = createOrderDTO.toOrder();
        createOrder.completed();
        return createOrder;
    }
}
