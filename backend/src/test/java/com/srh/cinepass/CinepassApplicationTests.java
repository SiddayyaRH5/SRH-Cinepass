package com.srh.cinepass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

<<<<<<< HEAD
@SpringBootTest(properties = {
        "app.jwt.secret=test-only-cinepass-secret-key-32-bytes-minimum",
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/cinepass",
        "spring.datasource.hikari.initialization-fail-timeout=-1"
})
=======
@SpringBootTest
>>>>>>> origin/main
class CinepassApplicationTests {

	@Test
	void contextLoads() {
	}

}
