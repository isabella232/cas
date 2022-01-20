package org.apereo.cas.syncope.authentication;

import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.exceptions.AccountDisabledException;
import org.apereo.cas.authentication.exceptions.AccountPasswordMustChangeException;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.junit.EnabledIfPortOpen;
import org.apereo.cas.util.spring.BeanContainer;

import lombok.Cleanup;
import lombok.val;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link SyncopeAuthenticationHandlerTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("Syncope")
public class SyncopeAuthenticationHandlerTests extends BaseSyncopeTests {

    @Nested
    @SuppressWarnings("ClassCanBeStatic")
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @EnabledIfPortOpen(port = 18080)
    @SpringBootTest(classes = BaseSyncopeTests.SharedTestConfiguration.class,
        properties = "cas.authn.syncope.url=http://localhost:18080/syncope")
    public class SyncopeCoreServerTests {
        @Autowired
        @Qualifier("syncopeAuthenticationHandlers")
        private BeanContainer<AuthenticationHandler> syncopeAuthenticationHandlers;

        @Test
        public void verifyHandlerPasses() throws Exception {
            assertNotNull(syncopeAuthenticationHandlers);
            val syncopeAuthenticationHandler = syncopeAuthenticationHandlers.first();
            val credential = CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("syncopecas", "Mellon");
            val result = syncopeAuthenticationHandler.authenticate(credential);
            assertNotNull(result);
        }
    }

    @Nested
    @SuppressWarnings("ClassCanBeStatic")
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @SpringBootTest(classes = BaseSyncopeTests.SharedTestConfiguration.class,
        properties = "cas.authn.syncope.url=http://localhost:8095")
    public class SyncopeMockDataTests {

        @Autowired
        @Qualifier("syncopeAuthenticationHandlers")
        private BeanContainer<AuthenticationHandler> syncopeAuthenticationHandlers;

        @Test
        public void verifyHandlerPasses() {
            val syncopeAuthenticationHandler = syncopeAuthenticationHandlers.first();
            @Cleanup("stop")
            val webserver = startMockSever(user(), HttpStatus.OK);
            assertDoesNotThrow(() ->
                syncopeAuthenticationHandler.authenticate(
                    CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("casuser", "password")));
        }

        @Test
        public void verifyHandlerMustChangePassword() {
            val user = MAPPER.createObjectNode();
            user.put("username", "casuser");
            user.put("mustChangePassword", true);
            @Cleanup("stop")
            val webserver = startMockSever(user, HttpStatus.OK);

            val syncopeAuthenticationHandler = syncopeAuthenticationHandlers.first();
            assertThrows(AccountPasswordMustChangeException.class,
                () -> syncopeAuthenticationHandler.authenticate(
                    CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("casuser", "password")));
        }

        @Test
        public void verifyHandlerSuspended() {
            val user = MAPPER.createObjectNode();
            user.put("username", "casuser");
            user.put("suspended", true);
            @Cleanup("stop")
            val webserver = startMockSever(user, HttpStatus.OK);

            val syncopeAuthenticationHandler = syncopeAuthenticationHandlers.first();
            assertThrows(AccountDisabledException.class,
                () -> syncopeAuthenticationHandler.authenticate(
                    CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("casuser", "password")));
        }
    }
}
