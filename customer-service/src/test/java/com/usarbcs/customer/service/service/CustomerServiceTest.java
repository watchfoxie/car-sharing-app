package com.usarbcs.customer.service.service;

import com.usarbcs.customer.service.command.CustomerCommand;
import com.usarbcs.customer.service.command.CustomerInfoUpdateCmd;
import com.usarbcs.customer.service.criteria.CustomerCriteria;
import com.usarbcs.customer.service.model.Customer;
import com.usarbcs.customer.service.repository.CustomerRepository;
import com.usarbcs.customer.service.service.customer.CustomerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


@Slf4j
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {


    /**
     * @InjectMocks, the phrase inject might be deceptive.
     * This is a Mockito utility that relieves us of the responsibility of constructing an instance of the class under test.
     * In a word, Mockito will look for a suitable public function Object() { [native code] } to create an instance of our
     * CustomerServiceImpl and pass it all mocks (there is just one).
     */
    @InjectMocks
    private CustomerServiceImpl customerService;

    /**
     * we have @Mock annotation on
     * Client class because we need client class in our test but we do not want to invoke the original class.
     * so, we 'Mock' that class which works the same as the original but is actually just a mock
     *
     */
    @Mock
    private CustomerRepository customerRepository;


    @Test
    void can_i_get_all_customer(){
        PageRequest pageRequest = PageRequest.of(0, 10);
        final List<Customer> customers = Arrays.asList(
                Customer.create(new CustomerCommand()),
                Customer.create(new CustomerCommand()),
                Customer.create(new CustomerCommand())
        );
        final Page<Customer> customers1 = new PageImpl<>(customers);
        when(customerRepository.findAll(pageRequest)).thenReturn(customers1);

        customerService.findAllByDeletedFalse(pageRequest);
        assertThat(customerRepository.findAll(pageRequest).getContent()).hasSameSizeAs(customers);
    }
    @Test
    void should_can_create_customer_with_valid_payload(){
        final CustomerCommand customerCommand = new CustomerCommand();
        customerCommand.setFirstName("john");
        customerCommand.setLastName("doe");
        customerCommand.setPassword("csadmin123");
        customerCommand.setEmail("john.doe@gmail.com");

        final Customer customer = Customer.create(customerCommand);

        when(customerRepository.save(Customer.create(customerCommand))).thenReturn(customer);
        customerService.create(customerCommand);

        assertThat(customerRepository.save(customer).getFirstName()).isEqualTo("john");
    }
    @Test
    void should_can_i_get_customer_with_id(){
        final CustomerCommand customerCommand = new CustomerCommand();
        customerCommand.setFirstName("john");
        customerCommand.setLastName("doe");
        customerCommand.setPassword("csadmin123");
        customerCommand.setEmail("john.doe@gmail.com");

        final Customer customer = Customer.create(customerCommand);
        final UUID customerId = UUID.randomUUID();
        customer.setId(customerId);
        doReturn(Optional.of(customer)).when(customerRepository).findById(customerId);

        final Customer customer1 = customerService.findById(customerId.toString());
        assertEquals(Optional.of(customer), customerRepository.findById(customerId));
        assertEquals("john", customer.getFirstName());
        assertEquals("doe", customer1.getLastName());
    }
    @Test
    void should_can_i_update_customer_with_id(){
        final CustomerCommand customerCommand = new CustomerCommand();
        customerCommand.setFirstName("john");
        customerCommand.setLastName("doe");
        customerCommand.setPassword("csadmin123");
        customerCommand.setEmail("john.doe@gmail.com");

        final Customer customer = Customer.create(customerCommand);
        final UUID customerId = UUID.randomUUID();
        customer.setId(customerId);

        final CustomerInfoUpdateCmd customerCommand1 = new CustomerInfoUpdateCmd();
        customerCommand1.setFirstName("CarSharing");
        customerCommand1.setLastName("Soft");
        customerCommand1.setEmail("carsharing.soft@gmail.com");

        doReturn(Optional.of(customer)).when(customerRepository).findById(customerId);
        when(customerRepository.save(any(Customer.class))).thenReturn(any(Customer.class));

        customerService.updateInfo(customerCommand1, customerId.toString());

        assertThat(customer.getFirstName()).isNotEmpty();
        assertEquals("CarSharing", customer.getFirstName());
    }

    @Test
    void should_can_get_get_all_by_criteria(){
        PageRequest pageRequest = PageRequest.of(0, 10);

        final CustomerCommand customerCommand = new CustomerCommand();
        customerCommand.setFirstName("john");
        customerCommand.setLastName("doe");
        customerCommand.setPassword("csadmin123");
        customerCommand.setEmail("john.doe@gmail.com");

        final CustomerCommand customerCommand1 = new CustomerCommand();
        customerCommand1.setFirstName("CarSharing");
        customerCommand1.setLastName("Soft");
        customerCommand1.setEmail("carsharing.soft@gmail.com");

        final List<Customer> customers = Arrays.asList(
                Customer.create(customerCommand),
                Customer.create(customerCommand1)
        );
        // Criteria
        final CustomerCriteria customerCriteria = new CustomerCriteria("CarSharing");

        final Page<Customer> customerPage = new PageImpl<>(customers);

        when(customerRepository.findAllByCriteria(pageRequest, customerCriteria)).thenReturn(customerPage);
        customerService.getAllByCriteria(pageRequest, customerCriteria);
        assertEquals(2, customerService.getAllByCriteria(pageRequest, customerCriteria).getSize());
    }
}
