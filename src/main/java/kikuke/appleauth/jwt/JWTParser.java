package kikuke.appleauth.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kikuke.appleauth.jwt.dto.ParsedJWT;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;


public class JWTParser {

    public static ParsedJWT getParsedJWT(String jwt) throws JsonProcessingException, UnsupportedEncodingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String[] splitJwt = jwt.split("\\.", 3);

        Map<String, String> header = objectMapper.readValue(new String(Base64.getDecoder().decode(splitJwt[0]), "UTF-8"), Map.class);
        Map<String, String> payload = objectMapper.readValue(new String(Base64.getDecoder().decode(splitJwt[1]), "UTF-8"), Map.class);
        String signature = splitJwt[2];

        return new ParsedJWT(header, payload, signature);
    }
}
