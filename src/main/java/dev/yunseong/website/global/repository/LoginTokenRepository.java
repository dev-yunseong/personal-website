package dev.yunseong.website.global.repository;

import dev.yunseong.website.global.domain.LoginToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
@Transactional
public class LoginTokenRepository implements PersistentTokenRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        entityManager.persist(new LoginToken(token.getSeries(), token.getUsername(),
                token.getTokenValue(), token.getDate().toInstant()));
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        LoginToken token = entityManager.find(LoginToken.class, series);
        if (token != null) {
            token.update(tokenValue, lastUsed.toInstant());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PersistentRememberMeToken getTokenForSeries(String series) {
        LoginToken token = entityManager.find(LoginToken.class, series);
        return token == null ? null : new PersistentRememberMeToken(token.getUsername(),
                token.getSeries(), token.getToken(), Date.from(token.getLastUsed()));
    }

    @Override
    public void removeUserTokens(String username) {
        entityManager.createQuery("delete from LoginToken t where t.username = :username")
                .setParameter("username", username).executeUpdate();
    }
}
