package com.wholesale.marketplace.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Accepts a raw PaaS-style Postgres connection string and rewrites it into a
 * valid JDBC datasource.
 *
 * <p>Render/Heroku expose the DB as {@code postgresql://user:pass@host[:port]/db},
 * but the PostgreSQL JDBC driver requires {@code jdbc:postgresql://host:port/db}
 * with the credentials supplied separately. If {@code DATABASE_URL} / {@code DB_URL}
 * (or the resolved {@code spring.datasource.url}) is a {@code postgres(ql)://} URL,
 * this splits it into {@code spring.datasource.url/username/password} so you can
 * paste Render's "Internal Database URL" straight into the env var.
 *
 * <p>Registered in {@code META-INF/spring.factories}. Runs last so application
 * properties are already loaded; its property source is added first so it wins.
 */
public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String raw = firstNonBlank(
                env.getProperty("DATABASE_URL"),
                env.getProperty("DB_URL"),
                env.getProperty("spring.datasource.url"));
        if (raw == null) {
            return;
        }
        String lower = raw.trim().toLowerCase();
        if (!lower.startsWith("postgres://") && !lower.startsWith("postgresql://")) {
            return; // already a jdbc: URL (or composed from DB_HOST/...), leave it alone
        }

        try {
            URI uri = new URI(raw.trim());
            String user = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int sep = userInfo.indexOf(':');
                user = sep >= 0 ? userInfo.substring(0, sep) : userInfo;
                password = sep >= 0 ? userInfo.substring(sep + 1) : null;
            }
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String db = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                    .append(uri.getHost()).append(':').append(port).append('/').append(db);
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbc.append('?').append(uri.getQuery());
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc.toString());
            if (user != null) {
                props.put("spring.datasource.username", user);
            }
            if (password != null) {
                props.put("spring.datasource.password", password);
            }
            env.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", props));
        } catch (Exception ex) {
            // Leave the original value in place; Spring will report the underlying error.
            System.err.println("[RenderDatabaseUrl] Could not parse DB connection string: " + ex.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        // After ConfigDataEnvironmentPostProcessor so application.properties is loaded.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
