package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    static final String ISSUER = "v-mazz";

    private final JwtEncoder encoder;
    private final long ttlSeconds;

    public JwtService(JwtEncoder encoder,
                      @Value("${vmazz.jwt.expiry-minutes:120}") long expiryMinutes) {
        this.encoder = encoder;
        this.ttlSeconds = expiryMinutes * 60;
    }

    public String issue(AppUser user) {

        Instant now = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name());

        if (user.getCompany() != null) {
            claims.claim("companyId", user.getCompany().getId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}
