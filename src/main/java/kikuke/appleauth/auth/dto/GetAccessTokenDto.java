package kikuke.appleauth.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAccessTokenDto {
    String identity_token;
    String refresh_token;
}
