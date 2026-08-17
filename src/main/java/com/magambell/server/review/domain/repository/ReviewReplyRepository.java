package com.magambell.server.review.domain.repository;

import com.magambell.server.review.domain.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
}
