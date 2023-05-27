package kikuke.appleauth.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetTokensDto {
    String identity_token;
    String authorization_code;
}
