package dev.yunseong.website.global.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

public class PersistentLoginService extends PersistentTokenBasedRememberMeServices {
    public PersistentLoginService(String key, UserDetailsService users, PersistentTokenRepository tokens) {
        super(key, users, tokens);
        setTokenValiditySeconds(30 * 24 * 60 * 60);
        setCookieCustomizer(cookie -> cookie.setAttribute("SameSite", "Lax"));
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        try {
            return super.autoLogin(request, response);
        } catch (CookieTheftException exception) {
            // Spring에서 token과 cookie를 폐기한 뒤 로그인 페이지로 이동하도록 처리한다.
            return null;
        }
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // LogoutFilter는 RememberMeAuthenticationFilter보다 먼저 실행된다.
        Authentication current = authentication == null ? autoLogin(request, response) : authentication;
        super.logout(request, response, current);
    }
}
