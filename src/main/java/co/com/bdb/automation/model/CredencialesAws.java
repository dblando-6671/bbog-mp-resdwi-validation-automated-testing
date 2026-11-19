package co.com.bdb.automation.model;

import co.com.bdb.automation.client.Configuracion;

/** Credenciales AWS leidas del .env (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN). */
public class CredencialesAws {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    public CredencialesAws(String accessKeyId, String secretAccessKey, String sessionToken) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken == null || sessionToken.trim().isEmpty() ? null : sessionToken.trim();
    }

    public static CredencialesAws desde(Configuracion config) {
        return new CredencialesAws(config.requerido("AWS_ACCESS_KEY_ID"), config.requerido("AWS_SECRET_ACCESS_KEY"),
                config.obtener("AWS_SESSION_TOKEN").orElse(null));
    }

    public String getAccessKeyId() { return accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public String getSessionToken() { return sessionToken; }

    @Override
    public String toString() {
        return "CredencialesAws{" + accessKeyId.substring(0, Math.min(4, accessKeyId.length())) + "****}";
    }
}
