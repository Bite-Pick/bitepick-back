package com.magambell.server.servicearea.adapter;

import com.magambell.server.servicearea.domain.entity.ServiceArea;
import com.magambell.server.servicearea.domain.repository.ServiceAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServiceAreaRecommendationControllerTest {

    private static final String PATH = "/api/v1/store/map/service-areas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceAreaRepository serviceAreaRepository;

    @BeforeEach
    void setUp() {
        serviceAreaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("기존 서비스 지역 목록 API는 활성 지역 전체와 기존 응답 필드를 유지한다")
    void preservesExistingServiceAreaList() throws Exception {
        serviceAreaRepository.saveAllAndFlush(List.of(
                area("지역 1", 37.0, 127.0, true),
                area("지역 2", 37.1, 127.1, true),
                area("지역 3", 37.2, 127.2, true),
                area("지역 4", 37.3, 127.3, true),
                area("비활성 지역", 37.4, 127.4, false)));

        mockMvc.perform(get("/api/v1/service-areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].label").isString())
                .andExpect(jsonPath("$.data[0].name").isString())
                .andExpect(jsonPath("$.data[0].latitude").isNumber())
                .andExpect(jsonPath("$.data[0].longitude").isNumber());
    }

    @Test
    @DisplayName("비로그인 요청에서 활성 지역을 거리와 ID 순으로 최대 3개 반환한다")
    void recommendsNearestEnabledAreas() throws Exception {
        ServiceArea firstTie = area("동일 거리 1", 37.3, 127.0, true);
        ServiceArea secondTie = area("동일 거리 2", 37.3, 127.0, true);
        ServiceArea nearest = area("가장 가까운 지역", 37.0, 127.0, true);
        serviceAreaRepository.saveAllAndFlush(List.of(
                area("먼 지역", 38.0, 127.0, true),
                firstTie, secondTie, nearest,
                area("비활성 지역", 37.0, 127.0, false)));

        mockMvc.perform(get(PATH).param("latitude", "37").param("longitude", "127"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.serviceAreas", hasSize(3)))
                .andExpect(jsonPath("$.data.serviceAreas[0].id").value(nearest.getId()))
                .andExpect(jsonPath("$.data.serviceAreas[0].name").value("가장 가까운 지역"))
                .andExpect(jsonPath("$.data.serviceAreas[0].latitude").value(37.0))
                .andExpect(jsonPath("$.data.serviceAreas[0].longitude").value(127.0))
                .andExpect(jsonPath("$.data.serviceAreas[1].id").value(firstTie.getId()))
                .andExpect(jsonPath("$.data.serviceAreas[2].id").value(secondTie.getId()));
    }

    @Test
    @DisplayName("활성 지역이 없으면 빈 목록을 반환한다")
    void returnsEmptyWhenNoEnabledAreas() throws Exception {
        serviceAreaRepository.saveAndFlush(area("비활성 지역", 37.0, 127.0, false));

        mockMvc.perform(get(PATH).param("latitude", "37").param("longitude", "127"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceAreas", hasSize(0)));
    }

    @Test
    @DisplayName("고위도에서는 경도 2도 차이가 위도 1도 차이보다 가까울 수 있다")
    void accountsForLatitudeInDistance() throws Exception {
        serviceAreaRepository.saveAllAndFlush(List.of(
                area("북쪽 지역", 81.0, 0.0, true),
                area("동쪽 지역", 80.0, 2.0, true)));

        mockMvc.perform(get(PATH).param("latitude", "80").param("longitude", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceAreas", hasSize(2)))
                .andExpect(jsonPath("$.data.serviceAreas[0].name").value("동쪽 지역"));
    }

    @Test
    @DisplayName("날짜 변경선을 사이에 둔 지역의 거리를 계산한다")
    void calculatesDistanceAcrossDateLine() throws Exception {
        serviceAreaRepository.saveAllAndFlush(List.of(
                area("먼 지역", 0.0, 175.0, true),
                area("가까운 지역", 0.0, -179.0, true)));

        mockMvc.perform(get(PATH).param("latitude", "0").param("longitude", "179"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceAreas[0].name").value("가까운 지역"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitude", "longitude"})
    @DisplayName("필수 좌표가 누락되면 400을 반환한다")
    void rejectsMissingCoordinate(String providedParameter) throws Exception {
        mockMvc.perform(get(PATH).param(providedParameter, "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @ParameterizedTest
    @CsvSource({
            "-90.1, 0", "90.1, 0", "0, -180.1", "0, 180.1",
            "NaN, 0", "0, NaN", "Infinity, 0", "0, -Infinity",
            "invalid, 0", "0, invalid"
    })
    @DisplayName("범위를 벗어나거나 숫자 형식이 잘못된 좌표는 400을 반환한다")
    void rejectsInvalidCoordinates(String latitude, String longitude) throws Exception {
        mockMvc.perform(get(PATH).param("latitude", latitude).param("longitude", longitude))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @ParameterizedTest
    @CsvSource({"-90, -180", "90, 180", "0, 0"})
    @DisplayName("위도와 경도의 유효 범위 경계값을 허용한다")
    void acceptsCoordinateBoundaries(String latitude, String longitude) throws Exception {
        mockMvc.perform(get(PATH).param("latitude", latitude).param("longitude", longitude))
                .andExpect(status().isOk());
    }

    private ServiceArea area(String name, double latitude, double longitude, boolean active) {
        return ServiceArea.create(name, name, latitude, longitude, active);
    }
}
