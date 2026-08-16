package dev.yunseong.website;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = {
    "s3.endpoint=http://localhost:9000",
    "s3.region=us-east-1",
    "s3.access-key=test",
    "s3.secret-key=test",
    "s3.bucket-name=test-bucket",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin",
    "spring.ai.openai.api-key=test"
})
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
