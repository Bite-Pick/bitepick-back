package com.magambell.server.servicearea.domain.repository;

import com.magambell.server.servicearea.domain.entity.ServiceArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, Long> {
    List<ServiceArea> findByActiveTrue();

    List<ServiceArea> findByActiveTrueOrderByIdAsc();
}
