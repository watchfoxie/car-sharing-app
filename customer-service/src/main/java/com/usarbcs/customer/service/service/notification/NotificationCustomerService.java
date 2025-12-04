package com.usarbcs.customer.service.service.notification;

import com.usarbcs.customer.service.model.NotificationCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationCustomerService {
    Page<NotificationCustomer> getNotificationsCustomerById(String customerId, Pageable pageable);

    void deleteAllNotificationByCustomerId(String customerId);
}
