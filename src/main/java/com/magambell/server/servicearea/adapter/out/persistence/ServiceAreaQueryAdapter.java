package com.magambell.server.servicearea.adapter.out.persistence;

import com.magambell.server.servicearea.app.port.out.ServiceAreaQueryPort;
import com.magambell.server.servicearea.domain.entity.ServiceArea;
import com.magambell.server.servicearea.domain.repository.ServiceAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ServiceAreaQueryAdapter implements ServiceAreaQueryPort {

    private final ServiceAreaRepository serviceAreaRepository;

    @Override
    public List<ServiceArea> findEnabledServiceAreas() {
        return serviceAreaRepository.findByActiveTrueOrderByIdAsc();
    }
}
