package com.usarbcs.driverlocationservice.service;

import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.util.JSONUtil;
import com.usarbcs.driverlocationservice.command.AvailabilityCommand;
import com.usarbcs.driverlocationservice.command.DriverLocationCommand;
import com.usarbcs.driverlocationservice.dto.DriverLocationView;
import com.usarbcs.driverlocationservice.mapper.DriverLocationMapper;
import com.usarbcs.driverlocationservice.model.DriverLocation;
import com.usarbcs.driverlocationservice.payload.DriverLocationPayload;
import com.usarbcs.driverlocationservice.repository.DriverLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DriverLocationServiceImpl implements DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;
    private final DriverLocationMapper driverLocationMapper;

    @Override
    public DriverLocationDto createOrUpdate(DriverLocationCommand command) {
        log.info("[DRIVER-LOCATION] createOrUpdate payload {}", JSONUtil.toJSON(command));
        return driverLocationRepository.findByDriverId(command.getDriverId())
                .map(existing -> updateExisting(existing, command))
                .orElseGet(() -> persistNew(command));
    }

    @Override
    public DriverLocationDto update(String driverId, DriverLocationCommand command) {
        log.info("[DRIVER-LOCATION] update driverId {} payload {}", driverId, JSONUtil.toJSON(command));
        DriverLocation driverLocation = getByDriverId(driverId);
        driverLocationMapper.updateEntity(driverLocation, command);
        DriverLocation saved = driverLocationRepository.save(driverLocation);
        return driverLocationMapper.toDto(saved);
    }

    @Override
    public DriverLocationPayload ensureDriverLocation(String driverId) {
        log.info("[DRIVER-LOCATION] ensure location for driverId {}", driverId);
        DriverLocation driverLocation = driverLocationRepository.findByDriverId(driverId)
                .orElseGet(() -> driverLocationRepository.save(DriverLocation.createDefault(driverId)));
        return driverLocationMapper.toPayload(driverLocation);
    }

    @Override
    public DriverLocationDto getDetails(String driverId) {
        log.info("[DRIVER-LOCATION] fetch details for driverId {}", driverId);
        return driverLocationMapper.toDto(getByDriverId(driverId));
    }

    @Override
    public DriverLocationView getView(String driverId) {
        return driverLocationMapper.toView(getByDriverId(driverId));
    }

    @Override
    public DriverLocationDto updateAvailability(String driverId, AvailabilityCommand command) {
        DriverLocation driverLocation = getByDriverId(driverId);
        driverLocation.setAvailable(command.getAvailable());
        DriverLocation saved = driverLocationRepository.save(driverLocation);
        return driverLocationMapper.toDto(saved);
    }

    @Override
    public void deleteByDriverId(String driverId) {
        log.info("[DRIVER-LOCATION] delete driverId {}", driverId);
        driverLocationRepository.findByDriverId(driverId)
                .ifPresent(driverLocationRepository::delete);
    }

    private DriverLocation getByDriverId(String driverId) {
        return driverLocationRepository.findByDriverId(driverId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.DRIVER_LOCATION_NOT_FOUND.get()));
    }

    private DriverLocationDto persistNew(DriverLocationCommand command) {
        DriverLocation driverLocation = driverLocationMapper.toEntity(command);
        DriverLocation saved = driverLocationRepository.save(driverLocation);
        return driverLocationMapper.toDto(saved);
    }

    private DriverLocationDto updateExisting(DriverLocation driverLocation, DriverLocationCommand command) {
        driverLocationMapper.updateEntity(driverLocation, command);
        DriverLocation saved = driverLocationRepository.save(driverLocation);
        return driverLocationMapper.toDto(saved);
    }
}
