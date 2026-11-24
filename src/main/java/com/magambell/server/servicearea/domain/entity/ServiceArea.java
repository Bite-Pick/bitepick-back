package com.magambell.server.servicearea.domain.entity;

import com.magambell.server.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "service_areas")
public class ServiceArea extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_area_id")
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Boolean active = true;

    @Builder(access = AccessLevel.PRIVATE)
    private ServiceArea(String label, String name, Double latitude, Double longitude, Boolean active) {
        this.label = label;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active != null ? active : true;
    }

    public static ServiceArea create(String label, String name, Double latitude, Double longitude, Boolean active) {
        return ServiceArea.builder()
                .label(label)
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .active(active)
                .build();
    }
}
