package com.magambell.server.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    DUPLICATE_EMAIL("중복된 이메일 입니다."),
    DUPLICATE_STORE("매장이 이미 존재합니다."),
    DUPLICATE_NICKNAME("중복된 닉네임 입니다."),
    DUPLICATE_GOODS("이미 등록된 바이트백입니다."),
    DUPLICATE_REVIEW("이미 등록된 리뷰입니다."),
    DUPLICATE_REVIEW_REPLY("이미 등록된 리뷰 답글입니다."),
    DUPLICATE_FAVORITE("이미 즐겨찾기한 매장입니다."),
    DUPLICATE_NOTIFICATION_STORE("이미 알림 받기로한 매장입니다."),
    DUPLICATE_OPEN_REGION_REQUEST("이미 오픈 요청된 지역입니다."),

    USER_VALID_PASSWORD("비밀번호는 8~16자리의 영문 대문자와 소문자, 특수문자 숫자의 조합이어야 합니다."),
    USER_VALID_EMAIL("유효한 이메일 주소가 아닙니다."),
    USER_VALID_PHONE("휴대폰 번호는 하이픈이 없어야 하며, 10자리 또는 11자리 숫자여야 합니다."),
    USER_ROLE_NOT_ASSIGNABLE("해당 역할은 사용자에게 할당할 수 없습니다."),
    STORE_VALID_IMAGE("이미지를 등록해 주세요."),
    BANNER_VALID_IMAGE("배너를 등록해 주세요."),
    INVALID_ONLY_NUMBER("숫자만 입력해 주세요."),
    TIME_VALID("판매 시작 시간은 종료 시간보다 빨라야합니다."),
    INVALID_NICK_NAME("닉네임을 입력해 주세요."),
    INVALID_USER_ROLE("사용자 유형을 선택해 주세요."),
    INVALID_PHONE_NUMBER("휴대폰 번호를 입력해 주세요."),
    STOCK_NOT_ENOUGH("재고가 부족합니다."),
    STOCK_INVALID_QUANTITY("수량은 0보다 커야 합니다."),
    INVALID_PICKUP_TIME("유효하지 않은 픽업 시간입니다."),
    INVALID_NOW_TIME_PICKUP_TIME("픽업 시간이 지났습니다."),
    INVALID_TOTAL_PRICE("결제 금액이 올바르지 않습니다."),
    INVALID_CARD_NAME("카드를 선택해 주세요."),
    INVALID_EASY_PAY_PROVIDER("간편 결제 수단을 선택해 주세요."),
    INVALID_GOODS_OWNER("잘못된 접근입니다."),
    INVALID_STORE_OWNER("잘못된 접근입니다."),
    INVALID_PAYMENT_STATUS("사용하지 않는 값 입니다."),
    INVALID_REVIEW_RATING("아쉬워요, 적당해요, 좋아요, 최고예요 중 선택해 주세요."),
    INVALID_REJECT_REASON("선택할 수 없는 주문 거절 사유입니다."),

    INVALID_PORT_ONE_ACCESS_TOKEN("토큰 발급 실패"),
    INVALID_PAYMENT_STATUS_PAID("결제가 완료되지 않았습니다."),
    INVALID_PAYMENT_STATUS_CANCEL("결제가 취소되지 않았습니다."),
    INVALID_PAYMENT_STATUS_FAILED("결제가 실패하지 않았습니다."),
    INVALID_ORDER_STATUS_COMPLETED("주문이 완료되지 않았습니다."),
    INVALID_ORDER_STATUS("요청할 수 없는 주문 상태입니다."),

    APP_UPDATE_REQUIRED("앱 업데이트가 필요합니다."),
    APP_UPDATE_RECOMMENDED("앱 업데이트를 권장합니다."),

    ORDER_ALREADY_ACCEPTED("주문이 이미 승인됐습니다."),
    ORDER_ALREADY_REJECTED("주문이 이미 거절됐습니다."),
    ORDER_NOT_APPROVED("주문이 승인되지 않았습니다."),

    TOTAL_PRICE_NOT_EQUALS("결제 금액이 일치하지 않습니다."),
    PAYMENT_ALREADY_PROCESSED("이미 처리된 결제입니다."),
    PAYMENT_NOT_ALREADY_PROCESSED("결제 완료 처리가 안된 결제입니다."),

    USER_CUSTOMER_NO_ACCESS("사장님만 접근되는 메뉴입니다."),
    ORDER_NO_ACCESS("해당 주문에 접근할 권한이 없습니다."),

    USER_EMAIL_AUTH_CODE_NOT_EQUALS("인증번호가 일치하지 않습니다."),

    USER_EMAIL_NOT_FOUND("인증을 하지 않았습니다."),
    USER_NOT_FOUND("계정이 존재하지 않았습니다."),
    OAUTH_KAKAO_USER_NOT_FOUND("카카오 사용자 정보를 찾을 수 없습니다."),
    OAUTH_GOOGLE_USER_NOT_FOUND("구글 사용자 정보를 찾을 수 없습니다."),
    OAUTH_NAVER_USER_NOT_FOUND("네이버 사용자 정보를 찾을 수 없습니다."),
    OAUTH_APPLE_USER_NOT_FOUND("애플 사용자 정보를 찾을 수 없습니다."),
    BANK_NOT_FOUND("은행정보를 찾을 수 없습니다."),
    STORE_NOT_FOUND("매장 정보를 찾을 수 없습니다."),
    GOODS_NOT_FOUND("바이트백을 찾을 수 없습니다."),
    ORDER_NOT_FOUND("주문을 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND("주문을 찾을 수 없습니다."),
    STOCK_NOT_FOUND("재고를 찾을 수 없습니다."),
    EMAIL_NOT_FOUND("이메일을 찾을 수 없습니다."),
    FAVORITE_NOT_FOUND("즐겨찾기한 매장을 찾을 수 없습니다."),
    FCM_NOT_FOUND("알림 설정한 매장을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND("리뷰를 찾을 수 없습니다."),
    BANNER_NOT_FOUND("배너가 존재하지 않았습니다."),
    NOT_FOUND_REGION("지역 정보를 찾을 수 없습니다."),


    JWT_VERIFY_EXPIRED("인증정보가 만료 됐습니다."),
    JWT_VALIDATE_ERROR("유효한 인증정보가 아닙니다."),
    JWT_TOKEN_EMPTY("토큰값이 존재하지 않습니다."),

    FIREBASE_INIT_FAILED("Firebase 초기화에 실패했습니다."),
    FIREBASE_SEND_FAILED("Firebase 메세지 발송에 실패했습니다."),
    OAUTH_APPLE_PARSE_FAILED("사용자 정보를 가져오는데 실패했습니다."),

    REVIEW_ALREADY_REPORTED("이미 신고한 리뷰입니다."),

    ;

    
    private final String message;
}
