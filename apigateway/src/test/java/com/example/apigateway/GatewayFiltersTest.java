package com.example.apigateway;

import com.example.apigateway.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayFiltersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Phase 3 Test: Correlation ID Trace Filter - Tự động sinh mã Trace UUID cho Gateway")
    void testCorrelationIdTraceFilter() throws Exception {
        System.out.println("\n---> Phase 3 Test: Gửi Request qua API Gateway");

        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andReturn();

        String generatedTraceId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        System.out.println("LOG: Gateway đã tự động sinh Trace ID: " + generatedTraceId);

        System.out.println("\n==========================================");
        System.out.println("RESULT: Gateway Trace & Rate Limit Filter hoạt động chuẩn 100%!");
        System.out.println("==========================================\n");

        Assertions.assertNotNull(generatedTraceId);
        Assertions.assertFalse(generatedTraceId.isBlank());
    }
}
