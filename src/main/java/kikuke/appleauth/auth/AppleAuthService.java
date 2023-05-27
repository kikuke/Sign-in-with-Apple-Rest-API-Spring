package kikuke.appleauth.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kikuke.appleauth.auth.dto.*;
import kikuke.appleauth.auth.exception.AppleAuthServiceException;
import kikuke.appleauth.jwt.JWTParser;
import kikuke.appleauth.jwt.dto.ParsedJWT;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppleAuthService {

    @Value("${apple.auth.token.alg}")
    private String secretAlg;

    @Value("${apple.auth.token.keyid}")
    private String secretKid;

    @Value("${apple.auth.token.teamid}")
    private String secretIss;

    @Value("${apple.auth.token.expiration}")
    private Long secretExp;

    @Value("${apple.auth.token.aud}")
    private String secretAud;

    @Value("${apple.auth.token.appbundleid}")
    private String secretSub;

    @Value("${apple.auth.p8.url}")
    private String p8URL;

    private final AppleClient appleClient;
    /**
     * refer to below
     * https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api/authenticating_users_with_sign_in_with_apple
     * @param getTokensDto identity_token, authorization_code
     */
    public GetTokens getTokens(GetTokensDto getTokensDto) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, AppleAuthServiceException {
        String identity_token = getTokensDto.getIdentity_token();
        String authorization_code = getTokensDto.getAuthorization_code();

        Claims idClaims = getClaims(identity_token);
        String clientSecret = getClientSecret();

        GetTokensRequestBody getTokensRequestBody = new GetTokensRequestBody(secretSub, clientSecret, authorization_code, "authorization_code");
        GetTokensResponse getTokensResponse = appleClient.getTokensResponse(getTokensRequestBody);

        if (getTokensResponse.getHttpStatus().equals(HttpStatus.BAD_REQUEST))
            throw new AppleAuthServiceException(getTokensResponse.getErrorResponse().getError());

        return new GetTokens(getTokensResponse.getTokenResponse().getAccess_token(), getTokensResponse.getTokenResponse().getRefresh_token());
    }

    public GetAccessToken getAccessToken(GetAccessTokenDto getAccessTokenDto) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, AppleAuthServiceException {
        String identity_token = getAccessTokenDto.getIdentity_token();
        String refresh_token = getAccessTokenDto.getRefresh_token();

        Claims idClaims = getClaims(identity_token);
        String clientSecret = getClientSecret();

        GetAccessTokenRequestBody getAccessTokenRequestBody = new GetAccessTokenRequestBody(secretSub, clientSecret, "refresh_token", refresh_token);
        GetAccessTokenResponse getAccessTokenResponse = appleClient.getAccessTokenResponse(getAccessTokenRequestBody);

        if (getAccessTokenResponse.getHttpStatus().equals(HttpStatus.BAD_REQUEST))
            throw new AppleAuthServiceException(getAccessTokenResponse.getErrorResponse().getError());

        return new GetAccessToken(getAccessTokenResponse.getTokenResponse().getAccess_token());
    }

    private String getClientSecret() throws IOException {
        Date expiration = Date.from(LocalDateTime.now().plusDays(secretExp).atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setHeaderParam("alg", secretAlg)
                .setHeaderParam("kid", secretKid)
                .setIssuer(secretIss)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expiration)
                .setAudience(secretAud)
                .setSubject(secretSub)
                .signWith(getPrivateKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    private PrivateKey getPrivateKey() throws IOException {
        ClassPathResource resource = new ClassPathResource(p8URL);
        String privateKey = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        Reader pemReader = new StringReader(privateKey);
        PEMParser pemParser = new PEMParser(pemReader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKeyInfo object = (PrivateKeyInfo) pemParser.readObject();
        return converter.getPrivateKey(object);
    }

    /**
     *
     * @param idToken getClaims target
     * @return idToken is validate, return Claims else throws
     */
    private Claims getClaims(String idToken) throws JsonProcessingException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, AppleAuthServiceException {
        ParsedJWT parsedIDToken = JWTParser.getParsedJWT(idToken);
        PublicKey publicKey = getPublicKey(parsedIDToken.getHeader().get("kid"));

        return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(idToken).getBody();
    }

    /**
     *
     * refer to below
     * https://developer.apple.com/documentation/sign_in_with_apple/fetch_apple_s_public_key_for_verifying_token_signature
     * @param kid key identifier
     * @return public key
     */
    private PublicKey getPublicKey(String kid) throws NoSuchAlgorithmException, InvalidKeySpecException, AppleAuthServiceException {
        JWKSet.Keys jwkSetKey = getJWKSetKey(kid);

        byte[] nBytes = Base64.getUrlDecoder().decode(jwkSetKey.getN());
        byte[] eBytes = Base64.getUrlDecoder().decode(jwkSetKey.getE());

        BigInteger n = new BigInteger(1, nBytes);
        BigInteger e = new BigInteger(1, eBytes);

        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(n, e);
        KeyFactory keyFactory = KeyFactory.getInstance(jwkSetKey.getKty());

        return keyFactory.generatePublic(rsaPublicKeySpec);
    }

    /**
     *
     * @param kid key identifier
     * @return success: return matched JWKSet.Keys failed: return null;
     */
    private JWKSet.Keys getJWKSetKey(String kid) throws AppleAuthServiceException {
        JWKSet jwkSet = appleClient.getJWKSet();
        if (jwkSet == null)
            throw new AppleAuthServiceException("AppleClient getJWKSet Failed!");

        Optional<JWKSet.Keys> optKey = jwkSet.getKeys().stream()
                .filter(key -> key.getKid().equals(kid))
                .findFirst();

        if (optKey.isEmpty())
            throw new AppleAuthServiceException("Cannot find kid: " + kid);
        return optKey.get();
    }
}
