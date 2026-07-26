package com.jarl.seatforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SeatForgeApplicationTest {

    private static final String[] MODULE_CONFIGURATION_BEANS = {
            "identityModuleConfiguration",
            "eventsModuleConfiguration",
            "inventoryModuleConfiguration",
            "ordersModuleConfiguration",
            "paymentsModuleConfiguration",
            "notificationsModuleConfiguration",
            "auditModuleConfiguration",
            "sharedModuleConfiguration"
    };

    @Autowired
    private ApplicationContext context;

    @Test
    void t02_complete_context_starts_with_all_modules_in_one_application() {
        assertThat(context).isNotNull();
        assertThat(MODULE_CONFIGURATION_BEANS)
                .allSatisfy(beanName -> assertThat(context.containsBean(beanName))
                        .as("module configuration bean %s", beanName)
                        .isTrue());
    }
}
