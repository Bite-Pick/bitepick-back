package com.magambell.server.review.app.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.DuplicateException;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.goods.adapter.in.web.GoodsImagesRegister;
import com.magambell.server.goods.app.port.in.dto.RegisterGoodsDTO;
import com.magambell.server.goods.domain.entity.Goods;
import com.magambell.server.goods.domain.repository.GoodsRepository;
import com.magambell.server.order.app.port.in.dto.CreateOrderDTO;
import com.magambell.server.order.domain.entity.Order;
import com.magambell.server.order.domain.entity.OrderGoods;
import com.magambell.server.order.domain.repository.OrderGoodsRepository;
import com.magambell.server.order.domain.repository.OrderRepository;
import com.magambell.server.payment.domain.repository.PaymentRepository;
import com.magambell.server.review.app.port.in.dto.RegisterReviewDTO;
import com.magambell.server.review.app.port.in.request.DeleteReviewReplyServiceRequest;
import com.magambell.server.review.app.port.in.request.DeleteReviewServiceRequest;
import com.magambell.server.review.app.port.in.request.RegisterReviewReplyServiceRequest;
import com.magambell.server.review.app.port.in.request.RegisterReviewServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewListServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewMyServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewRatingAllServiceRequest;
import com.magambell.server.review.app.port.in.request.ReportReviewServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewReportListServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewStoreServiceRequest;
import com.magambell.server.review.app.port.out.ReviewCommandPort;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewReportListDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreItemDTO;
import com.magambell.server.review.adapter.out.persistence.ReviewStoreResponse;
import com.magambell.server.review.domain.entity.ReviewImage;
import com.magambell.server.review.domain.entity.ReviewReply;
import com.magambell.server.review.domain.enums.ReviewReplyStatus;
import com.magambell.server.review.domain.enums.ReviewStatus;
import com.magambell.server.review.domain.enums.ReviewStoreFilter;
import com.magambell.server.review.domain.entity.Review;
import com.magambell.server.review.domain.repository.ReviewImageRepository;
import com.magambell.server.review.domain.repository.ReviewReplyRepository;
import com.magambell.server.review.domain.repository.ReviewReportRepository;
import com.magambell.server.review.domain.repository.ReviewRepository;
import com.magambell.server.stock.domain.repository.StockHistoryRepository;
import com.magambell.server.stock.domain.repository.StockRepository;
import com.magambell.server.store.app.port.in.dto.RegisterStoreDTO;
import com.magambell.server.store.domain.enums.Approved;
import com.magambell.server.store.domain.enums.Bank;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.store.domain.repository.StoreImageRepository;
import com.magambell.server.store.domain.repository.StoreRepository;
import com.magambell.server.user.app.port.in.dto.UserSocialAccountDTO;
import com.magambell.server.user.domain.enums.UserRole;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.repository.UserRepository;
import com.magambell.server.user.domain.repository.UserSocialAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ReviewServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private StoreImageRepository storeImageRepository;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private StockHistoryRepository stockHistoryRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewReplyRepository reviewReplyRepository;
    @Autowired
    private ReviewReportRepository reviewReportRepository;
    @Autowired
    private ReviewImageRepository reviewImageRepository;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewCommandPort reviewCommandPort;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderGoodsRepository orderGoodsRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    private User user;
    private User owner;
    private Goods goods;
    private Order order;
    private OrderGoods orderGoods;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        UserSocialAccountDTO accountDTO = new UserSocialAccountDTO(
                "order@test.com", "주문자", "주문자닉", "01011112222",
                ProviderType.KAKAO,
                "socialId", UserRole.CUSTOMER
        );
        user = accountDTO.toUser();
        user.addUserSocialAccount(accountDTO.toUserSocialAccount());
        user = userRepository.save(user);

        UserSocialAccountDTO ownerAccountDTO = new UserSocialAccountDTO(
                "test@test.com", "사장님", "사장님닉네임", "01077771111",
                ProviderType.KAKAO,
                "123974",
                UserRole.OWNER
        );
        owner = ownerAccountDTO.toUser();
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
                10, 10000, 10, 9000,
                store,
                List.of(new GoodsImagesRegister(0, "test", "https://test.com/test.jpg", "상품명"))
        );
        goods = Goods.create(registerGoodsDTO);
        store.addGoods(goods);

        owner.addStore(store);
        userRepository.save(owner);
        CreateOrderDTO createOrderDTO = new CreateOrderDTO(user, goods, 1, 9000, LocalDateTime.now(), "test");
        Order createOrder = createOrderDTO.toOrder();
        createOrder.completed();
        order = orderRepository.save(createOrder);
        orderGoods = order.getOrderGoodsList().get(0);
    }

    @AfterEach
    void tearDown() {
        reviewImageRepository.deleteAllInBatch();
        reviewReplyRepository.deleteAllInBatch();
        reviewReportRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        stockHistoryRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
        orderGoodsRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        goodsRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        userSocialAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("리뷰를 등록한다.")
    @Test
    void registerReview() {
        // given
        RegisterReviewServiceRequest request = new RegisterReviewServiceRequest(
                orderGoods.getId(),
                2,
                "test",
                List.of()
        );

        // when
        reviewService.registerReview(request, user.getId());

        // then
        Review review = reviewRepository.findAll().get(0);
        assertThat(review).isNotNull();
        assertThat(review.getDescription()).isEqualTo("test");
    }

    @DisplayName("이미지 없이 리뷰를 등록한다.")
    @Test
    void registerReviewWithoutImages() {
        // given
        RegisterReviewServiceRequest request = new RegisterReviewServiceRequest(
                orderGoods.getId(),
                3,
                "이미지 없이 작성된 리뷰입니다.",
                null
        );

        // when
        reviewService.registerReview(request, user.getId());

        // then
        Review review = reviewRepository.findAll().get(0);
        assertThat(review).isNotNull();
        assertThat(review.getDescription()).isEqualTo("이미지 없이 작성된 리뷰입니다.");
        assertThat(review.getRating()).isEqualTo(3);
        // 이미지가 저장되지 않았는지 확인
        assertThat(reviewImageRepository.findAll()).isEmpty();
    }

    @DisplayName("리뷰 리스트를 출력한다.")
    @Test
    void getReviewList() {
        // given
        RegisterReviewDTO dto = new RegisterReviewDTO(
                orderGoods.getId(),
                2,
                "test",
                List.of(),
                user,
                orderGoods
        );
        reviewRepository.save(Review.create(dto));
        ReviewListServiceRequest request = new ReviewListServiceRequest(user.getId(), goods.getId(), false, 1, 10);

        // when
        List<ReviewListDTO> reviewList = reviewService.getReviewList(request);

        // then
        assertThat(reviewList).isNotNull();
        assertThat(reviewList.get(0).description()).isEqualTo("test");
        assertThat(reviewList.get(0).goodsId()).isEqualTo(goods.getId());
    }

    @DisplayName("리뷰를 리스트 평점별 조회")
    @Test
    void getReviewRatingAll() {
        // given
        List<Review> reviewList = IntStream.range(1, 4)
                .mapToObj(this::createReview)
                .toList();

        reviewRepository.saveAll(reviewList);

        List<Review> reviewList2 = IntStream.range(1, 3)
                .mapToObj(this::createReview)
                .toList();

        reviewRepository.saveAll(reviewList2);
        ReviewRatingAllServiceRequest request = new ReviewRatingAllServiceRequest(goods.getId(), false);

        // when
        ReviewRatingSummaryDTO reviewRatingAll = reviewService.getReviewRatingAll(request);

        // then
        assertThat(reviewRatingAll).isNotNull();
        assertThat(reviewRatingAll.averageRating()).isEqualTo(1.8);
        assertThat(reviewRatingAll.totalCount()).isEqualTo(5);
        assertThat(reviewRatingAll.rating2Count()).isEqualTo(2);
    }

    @DisplayName("내가 작성한 리뷰 목록")
    @Test
    void getReviewListByUser() {
        // given
        RegisterReviewDTO dto = new RegisterReviewDTO(
                order.getId(),
                2,
                "test",
                List.of(),
                user,
                orderGoods
        );
        reviewRepository.save(Review.create(dto));
        ReviewMyServiceRequest request = new ReviewMyServiceRequest(1, 10, user.getId());

        // when
        List<ReviewListDTO> reviewList = reviewService.getReviewListByUser(request);

        // then
        assertThat(reviewList).isNotNull();
        assertThat(reviewList.get(0).description()).isEqualTo("test");
        assertThat(reviewList.get(0).nickName()).isEqualTo(user.getNickName());
    }

    @DisplayName("내가 작성한 리뷰 삭제")
    @Test
    void deleteReview() {
        // given
        RegisterReviewDTO dto = new RegisterReviewDTO(
                order.getId(),
                2,
                "test",
                List.of(),
                user,
                orderGoods
        );
        Review review = reviewRepository.save(Review.create(dto));

        // when
        reviewService.deleteReview(new DeleteReviewServiceRequest(review.getId(), user.getId()));

        // then
        List<Review> reviewAll = reviewRepository.findAll();
        assertThat(reviewAll).hasSize(1);
        assertThat(reviewAll.get(0).getReviewStatus()).isEqualTo(ReviewStatus.DELETED);
    }

    @DisplayName("사장님이 본인 매장 리뷰에 답글을 작성한다.")
    @Test
    void registerReviewReply() {
        // given
        Review review = saveReview();
        RegisterReviewReplyServiceRequest request = new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "방문해 주셔서 감사합니다."
        );

        // when
        reviewService.registerReviewReply(request);

        // then
        ReviewReply reply = reviewReplyRepository.findAll().get(0);
        assertThat(reply.getReview().getId()).isEqualTo(review.getId());
        assertThat(reply.getContent()).isEqualTo("방문해 주셔서 감사합니다.");
        assertThat(reply.getReplyStatus()).isEqualTo(ReviewReplyStatus.ACTIVE);
    }

    @DisplayName("이미 ACTIVE 답글이 있으면 중복 답글 작성에 실패한다.")
    @Test
    void registerReviewReply_throwsWhenDuplicateActiveReply() {
        // given
        Review review = saveReview();
        reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "첫 번째 답글"
        ));

        // when & then
        assertThatThrownBy(() -> reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "두 번째 답글"
        ))).isInstanceOf(DuplicateException.class);
    }

    @DisplayName("리뷰 답글 review_id unique 제약 위반은 중복 답글 예외로 변환된다.")
    @Test
    void saveReviewReply_translatesReviewIdUniqueConstraintViolation() {
        // given
        Review review = saveReview();
        ReviewReply firstReply = ReviewReply.create("첫 번째 답글");
        firstReply.addReview(review);
        reviewReplyRepository.saveAndFlush(firstReply);

        ReviewReply duplicateReply = ReviewReply.create("두 번째 답글");
        duplicateReply.addReview(review);

        // when & then
        assertThatThrownBy(() -> reviewCommandPort.saveReviewReply(duplicateReply))
                .isInstanceOf(DuplicateException.class);
    }

    @DisplayName("답글 내용이 blank이면 작성에 실패한다.")
    @Test
    void registerReviewReply_throwsWhenContentBlank() {
        // given
        Review review = saveReview();

        // when & then
        assertThatThrownBy(() -> reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "   "
        )))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_REVIEW_REPLY_CONTENT.getMessage());
    }

    @DisplayName("답글 내용이 500자를 초과하면 작성에 실패한다.")
    @Test
    void registerReviewReply_throwsWhenContentTooLong() {
        // given
        Review review = saveReview();
        String content = "a".repeat(501);

        // when & then
        assertThatThrownBy(() -> reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                content
        )))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(ErrorCode.INVALID_REVIEW_REPLY_CONTENT.getMessage());
    }

    @DisplayName("타 매장 사장님은 답글 작성에 실패한다.")
    @Test
    void registerReviewReply_throwsWhenNotStoreOwner() {
        // given
        Review review = saveReview();
        User otherOwner = saveOwner("other-owner@test.com", "other-owner-social-id");

        // when & then
        assertThatThrownBy(() -> reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                otherOwner.getId(),
                "타 매장 답글"
        ))).isInstanceOf(InvalidRequestException.class);
    }

    @DisplayName("사장님이 본인 매장 리뷰 답글을 삭제한다.")
    @Test
    void deleteReviewReply() {
        // given
        Review review = saveReview();
        reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "삭제할 답글"
        ));

        // when
        reviewService.deleteReviewReply(new DeleteReviewReplyServiceRequest(review.getId(), owner.getId()));

        // then
        ReviewReply reply = reviewReplyRepository.findAll().get(0);
        assertThat(reply.getReplyStatus()).isEqualTo(ReviewReplyStatus.DELETED);
    }

    @DisplayName("리뷰를 삭제하면 연결된 답글도 DELETED 상태로 변경된다.")
    @Test
    void deleteReview_deletesReviewReply() {
        // given
        Review review = saveReview();
        reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "리뷰 삭제 시 함께 삭제될 답글"
        ));

        // when
        reviewService.deleteReview(new DeleteReviewServiceRequest(review.getId(), user.getId()));

        // then
        ReviewReply reply = reviewReplyRepository.findAll().get(0);
        assertThat(reply.getReplyStatus()).isEqualTo(ReviewReplyStatus.DELETED);
    }

    @DisplayName("사장님이 본인 매장 리뷰 목록을 조회한다.")
    @Test
    void getStoreReviewList() {
        // given
        Review review = saveReviewWithOrderGoods(createCompletedOrderGoods(), 3, "사장님 목록 리뷰");
        review.addReviewImage(ReviewImage.create("review-image-1.jpg", 1));
        reviewRepository.saveAndFlush(review);

        ReviewStoreServiceRequest request = new ReviewStoreServiceRequest(
                owner.getId(),
                ReviewStoreFilter.ALL,
                null,
                20
        );

        // when
        ReviewStoreResponse response = reviewService.getStoreReviewList(request);

        // then
        assertThat(response.items()).hasSize(1);
        ReviewStoreItemDTO item = response.items().get(0);
        assertThat(item.reviewId()).isEqualTo(review.getId());
        assertThat(item.rating()).isEqualTo(3);
        assertThat(item.description()).isEqualTo("사장님 목록 리뷰");
        assertThat(item.productName()).isEqualTo(goods.getName());
        assertThat(item.nickName()).isEqualTo(user.getNickName());
        assertThat(item.orderedAt()).isNotNull();
        assertThat(item.imageUrls()).containsExactly("review-image-1.jpg");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @DisplayName("사장님 리뷰 목록에서 미답변 리뷰만 조회한다.")
    @Test
    void getStoreReviewListNoReplyFilter() {
        // given
        Review repliedReview = saveReviewWithOrderGoods(createCompletedOrderGoods(), 3, "답변 완료 리뷰");
        reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                repliedReview.getId(),
                owner.getId(),
                "답변입니다."
        ));
        Review noReplyReview = saveReviewWithOrderGoods(createCompletedOrderGoods(), 2, "미답변 리뷰");

        ReviewStoreServiceRequest request = new ReviewStoreServiceRequest(
                owner.getId(),
                ReviewStoreFilter.NO_REPLY,
                null,
                20
        );

        // when
        ReviewStoreResponse response = reviewService.getStoreReviewList(request);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).reviewId()).isEqualTo(noReplyReview.getId());
        assertThat(response.items().get(0).reply()).isNull();
    }

    @DisplayName("사장님 리뷰 목록은 cursor 기반으로 다음 페이지를 조회한다.")
    @Test
    void getStoreReviewListCursorPagination() {
        // given
        Review first = saveReviewWithOrderGoods(createCompletedOrderGoods(), 1, "첫 번째 리뷰");
        Review second = saveReviewWithOrderGoods(createCompletedOrderGoods(), 2, "두 번째 리뷰");
        Review third = saveReviewWithOrderGoods(createCompletedOrderGoods(), 3, "세 번째 리뷰");

        ReviewStoreServiceRequest firstPageRequest = new ReviewStoreServiceRequest(
                owner.getId(),
                ReviewStoreFilter.ALL,
                null,
                2
        );

        // when
        ReviewStoreResponse firstPage = reviewService.getStoreReviewList(firstPageRequest);
        ReviewStoreResponse secondPage = reviewService.getStoreReviewList(new ReviewStoreServiceRequest(
                owner.getId(),
                ReviewStoreFilter.ALL,
                firstPage.nextCursor(),
                2
        ));

        // then
        assertThat(firstPage.items()).extracting(ReviewStoreItemDTO::reviewId)
                .containsExactly(third.getId(), second.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isEqualTo(second.getId());

        assertThat(secondPage.items()).extracting(ReviewStoreItemDTO::reviewId)
                .containsExactly(first.getId());
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
    }

    @DisplayName("사장님 리뷰 목록 summary 값을 조회한다.")
    @Test
    void getStoreReviewListSummary() {
        // given
        saveReviewWithOrderGoods(createCompletedOrderGoods(), 1, "1점 리뷰");
        saveReviewWithOrderGoods(createCompletedOrderGoods(), 2, "2점 리뷰");
        Review review3 = saveReviewWithOrderGoods(createCompletedOrderGoods(), 3, "3점 리뷰");
        reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review3.getId(),
                owner.getId(),
                "답변입니다."
        ));

        ReviewStoreServiceRequest request = new ReviewStoreServiceRequest(
                owner.getId(),
                ReviewStoreFilter.ALL,
                null,
                20
        );

        // when
        ReviewStoreResponse response = reviewService.getStoreReviewList(request);

        // then
        assertThat(response.summary().averageRating()).isEqualTo(2.0);
        assertThat(response.summary().totalCount()).isEqualTo(3);
        assertThat(response.summary().noReplyCount()).isEqualTo(2);
    }

    @DisplayName("고객 리뷰 조회 응답에 사장님 답글을 포함한다.")
    @Test
    void getReviewListContainsReply() {
        // given
        Review review = saveReviewWithOrderGoods(orderGoods, 2, "답글 포함 리뷰");
        reviewService.registerReviewReply(new RegisterReviewReplyServiceRequest(
                review.getId(),
                owner.getId(),
                "방문해 주셔서 감사합니다."
        ));
        ReviewListServiceRequest request = new ReviewListServiceRequest(user.getId(), goods.getId(), false, 1, 10);

        // when
        List<ReviewListDTO> reviewList = reviewService.getReviewList(request);

        // then
        assertThat(reviewList).hasSize(1);
        assertThat(reviewList.get(0).reply()).isNotNull();
        assertThat(reviewList.get(0).reply().content()).isEqualTo("방문해 주셔서 감사합니다.");
    }

    @DisplayName("고객 리뷰 조회 응답의 이미지 목록은 ReviewImage.order 기준으로 정렬된다.")
    @Test
    void getReviewListSortsImageUrlsByReviewImageOrder() {
        // given
        Review review = saveReviewWithOrderGoods(orderGoods, 2, "이미지 정렬 리뷰");
        review.addReviewImage(ReviewImage.create("second.jpg", 2));
        review.addReviewImage(ReviewImage.create("first.jpg", 1));
        review.addReviewImage(ReviewImage.create("third.jpg", 3));
        reviewRepository.saveAndFlush(review);

        ReviewListServiceRequest request = new ReviewListServiceRequest(user.getId(), goods.getId(), false, 1, 10);

        // when
        List<ReviewListDTO> reviewList = reviewService.getReviewList(request);

        // then
        assertThat(reviewList).hasSize(1);
        assertThat(reviewList.get(0).imageUrls())
                .containsExactly("first.jpg", "second.jpg", "third.jpg");
    }

    @DisplayName("사장님이 본인 매장 리뷰를 신고한다.")
    @Test
    void ownerReportsOwnStoreReview() {
        // given
        Review review = saveReviewWithOrderGoods(orderGoods, 2, "사장님 신고 리뷰");

        // when
        reviewService.reportReview(new ReportReviewServiceRequest(
                review.getId(),
                owner.getId()
        ));

        // then
        List<ReviewReportListDTO> reportList = reviewService.getReviewReportList(
                new ReviewReportListServiceRequest(review.getId(), 1, 10));
        assertThat(reportList).hasSize(1);
        assertThat(reportList.get(0).userId()).isEqualTo(owner.getId());
        assertThat(reportList.get(0).userRole()).isEqualTo(UserRole.OWNER);
    }

    @DisplayName("관리자 신고 목록에 신고자 role을 노출한다.")
    @Test
    void getReviewReportListContainsReporterRole() {
        // given
        Review review = saveReviewWithOrderGoods(orderGoods, 2, "신고자 role 리뷰");
        reviewService.reportReview(new ReportReviewServiceRequest(
                review.getId(),
                user.getId()
        ));

        // when
        List<ReviewReportListDTO> reportList = reviewService.getReviewReportList(
                new ReviewReportListServiceRequest(review.getId(), 1, 10));

        // then
        assertThat(reportList).hasSize(1);
        assertThat(reportList.get(0).userId()).isEqualTo(user.getId());
        assertThat(reportList.get(0).nickName()).isEqualTo(user.getNickName());
        assertThat(reportList.get(0).userRole()).isEqualTo(UserRole.CUSTOMER);
    }

    private Review createReview(int i) {
        CreateOrderDTO createOrderDTO = new CreateOrderDTO(user, goods, 1, 9000, LocalDateTime.now(), "test");
        Order createOrder = createOrderDTO.toOrder();
        createOrder.completed();
        order = orderRepository.save(createOrder);
        orderGoods = order.getOrderGoodsList().get(0);

        RegisterReviewDTO dto = new RegisterReviewDTO(
                order.getOrderGoodsList().get(0).getId(),
                i,
                "test",
                List.of(),
                user,
                orderGoods
        );

        return Review.create(dto);
    }

    private Review saveReview() {
        RegisterReviewDTO dto = new RegisterReviewDTO(
                orderGoods.getId(),
                2,
                "test",
                List.of(),
                user,
                orderGoods
        );
        return reviewRepository.save(Review.create(dto));
    }

    private User saveOwner(final String email, final String socialId) {
        UserSocialAccountDTO ownerAccountDTO = new UserSocialAccountDTO(
                email,
                "다른 사장님",
                "다른 사장님 닉네임",
                "01088889999",
                ProviderType.KAKAO,
                socialId,
                UserRole.OWNER
        );
        User otherOwner = ownerAccountDTO.toUser();
        otherOwner.addUserSocialAccount(ownerAccountDTO.toUserSocialAccount());
        return userRepository.save(otherOwner);
    }

    private OrderGoods createCompletedOrderGoods() {
        CreateOrderDTO createOrderDTO = new CreateOrderDTO(user, goods, 1, 9000, LocalDateTime.now(), "test");
        Order createOrder = createOrderDTO.toOrder();
        createOrder.completed();
        Order savedOrder = orderRepository.save(createOrder);
        return savedOrder.getOrderGoodsList().get(0);
    }

    private Review saveReviewWithOrderGoods(final OrderGoods targetOrderGoods, final int rating,
                                            final String description) {
        RegisterReviewDTO dto = new RegisterReviewDTO(
                targetOrderGoods.getId(),
                rating,
                description,
                List.of(),
                user,
                targetOrderGoods
        );
        return reviewRepository.saveAndFlush(Review.create(dto));
    }
}
