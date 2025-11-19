package com.concrete.buildup;

import com.concrete.buildup.domain.upload.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.driver-class-name=org.h2.Driver"
})
class BuildupApplicationTests {

	@MockBean
	private S3Service s3Service;

	@Test
	void contextLoads() {
	}

}
