package com.usarbcs.customer.service.service.notification;


import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.customer.service.model.Customer;
import com.usarbcs.customer.service.model.NotificationCustomer;
import com.usarbcs.customer.service.repository.CustomerRepository;
import com.usarbcs.customer.service.repository.NotificationCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationCustomerServiceImpl implements NotificationCustomerService{

    private final NotificationCustomerRepository notificationCustomerRepository;
    private final CustomerRepository customerRepository;



    @Override
    public Page<NotificationCustomer> getNotificationsCustomerById(String customerId, Pageable pageable){
        log.info("[+] Begin fetching notification customer with id {}", customerId);
        return notificationCustomerRepository.findNotificationCustomersByCustomer_Id(parseCustomerId(customerId), pageable);
    }

    @Override
    public void deleteAllNotificationByCustomerId(String customerId) {
        final Customer customer = customerRepository.findById(parseCustomerId(customerId)).orElseThrow(
                () -> new BusinessException(ExceptionPayloadFactory.CUSTOMER_NOT_FOUND.get())
        );
        customer.emptyNotification();
        customerRepository.save(customer);
    }

    private UUID parseCustomerId(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ExceptionPayloadFactory.CUSTOMER_NOT_FOUND.get());
        }
    }
}
