package com.farm.fpms.service;

import com.farm.fpms.domain.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BindTokenService {

    private final Clock clock;
    private final Map<String, BindToken> tokens = new ConcurrentHashMap<String, BindToken>();

    public BindTokenService() {
        this(Clock.systemUTC());
    }

    public BindTokenService(Clock clock) {
        this.clock = clock;
    }

    public String issueToken(String pcSessionId, int ttlSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new BindToken(token, pcSessionId, clock.instant().plusSeconds(ttlSeconds)));
        return token;
    }

    public synchronized BindToken consume(String token) {
        return consume(token, null, null);
    }

    public synchronized BindToken consume(String token, String usedIp, String usedUa) {
        BindToken bindToken = tokens.get(token);
        if (bindToken == null) {
            throw new BusinessException("bind_token 不存在");
        }
        if (bindToken.isUsed()) {
            throw new BusinessException("bind_token 已使用");
        }
        if (clock.instant().isAfter(bindToken.getExpiresAt())) {
            throw new BusinessException("bind_token 已过期");
        }
        bindToken.markUsed(usedIp, usedUa);
        return bindToken;
    }

    public boolean exists(String token) {
        return tokens.containsKey(token);
    }
}
