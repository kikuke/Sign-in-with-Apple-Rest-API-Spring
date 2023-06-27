package kikuke.appleauth.appleclient;

import kikuke.appleauth.auth.AppleAuthService;
import kikuke.appleauth.auth.AppleClient;
import kikuke.appleauth.auth.dto.GetTokensDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AppleClientTest {
    @Autowired
    AppleAuthService appleAuthService;

    @Autowired
    AppleClient appleClient;

    @Test
    @DisplayName("Check getJWKSetTest")
    void getJWKSetTest() {
        try{
        } catch (Exception e) {e.printStackTrace();}
    }
}
