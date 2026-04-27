package com.atomic.focus.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具：负责 access_token / refresh_token 的签发与解析。
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_IS_GUEST = "is_guest";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${atomic.jwt.secret}")
    private String secret;

    @Value("${atomic.jwt.access-token-expire-seconds}")
    private long accessExpireSeconds;

    @Value("${atomic.jwt.refresh-token-expire-seconds}")
    private long refreshExpireSeconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public String issueAccessToken(String userId, boolean isGuest) {
        return buildToken(userId, isGuest, TYPE_ACCESS, accessExpireSeconds);
    }

    public String issueRefreshToken(String userId, boolean isGuest) {
        return buildToken(userId, isGuest, TYPE_REFRESH, refreshExpireSeconds);
    }

    private String buildToken(String userId, boolean isGuest, String type, long ttlSeconds) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + ttlSeconds * 1000);
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(CLAIM_IS_GUEST, isGuest, CLAIM_TYPE, type))
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException e) {
            log.debug("Token 解析失败: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isRefreshToken(Claims claims) {
        Object typ = claims.get(CLAIM_TYPE);
        return TYPE_REFRESH.equals(typ);
    }

    public boolean isGuest(Claims claims) {
        Object v = claims.get(CLAIM_IS_GUEST);
        return v instanceof Boolean && (Boolean) v;
    }
}
