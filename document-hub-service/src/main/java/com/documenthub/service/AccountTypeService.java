package com.documenthub.service;

import com.documenthub.service.integration.AccountServiceClient;
import com.documenthub.service.integration.CustomerServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service to determine account types and customer segments.
 * Integrates with external services to fetch account and customer metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountServiceClient accountServiceClient;
    private final CustomerServiceClient customerServiceClient;

    /**
     * Check if an account is a credit card account.
     */
    @Cacheable(value = "accountType", key = "#accountId")
    public Mono<Boolean> isCreditCardAccount(UUID accountId) {
        log.debug("Checking if account {} is a credit card account", accountId);
        return accountServiceClient.getAccountDetails(accountId)
                .map(accountDetails -> {
                    String lineOfBusiness = (String) accountDetails.get("lineOfBusiness");
                    boolean isCreditCard = "credit_card".equalsIgnoreCase(lineOfBusiness);
                    log.debug("Account {} lineOfBusiness: {}, isCreditCard: {}", accountId, lineOfBusiness, isCreditCard);
                    return isCreditCard;
                })
                .defaultIfEmpty(false);
    }

    /**
     * Check if a customer is a digital banking customer.
     */
    @Cacheable(value = "customerType", key = "#customerId + '_digital'")
    public Mono<Boolean> isDigitalBankCustomer(UUID customerId) {
        log.debug("Checking if customer {} is a digital bank customer", customerId);
        return customerServiceClient.getCustomerProfile(customerId)
                .map(customerProfile -> {
                    String customerType = (String) customerProfile.get("customerType");
                    boolean isDigital = "digital_banking".equalsIgnoreCase(customerType);
                    log.debug("Customer {} customerType: {}, isDigital: {}", customerId, customerType, isDigital);
                    return isDigital;
                })
                .defaultIfEmpty(false);
    }

    /**
     * Check if a customer/account is an enterprise customer.
     */
    @Cacheable(value = "customerType", key = "#customerId + '_' + #accountId + '_enterprise'")
    public Mono<Boolean> isEnterpriseCustomer(UUID customerId, UUID accountId) {
        log.debug("Checking if customer {} / account {} is an enterprise customer", customerId, accountId);
        return customerServiceClient.getCustomerProfile(customerId)
                .map(customerProfile -> {
                    String segment = (String) customerProfile.get("segment");
                    boolean isEnterprise = "enterprise".equalsIgnoreCase(segment);
                    log.debug("Customer {} segment: {}, isEnterprise: {}", customerId, segment, isEnterprise);
                    return isEnterprise;
                })
                .defaultIfEmpty(false);
    }

    /**
     * Get account line of business.
     */
    @Cacheable(value = "accountLOB", key = "#accountId")
    public Mono<String> getLineOfBusiness(UUID accountId) {
        log.debug("Getting line of business for account {}", accountId);
        return accountServiceClient.getAccountDetails(accountId)
                .map(accountDetails -> (String) accountDetails.get("lineOfBusiness"))
                .defaultIfEmpty("unknown");
    }
}
