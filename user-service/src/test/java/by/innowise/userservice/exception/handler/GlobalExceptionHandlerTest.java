package by.innowise.userservice.exception.handler;

import by.innowise.userservice.dto.user.UserCreateDto;
import by.innowise.userservice.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist()
            throws Exception {

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("99")))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void shouldReturnConflictForDataIntegrityViolation()
            throws Exception {

        mockMvc.perform(get("/test/data-conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Request conflicts with existing data"))
                .andExpect(jsonPath("$.path").value("/test/data-conflict"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnBadRequestForInvalidPathVariableType()
            throws Exception {

        mockMvc.perform(get("/test/numbers/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request contains invalid data"))
                .andExpect(jsonPath("$.path").value("/test/numbers/not-a-number"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {

        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "Pavel",
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request contains invalid data"))
                .andExpect(jsonPath("$.path").value("/test/body"));
    }

    @Test
    void shouldReturnInternalServerErrorWithoutSensitiveDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected internal server error"))
                .andExpect(jsonPath("$.path").value("/test/unexpected"))
                .andExpect(content().string(not(containsString("sensitive details"))));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        String notFound() {
            throw new UserNotFoundException(99L);
        }

        @GetMapping("/data-conflict")
        String dataConflict() {
            throw new DataIntegrityViolationException("Database constraint details");
        }

        @GetMapping("/numbers/{id}")
        String number(@PathVariable Long id) {
            return id.toString();
        }

        @PostMapping("/body")
        String body(@RequestBody UserCreateDto dto) {
            return dto.email();
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException("sensitive details");
        }
    }
}
