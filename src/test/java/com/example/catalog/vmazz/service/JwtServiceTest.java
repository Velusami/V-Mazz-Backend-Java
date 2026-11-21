package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.security.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtConfig config = new JwtConfig();

    @Test
    void issuedToken_canBeDecoded_withRoleAndCompany() {
        JwtService service = new JwtService(config.jwtEncoder(SECRET), 60);

        String token = service.issue(TestData.admin(7L, TestData.company(3L)));
        Jwt jwt = config.jwtDecoder(SECRET).decode(token);

        assertEquals("admin7@x.com", jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertEquals(7L, ((Number) jwt.getClaim("uid")).longValue());
        assertEquals(3L, ((Number) jwt.getClaim("companyId")).longValue());
        assertEquals(3600, service.ttlSeconds());
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    void customerToken_hasNoCompanyClaim() {
        JwtService service = new JwtService(config.jwtEncoder(SECRET), 60);

        Jwt jwt = config.jwtDecoder(SECRET).decode(service.issue(TestData.customer(5L)));

        assertEquals("CUSTOMER", jwt.getClaimAsString("role"));
        assertNull(jwt.getClaim("companyId"));
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        JwtService service = new JwtService(config.jwtEncoder(SECRET), 60);
        String token = service.issue(TestData.customer(5L));

        assertThrows(Exception.class,
                () -> config.jwtDecoder("ffffffffffffffffffffffffffffffff").decode(token));
    }

    @Test
    void shortSecret_isRejected() {
        assertThrows(IllegalStateException.class, () -> config.jwtEncoder("too-short"));
    }
}
