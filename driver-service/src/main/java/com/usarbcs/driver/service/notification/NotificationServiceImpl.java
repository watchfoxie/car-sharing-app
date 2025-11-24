package com.usarbcs.driver.service.notification;


import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.driver.command.AcceptRequestCustomer;
import com.usarbcs.driver.model.Driver;
import com.usarbcs.driver.model.NotificationDriver;
import com.usarbcs.driver.repository.DriverRepository;
import com.usarbcs.driver.repository.NotificationDriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService{


    private final NotificationDriverRepository notificationDriverRepository;
    private final DriverRepository driverRepository;


    @Override
    public List<NotificationDriver> getNotificationsByDriverId(String driverId) {
        log.info("[+] Begin fetching driver with id {}", driverId);
        final Driver driver = fetchDriver(driverId);
        return notificationDriverRepository.findAllByDriverId(driver.getId());
    }

    @Override
    public Driver acceptRequest(final AcceptRequestCustomer acceptRequestCustomer) {
        return null;
    }
    @Override
    public Driver cancelRequest(final AcceptRequestCustomer acceptRequestCustomer){
        final Driver driver = fetchDriver(acceptRequestCustomer.getDriverId());
        log.info("[+] Driver with id {} fetched successfully", acceptRequestCustomer.getDriverId());
        final NotificationDriver notificationDriver = notificationDriverRepository.findByCustomerIdAndDriver(acceptRequestCustomer.getCustomerId(), driver);
        driver.cancelRequestNotification(notificationDriver);
        return driver;
    }
    @Override
    public Page<NotificationDriver> findAllByDriverId(Pageable pageable, String driverId) {
        final Driver driver = fetchDriver(driverId);
        log.info("Driver with id {} fetched successfully", driverId);
        return notificationDriverRepository.findAllByDriver(pageable, driver);
    }
    @Override
    public Page<NotificationDriver> getAll(Pageable pageable) {
        return notificationDriverRepository.findAll(pageable);
    }

    private Driver fetchDriver(String driverId) {
        return driverRepository.findById(parseDriverId(driverId)).orElseThrow(
                () -> new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get()));
    }

    private UUID parseDriverId(String driverId) {
        if (driverId == null) {
            throw new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get());
        }
        try {
            return UUID.fromString(driverId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get());
        }
    }
}
