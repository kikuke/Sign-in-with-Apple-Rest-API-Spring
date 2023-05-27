package kikuke.appleauth.appleclient;

import kikuke.appleauth.auth.AppleAuthService;
import kikuke.appleauth.auth.AppleClient;
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
            //System.out.println(appleAuthService.getClaims("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));

        } catch (Exception e) {e.printStackTrace();}
    }
}
