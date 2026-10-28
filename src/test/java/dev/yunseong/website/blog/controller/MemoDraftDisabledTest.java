package dev.yunseong.website.blog.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = MemoDraftSecurityTest.Config.class)
@TestPropertySource(properties = "app.draft.token=")
class MemoDraftDisabledTest {
    @Autowired WebApplicationContext context;

    @Test
    void unconfiguredTokenFailsClosedEvenWithEmptyHeader() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (String path : new String[]{"/api/agent/drafts", "/api/agent/drafts/rename", "/api/agent/drafts/images"}) {
            mvc.perform(post(path).header("X-Draft-Token", ""))
                    .andExpect(status().isUnauthorized());
        }
    }
}
