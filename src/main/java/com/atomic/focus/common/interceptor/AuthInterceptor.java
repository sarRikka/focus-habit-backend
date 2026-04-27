package com.atomic.focus.common.interceptor;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器：解析 Authorization 头并写入 UserContext。
 * 白名单：/auth/*、actuator 等。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String token = header.substring(PREFIX.length()).trim();
        try {
            Claims claims = jwtUtil.parse(token);
            if (jwtUtil.isRefreshToken(claims)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "应使用 access_token 访问业务接口");
            }
            UserContext.set(claims.getSubject(), jwtUtil.isGuest(claims));
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
