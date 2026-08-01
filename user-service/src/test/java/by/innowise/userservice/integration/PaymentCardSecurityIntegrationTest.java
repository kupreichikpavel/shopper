package by.innowise.userservice.integration;

import by.innowise.userservice.TestcontainersConfiguration;
import by.innowise.userservice.repository.PaymentCardRepository;
import by.innowise.userservice.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PaymentCardSecurityIntegrationTest {

    private static final String USER_DETAILS_CACHE = "user-details";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanDatabaseAndCache() {
        Cache cache = cacheManager.getCache(USER_DETAILS_CACHE);

        if (cache != null) {
            cache.clear();
        }

        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAllowOwnerToManageOwnPaymentCard()
            throws Exception {

        long userId = createUser("card.owner@example.com");

        long cardId = createPaymentCard(
                userId,
                userJwt(userId),
                "1111222233334444"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .with(userJwt(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId));

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                userId
                        )
                                .with(userJwt(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cardId));

        mockMvc.perform(
                        put(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .with(userJwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentCardJson(
                                        "5555666677778888"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.number")
                                .value("5555666677778888")
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/payment-cards/{id}/deactivate",
                                cardId
                        )
                                .with(userJwt(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(
                        patch(
                                "/api/v1/payment-cards/{id}/activate",
                                cardId
                        )
                                .with(userJwt(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(
                        delete(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .with(userJwt(userId))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldForbidCreatingPaymentCardForAnotherUser()
            throws Exception {

        long ownerId = createUser("owner@example.com");
        long anotherUserId = createUser("another@example.com");

        mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                ownerId
                        )
                                .with(userJwt(anotherUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentCardJson(
                                        "1111222233334444"
                                ))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldForbidManagingAnotherUsersPaymentCard()
            throws Exception {

        long ownerId = createUser("owner@example.com");
        long anotherUserId = createUser("another@example.com");

        long cardId = createPaymentCard(
                ownerId,
                adminJwt(),
                "1111222233334444"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .with(userJwt(anotherUserId))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                ownerId
                        )
                                .with(userJwt(anotherUserId))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .with(userJwt(anotherUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentCardJson(
                                        "5555666677778888"
                                ))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        patch(
                                "/api/v1/payment-cards/{id}/deactivate",
                                cardId
                        )
                                .with(userJwt(anotherUserId))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        delete(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .with(userJwt(anotherUserId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOnlyAdminToSearchAllPaymentCards()
            throws Exception {

        long userId = createUser("search.owner@example.com");

        long cardId = createPaymentCard(
                userId,
                adminJwt(),
                "1111222233334444"
        );

        mockMvc.perform(
                        get("/api/v1/payment-cards")
                                .with(userJwt(userId))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/v1/payment-cards")
                                .with(adminJwt())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(cardId)
                );
    }

    private long createUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/users")
                                .with(serviceJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Pavel",
                                          "surname": "Kupreichik",
                                          "birthDate": "2006-01-01",
                                          "email": "%s"
                                        }
                                        """.formatted(email))
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number userId = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );

        return userId.longValue();
    }

    private long createPaymentCard(
            long userId,
            RequestPostProcessor authentication,
            String number
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                userId
                        )
                                .with(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentCardJson(number))
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number cardId = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );

        return cardId.longValue();
    }

    private String paymentCardJson(String number) {
        return """
                {
                  "number": "%s",
                  "holder": "PAVEL KUPREICHIK",
                  "expirationDate": "2030-12-31"
                }
                """.formatted(number);
    }

    private RequestPostProcessor userJwt(long userId) {
        return jwt()
                .jwt(builder ->
                        builder.claim(
                                "userId",
                                List.of(Long.toString(userId))
                        )
                )
                .authorities(
                        new SimpleGrantedAuthority("ROLE_USER")
                );
    }

    private RequestPostProcessor serviceJwt() {
        return jwt()
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_SERVICE"
                        )
                );
    }

    private RequestPostProcessor adminJwt() {
        return jwt()
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                );
    }
}
