package by.innowise.userservice.integration;

import by.innowise.userservice.TestcontainersConfiguration;
import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class UserServiceFlowIntegrationTest {

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
        Cache cache = cacheManager.getCache(
                USER_DETAILS_CACHE
        );

        if (cache != null) {
            cache.clear();
        }

        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldProcessCompleteUserFlow() throws Exception {
        long userId = createUser(
                "integration.user@example.com"
        );

        User createdUser = userRepository.findById(userId)
                .orElseThrow();

        assertEquals("Pavel", createdUser.getName());
        assertEquals(
                "integration.user@example.com",
                createdUser.getEmail()
        );
        assertTrue(createdUser.isActive());

        mockMvc.perform(
                        get("/api/v1/users/{id}", userId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Pavel"))
                .andExpect(
                        jsonPath("$.surname")
                                .value("Kupreichik")
                );

        mockMvc.perform(
                        put("/api/v1/users/{id}", userId)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "name": "Updated",
                                          "surname": "Tester",
                                          "birthDate": "2005-05-15",
                                          "email": "updated.user@example.com"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.name")
                                .value("Updated")
                )
                .andExpect(
                        jsonPath("$.surname")
                                .value("Tester")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "updated.user@example.com"
                                )
                );

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertEquals("Updated", updatedUser.getName());
        assertEquals("Tester", updatedUser.getSurname());
        assertEquals(
                "updated.user@example.com",
                updatedUser.getEmail()
        );

        mockMvc.perform(
                        get("/api/v1/users")
                                .param("name", "Updated")
                                .param("surname", "Tester")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(userId)
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/users/{id}/deactivate",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                );

        User inactiveUser = userRepository.findById(userId)
                .orElseThrow();

        assertFalse(inactiveUser.isActive());

        mockMvc.perform(
                        get(
                                "/api/v1/users/{id}/details",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.user.id")
                                .value(userId)
                )
                .andExpect(
                        jsonPath("$.user.active")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.paymentCards")
                                .isEmpty()
                );

        mockMvc.perform(
                        delete("/api/v1/users/{id}", userId)
                )
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(userId));

        mockMvc.perform(
                        get("/api/v1/users/{id}", userId)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "User with id "
                                                + userId
                                                + " was not found"
                                )
                );
    }

    @Test
    void shouldProcessCompletePaymentCardFlow()
            throws Exception {

        long userId = createUser(
                "card.owner@example.com"
        );

        MvcResult createCardResult = mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                userId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "number": "1111222233334444",
                                          "holder": "PAVEL KUPREICHIK",
                                          "expirationDate": "2030-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.number")
                                .value("1111222233334444")
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(userId)
                )
                .andReturn();

        long cardId = extractId(createCardResult);

        PaymentCard createdCard =
                paymentCardRepository.findById(cardId)
                        .orElseThrow();

        assertEquals(
                "1111222233334444",
                createdCard.getNumber()
        );
        assertTrue(createdCard.isActive());

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(cardId)
                )
                .andExpect(
                        jsonPath("$.number")
                                .value("1111222233334444")
                );

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(cardId)
                );

        mockMvc.perform(
                        get("/api/v1/payment-cards")
                                .param(
                                        "ownerName",
                                        "Pavel"
                                )
                                .param(
                                        "ownerSurname",
                                        "Kupreichik"
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(cardId)
                );

        mockMvc.perform(
                        get(
                                "/api/v1/users/{id}/details",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paymentCards[0].id")
                                .value(cardId)
                );

        mockMvc.perform(
                        put(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "number": "5555666677778888",
                                          "holder": "UPDATED HOLDER",
                                          "expirationDate": "2032-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.number")
                                .value("5555666677778888")
                )
                .andExpect(
                        jsonPath("$.holder")
                                .value("UPDATED HOLDER")
                );

        PaymentCard updatedCard =
                paymentCardRepository.findById(cardId)
                        .orElseThrow();

        assertEquals(
                "5555666677778888",
                updatedCard.getNumber()
        );
        assertEquals(
                "UPDATED HOLDER",
                updatedCard.getHolder()
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/payment-cards/{id}/deactivate",
                                cardId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                );

        PaymentCard inactiveCard =
                paymentCardRepository.findById(cardId)
                        .orElseThrow();

        assertFalse(inactiveCard.isActive());

        mockMvc.perform(
                        get(
                                "/api/v1/users/{id}/details",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.paymentCards[0].number"
                        ).value("5555666677778888")
                )
                .andExpect(
                        jsonPath(
                                "$.paymentCards[0].active"
                        ).value(false)
                );

        mockMvc.perform(
                        delete(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                )
                .andExpect(status().isNoContent());

        assertFalse(
                paymentCardRepository.existsById(cardId)
        );

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                cardId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Payment card with id "
                                                + cardId
                                                + " was not found"
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/users/{id}/details",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paymentCards")
                                .isEmpty()
                );
    }

    private long createUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
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
                .andExpect(
                        jsonPath("$.email")
                                .value(email)
                )
                .andReturn();

        return extractId(result);
    }

    private long extractId(MvcResult result)
            throws Exception {

        Number id = JsonPath.read(
                result.getResponse()
                        .getContentAsString(),
                "$.id"
        );

        return id.longValue();
    }
}