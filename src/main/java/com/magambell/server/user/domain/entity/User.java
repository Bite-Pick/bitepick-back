package com.magambell.server.user.domain.entity;

import com.magambell.server.common.BaseTimeEntity;
import com.magambell.server.favorite.domain.entity.Favorite;
import com.magambell.server.notification.domain.entity.FcmToken;
import com.magambell.server.order.domain.entity.Order;
import com.magambell.server.review.domain.entity.Review;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.user.app.port.in.dto.UserDTO;
import com.magambell.server.user.app.port.in.dto.UserSocialAccountDTO;
import com.magambell.server.user.domain.enums.SignupSource;
import com.magambell.server.user.domain.enums.UserRole;
import com.magambell.server.user.domain.enums.UserStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity
public class User extends BaseTimeEntity {

    @Id
    @Tsid
    @Column(name = "user_id")
    private Long id;

    private String email;

    private String password;

    private String name;

    private String nickName;

    private String phoneNumber;

    @Column(name = "signup_source", length = 30)
    @Enumerated(EnumType.STRING)
    private SignupSource signupSource;

    @Column(name = "signup_source_detail", length = 100)
    private String signupSourceDetail;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserSocialAccount> userSocialAccounts = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Store store;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Favorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<FcmToken> fcmTokens = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private User(final String email, final String password, final String name, final String nickName,
                 final String phoneNumber,
                 final SignupSource signupSource,
                 final String signupSourceDetail,
                 final UserRole userRole,
                 final UserStatus userStatus) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickName = nickName;
        this.phoneNumber = phoneNumber;
        this.signupSource = signupSource;
        this.signupSourceDetail = signupSourceDetail;
        this.userRole = userRole;
        this.userStatus = userStatus;
    }

    public static User create(final UserDTO dto) {
        return User.builder()
                .email(dto.email())
                .password(dto.password())
                .name(dto.name())
                .phoneNumber(dto.phoneNumber())
                .signupSource(dto.signupSource())
                .signupSourceDetail(dto.signupSourceDetail())
                .userRole(dto.userRole())
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    public static User createBySocial(final UserSocialAccountDTO dto) {
        return User.builder()
                .email(dto.email())
                .name(dto.name())
                .nickName(dto.nickName())
                .phoneNumber(dto.phoneNumber())
                .signupSource(null)
                .signupSourceDetail(null)
                .userRole(dto.userRole())
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    public void addUserSocialAccount(final UserSocialAccount userSocialAccount) {
        this.userSocialAccounts.add(userSocialAccount);
        userSocialAccount.addUser(this);
    }

    public void addStore(final Store store) {
        this.store = store;
        store.addUser(this);
    }

    public void withdraw() {
        this.userStatus = UserStatus.WITHDRAWN;
    }

    public void addOrder(final Order order) {
        this.orders.add(order);
    }

    public void addReview(final Review review) {
        this.reviews.add(review);
    }

    public void addFavorite(final Favorite favorite) {
        this.favorites.add(favorite);
    }

    public void editNickName(final String nickName) {
        this.nickName = nickName;
    }

    public void addFcmToken(final FcmToken fcmToken) {
        this.fcmTokens.add(fcmToken);
    }
}
