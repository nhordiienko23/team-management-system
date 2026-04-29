package com.nba.exception.handler;

import com.nba.exception.StaffNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.nba.controller.TeamManagerController;
import com.nba.service.TeamManager;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamManagerController.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamManager teamManager;

    @Test
    void handleNotFound_ShouldReturn404() throws Exception {
        when(teamManager.getStaffById(1)).thenThrow(new StaffNotFoundException(1));

        mockMvc.perform(get("/api/staff/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Error: Staff member with ID 1 does not exist!"));
    }

    @Test
    void handleGeneralException_ShouldReturn500() throws Exception {
        when(teamManager.getStaffById(1)).thenThrow(new RuntimeException("Oops!"));

        mockMvc.perform(get("/api/staff/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}