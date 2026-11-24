package com.magambell.server.servicearea.app.port.out;

import com.magambell.server.servicearea.domain.entity.ServiceArea;

import java.util.List;

public interface ServiceAreaQueryPort {

    List<ServiceArea> findEnabledServiceAreas();
}
