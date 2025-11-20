package com.usarbcs.driver.controller;


import com.usarbcs.driver.command.AcceptRequestCustomer;
import com.usarbcs.driver.dto.DriverDto;
import com.usarbcs.driver.dto.NotificationDriverDto;
import com.usarbcs.driver.mapper.DriverMapper;
import com.usarbcs.driver.mapper.NotificationDriverMapper;
import com.usarbcs.driver.model.Driver;
import com.usarbcs.driver.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.usarbcs.core.constants.ResourcePath.*;

@RestController
@RequestMapping(V1 + NOTIFICATIONS)
@RequiredArgsConstructor
public class NotificationDriverController {

    private final NotificationService notificationService;
    private final NotificationDriverMapper notificationDriverMapper;
    private final DriverMapper driverMapper;

    @PostMapping(REQUEST)
    public ResponseEntity<DriverDto> acceptRequest(@RequestBody final AcceptRequestCustomer acceptRequestCustomer){
        final Driver driver = notificationService.acceptRequest(acceptRequestCustomer);
        return ResponseEntity.ok(driverMapper.toDto(driver));
    }
    @GetMapping
    public ResponseEntity<Page<NotificationDriverDto>> getAllNotifications(Pageable pageable){
        return ResponseEntity.ok(notificationService.getAll(pageable).map(notificationDriverMapper::toDto));
    }
    @GetMapping(DRIVERS + "/{driverId}")
    public ResponseEntity<Page<NotificationDriverDto>> findAllByDriverId(@PathVariable("driverId") final String driverId, Pageable pageable){
        return ResponseEntity.ok(notificationService.findAllByDriverId(pageable, driverId).map(notificationDriverMapper::toDto));
    }
}
