package org.doogie.liquido;

import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.doogie.liquido.matchers.UserMatcher.userWithEMail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LiquidoBackendSpringApplicationTests {

	@Test
	public void contextLoads() {
	}

}
