package com.mydemos.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydemos.employee.dto.EmployeeRequest;
import com.mydemos.employee.dto.EmployeeResponse;
import com.mydemos.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void shouldCreateAndFetchEmployee() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Alice",
                "Johnson",
                "alice.johnson@example.com",
                "Engineering",
                "Senior Engineer",
                75000.0,
                true
        );

        String responseBody = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice.johnson@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        EmployeeResponse createdEmployee = objectMapper.readValue(responseBody, EmployeeResponse.class);

        assertThat(createdEmployee.id()).isNotNull();

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice.johnson@example.com"));

        mockMvc.perform(get("/api/employees/{id}", createdEmployee.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Johnson"));
    }
}
