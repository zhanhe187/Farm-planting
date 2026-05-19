package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import com.farm.fpms.entity.BindToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class BindTokenService {

    static final String SELECT_BY_TOKEN = "select token, pc_session_id, expires_at, used, used_ip, used_ua from ai_bind_token where token = ?";
    static final String CONSUME_SQL = "update ai_bind_token set used = 1, used_ip = ?, used_ua = ? where token = ? and used = 0 and expires_at >= ?";
    private static final int MAX_USED_IP_LENGTH = 80;
    private static final int MAX_USED_UA_LENGTH = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public BindTokenService(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    BindTokenService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public String issueToken(String pcSessionId, int ttlSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("insert into ai_bind_token(token, pc_session_id, expires_at, used) values (?, ?, ?, 0)",
                token, pcSessionId, Timestamp.from(clock.instant().plusSeconds(ttlSeconds)));
        return token;
    }

    public synchronized BindToken consume(String token) {
        return consume(token, null, null);
    }

    public synchronized BindToken consume(String token, String usedIp, String usedUa) {
        BindToken bindToken = find(token);
        if (bindToken.isUsed()) {
            throw new BusinessException("bind_token 已使用");
        }
        if (clock.instant().isAfter(bindToken.getExpiresAt())) {
            throw new BusinessException("bind_token 已过期");
        }
        String storedIp = abbreviate(usedIp, MAX_USED_IP_LENGTH);
        String storedUa = abbreviate(usedUa, MAX_USED_UA_LENGTH);
        int updated = jdbcTemplate.update(CONSUME_SQL, storedIp, storedUa, token, Timestamp.from(clock.instant()));
        if (updated == 0) {
            throw new BusinessException("bind_token 已使用或已过期");
        }
        bindToken.markUsed(storedIp, storedUa);
        return bindToken;
    }

    public boolean exists(String token) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from ai_bind_token where token = ?",
                Integer.class, token);
        return count != null && count > 0;
    }

    private BindToken find(String token) {
        List<BindToken> tokens = jdbcTemplate.query(SELECT_BY_TOKEN, new RowMapper<BindToken>() {
            @Override
            public BindToken mapRow(ResultSet rs, int rowNum) throws SQLException {
                BindToken bindToken = new BindToken(rs.getString("token"), rs.getString("pc_session_id"),
                        rs.getTimestamp("expires_at").toInstant());
                if (rs.getBoolean("used")) {
                    bindToken.markUsed(rs.getString("used_ip"), rs.getString("used_ua"));
                }
                return bindToken;
            }
        }, token);
        if (tokens.isEmpty()) {
            throw new BusinessException("bind_token 不存在");
        }
        return tokens.get(0);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
