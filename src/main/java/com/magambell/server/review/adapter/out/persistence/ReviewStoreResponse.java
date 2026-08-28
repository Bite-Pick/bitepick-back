package com.magambell.server.review.adapter.out.persistence;

import com.magambell.server.review.app.port.out.response.ReviewStoreItemDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreSummaryDTO;
import java.util.List;

public record ReviewStoreResponse(
        ReviewStoreSummaryDTO summary,
        List<ReviewStoreItemDTO> items,
        Long nextCursor,
        Boolean hasNext
) {
}
