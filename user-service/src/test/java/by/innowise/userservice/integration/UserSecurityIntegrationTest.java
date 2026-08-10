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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserSecurityIntegrationTest {

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
    void shouldAllowOwnerToReadOwnProfile() throws Exception {
        long userId = createUser("owner@example.com");

        mockMvc.perform(
                        get("/api/v1/users/{id}", userId)
                                .with(userJwt(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(
                        jsonPath("$.email")
                                .value("owner@example.com")
                );
    }

    @Test
    void shouldForbidUserFromReadingAnotherProfile()
            throws Exception {

        long firstUserId = createUser("first@example.com");
        long secondUserId = createUser("second@example.com");

        mockMvc.perform(
                        get("/api/v1/users/{id}", secondUserId)
                                .with(userJwt(firstUserId))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Access denied")
                );
    }

    @Test
    void shouldAllowAdminToReadAnyProfile() throws Exception {
        long userId = createUser("admin.target@example.com");

        mockMvc.perform(
                        get("/api/v1/users/{id}", userId)
                                .with(adminJwt())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));
    }

    @Test
    void shouldReturnUnauthorizedWithoutJwt()
            throws Exception {

        long userId = createUser("protected@example.com");

        mockMvc.perform(
                        get("/api/v1/users/{id}", userId)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidUserFromListingAllUsers()
            throws Exception {

        long userId = createUser("regular@example.com");

        mockMvc.perform(
                        get("/api/v1/users")
                                .with(userJwt(userId))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowAdminToListAllUsers() throws Exception {
        createUser("listed@example.com");

        mockMvc.perform(
                        get("/api/v1/users")
                                .with(adminJwt())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].email")
                                .value("listed@example.com")
                );
    }

    @Test
    void shouldAllowServiceToFindUserByEmail() throws Exception {
        createUser("service.lookup@example.com");

        mockMvc.perform(
                        get("/api/v1/users/by-email")
                                .param(
                                        "email",
                                        "service.lookup@example.com"
                                )
                                .with(serviceJwt())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.email")
                                .value("service.lookup@example.com")
                );
    }

    @Test
    void shouldForbidUserFromFindingUserByEmail() throws Exception {
        long userId = createUser("user.lookup@example.com");

        mockMvc.perform(
                        get("/api/v1/users/by-email")
                                .param(
                                        "email",
                                        "user.lookup@example.com"
                                )
                                .with(userJwt(userId))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Access denied")
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
