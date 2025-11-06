package com.documenthub.service;

import com.documenthub.model.entity.MasterTemplateDefinition;
import com.documenthub.rules.engine.CustomRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service to evaluate shared document eligibility based on sharing scope and custom rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharedDocumentEligibilityService {

    private final CustomRuleEngine customRuleEngine;
    private final AccountTypeService accountTypeService;

    /**
     * Evaluate if a shared document is eligible for a given account/customer.
     *
     * @param template The master template definition
     * @param customerId The customer ID
     * @param accountId The account ID
     * @return Mono<Boolean> indicating eligibility
     */
    public Mono<Boolean> isEligible(MasterTemplateDefinition template, UUID customerId, UUID accountId) {
        if (template.getIsSharedDocument() == null || !template.getIsSharedDocument()) {
            return Mono.just(false);
        }

        String sharingScope = template.getSharingScope();
        if (sharingScope == null) {
            log.warn("Shared document {} has no sharing scope defined", template.getTemplateId());
            return Mono.just(false);
        }

        log.debug("Evaluating eligibility for template {} with scope: {}", template.getTemplateId(), sharingScope);

        switch (sharingScope.toLowerCase()) {
            case "all":
                return evaluateAllScope();

            case "credit_card_account_only":
                return evaluateCreditCardAccountOnly(accountId);

            case "digital_bank_customer_only":
                return evaluateDigitalBankCustomerOnly(customerId);

            case "enterprise_customer_only":
                return evaluateEnterpriseCustomerOnly(customerId, accountId);

            case "custom_rule":
                return evaluateCustomRule(template, customerId, accountId);

            default:
                log.warn("Unknown sharing scope: {}", sharingScope);
                return Mono.just(false);
        }
    }

    /**
     * Evaluate 'all' scope - always eligible.
     */
    private Mono<Boolean> evaluateAllScope() {
        log.debug("Evaluating 'all' scope - always eligible");
        return Mono.just(true);
    }

    /**
     * Evaluate 'credit_card_account_only' scope.
     * Include only if the accountId belongs to credit card accounts.
     */
    private Mono<Boolean> evaluateCreditCardAccountOnly(UUID accountId) {
        log.debug("Evaluating credit_card_account_only scope for account: {}", accountId);
        return accountTypeService.isCreditCardAccount(accountId)
                .doOnNext(result -> log.debug("Credit card account check result: {}", result))
                .onErrorResume(error -> {
                    log.error("Error checking credit card account status: {}", error.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Evaluate 'digital_bank_customer_only' scope.
     * Include only if the accountId belongs to digital banking customers.
     */
    private Mono<Boolean> evaluateDigitalBankCustomerOnly(UUID customerId) {
        log.debug("Evaluating digital_bank_customer_only scope for customer: {}", customerId);
        return accountTypeService.isDigitalBankCustomer(customerId)
                .doOnNext(result -> log.debug("Digital bank customer check result: {}", result))
                .onErrorResume(error -> {
                    log.error("Error checking digital bank customer status: {}", error.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Evaluate 'enterprise_customer_only' scope.
     * Include only if the accountId belongs to enterprise accounts.
     */
    private Mono<Boolean> evaluateEnterpriseCustomerOnly(UUID customerId, UUID accountId) {
        log.debug("Evaluating enterprise_customer_only scope for customer: {} and account: {}", customerId, accountId);
        return accountTypeService.isEnterpriseCustomer(customerId, accountId)
                .doOnNext(result -> log.debug("Enterprise customer check result: {}", result))
                .onErrorResume(error -> {
                    log.error("Error checking enterprise customer status: {}", error.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Evaluate 'custom_rule' scope.
     * Apply complex rules from data_extraction_schema.
     */
    private Mono<Boolean> evaluateCustomRule(MasterTemplateDefinition template, UUID customerId, UUID accountId) {
        log.debug("Evaluating custom_rule scope for template: {}", template.getTemplateId());

        String dataExtractionSchema = template.getDataExtractionSchema();
        if (dataExtractionSchema == null || dataExtractionSchema.trim().isEmpty()) {
            log.warn("Template {} has custom_rule scope but no data_extraction_schema", template.getTemplateId());
            return Mono.just(false);
        }

        return customRuleEngine.evaluate(dataExtractionSchema, customerId, accountId)
                .doOnNext(result -> log.debug("Custom rule evaluation result: {}", result))
                .onErrorResume(error -> {
                    log.error("Error evaluating custom rule: {}", error.getMessage(), error);
                    // Default to exclude on error for safety
                    return Mono.just(false);
                });
    }
}
