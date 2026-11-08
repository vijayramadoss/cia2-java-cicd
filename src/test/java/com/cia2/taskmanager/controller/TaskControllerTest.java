package com.cia2.taskmanager.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.main.banner-mode=off", "logging.level.root=WARN"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private void createTask(String title) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createAndListTasks() throws Exception {
        createTask("Write Dockerfile");
        createTask("Write Jenkinsfile");

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Write Dockerfile"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void createWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Title must not be empty"));
    }

    @Test
    void completeTaskUpdatesStats() throws Exception {
        createTask("Run tests");

        mockMvc.perform(put("/api/tasks/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.pending").value(0));
    }

    @Test
    void deleteTaskThenGetReturns404() throws Exception {
        createTask("Delete me");

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task 1 not found"));
    }

    @Test
    void infoShowsNameAndVersion() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CIA2 Task Manager"))
                .andExpect(jsonPath("$.version").value(not(emptyString())))
                .andExpect(jsonPath("$.host").value(not(emptyString())));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void homePageIsServed() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CIA2 Task Manager")));
    }
}
