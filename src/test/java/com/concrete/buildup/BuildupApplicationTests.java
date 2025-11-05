package com.concrete.buildup;

import com.concrete.buildup.domain.upload.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class BuildupApplicationTests {

	@MockBean
	private S3Service s3Service;

	@Test
	void contextLoads() {
	}

}
