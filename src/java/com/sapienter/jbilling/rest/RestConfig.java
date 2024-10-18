package com.sapienter.jbilling.rest;

/**
 * @author Vojislav Stanojevikj
 * @since 13-Oct-2016.
 */
public class RestConfig {

    private final String authUsername;
    private final String authPassword;
    private final String restUrl;


    public RestConfig(String authUsername, String authPassword, String restUrl) {
        this.authUsername = authUsername;
        this.authPassword = authPassword;
        this.restUrl = restUrl;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public String getRestUrl() {
        return restUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RestConfig that = (RestConfig) o;

        if (authUsername != null ? !authUsername.equals(that.authUsername) : that.authUsername != null) return false;
        if (authPassword != null ? !authPassword.equals(that.authPassword) : that.authPassword != null) return false;
        return !(restUrl != null ? !restUrl.equals(that.restUrl) : that.restUrl != null);

    }

    @Override
    public int hashCode() {
        int result = authUsername != null ? authUsername.hashCode() : 0;
        result = 31 * result + (authPassword != null ? authPassword.hashCode() : 0);
        result = 31 * result + (restUrl != null ? restUrl.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RestConfig{" +
                "authUsername='" + authUsername + '\'' +
                ", authPassword='" + authPassword + '\'' +
                ", restUrl='" + restUrl + '\'' +
                '}';
    }
}
