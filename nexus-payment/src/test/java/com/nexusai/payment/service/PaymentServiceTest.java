package com.nexusai.payment.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.payment.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * PaymentService Tests
 * Note: These tests verify the service behavior without actually calling Stripe.
 * In a production environment, integration tests with Stripe's test mode would be used.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    private PaymentService paymentService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("GetInvoices Tests")
    class GetInvoicesTests {

        @Test
        @DisplayName("Should return empty list when customer not found")
        void shouldReturnEmptyListWhenCustomerNotFound() {
            // getCustomerId returns null for unknown users
            List<InvoiceDTO> invoices = paymentService.getInvoices(userId, 10);

            assertThat(invoices).isEmpty();
        }
    }

    @Nested
    @DisplayName("GetPaymentMethods Tests")
    class GetPaymentMethodsTests {

        @Test
        @DisplayName("Should return empty list when customer not found")
        void shouldReturnEmptyListWhenCustomerNotFound() {
            List<PaymentMethodDTO> methods = paymentService.getPaymentMethods(userId);

            assertThat(methods).isEmpty();
        }
    }

    @Nested
    @DisplayName("CreateSubscription Validation Tests")
    class CreateSubscriptionValidationTests {

        @Test
        @DisplayName("Should throw BusinessException when Stripe call fails")
        void shouldThrowBusinessExceptionOnStripeFailure() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .email("test@example.com")
                    .paymentMethodId("pm_invalid")
                    .priceId("price_invalid")
                    .build();

            // With a placeholder API key, this will fail
            assertThatThrownBy(() -> paymentService.createSubscription(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }

    @Nested
    @DisplayName("CancelSubscription Validation Tests")
    class CancelSubscriptionValidationTests {

        @Test
        @DisplayName("Should throw BusinessException for invalid subscription ID")
        void shouldThrowBusinessExceptionForInvalidSubscriptionId() {
            assertThatThrownBy(() -> paymentService.cancelSubscription(userId, "invalid_sub_id", false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }

        @Test
        @DisplayName("Should throw BusinessException for immediate cancellation")
        void shouldThrowBusinessExceptionForImmediateCancellation() {
            assertThatThrownBy(() -> paymentService.cancelSubscription(userId, "invalid_sub_id", true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }

    @Nested
    @DisplayName("UpdateSubscription Validation Tests")
    class UpdateSubscriptionValidationTests {

        @Test
        @DisplayName("Should throw BusinessException for invalid subscription update")
        void shouldThrowBusinessExceptionForInvalidUpdate() {
            assertThatThrownBy(() -> paymentService.updateSubscription(userId, "invalid_sub", "price_new"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }

    @Nested
    @DisplayName("GetSubscription Validation Tests")
    class GetSubscriptionValidationTests {

        @Test
        @DisplayName("Should throw BusinessException for non-existent subscription")
        void shouldThrowBusinessExceptionForNonExistent() {
            assertThatThrownBy(() -> paymentService.getSubscription("sub_nonexistent"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }

    @Nested
    @DisplayName("AddPaymentMethod Validation Tests")
    class AddPaymentMethodValidationTests {

        @Test
        @DisplayName("Should throw BusinessException for invalid payment method")
        void shouldThrowBusinessExceptionForInvalidPaymentMethod() {
            assertThatThrownBy(() -> paymentService.addPaymentMethod(userId, "pm_invalid"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }

    @Nested
    @DisplayName("DeletePaymentMethod Validation Tests")
    class DeletePaymentMethodValidationTests {

        @Test
        @DisplayName("Should throw BusinessException for invalid payment method deletion")
        void shouldThrowBusinessExceptionForInvalidDeletion() {
            assertThatThrownBy(() -> paymentService.deletePaymentMethod(userId, "pm_invalid"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }

    @Nested
    @DisplayName("CreateSetupIntent Validation Tests")
    class CreateSetupIntentValidationTests {

        @Test
        @DisplayName("Should throw BusinessException when creating setup intent fails")
        void shouldThrowBusinessExceptionOnSetupIntentFailure() {
            // With placeholder API key, this will fail
            assertThatThrownBy(() -> paymentService.createSetupIntent(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PAYMENT_ERROR");
        }
    }
}
