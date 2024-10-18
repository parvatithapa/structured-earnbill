package com.jbilling.test.ui;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

public interface Credentials {

    public String getLoginId ();

    public String getPassword ();

    public String getCompanyId ();

    @Immutable
    public final static class DefaultCredentials implements Credentials {

        private final String loginId;
        private final String password;
        private final String companyId;

        @Override
        public String getLoginId () {
            return loginId;
        }

        @Override
        public String getPassword () {
            return password;
        }

        @Override
        public String getCompanyId () {
            return companyId;
        }

        public DefaultCredentials (String loginId, String password, String companyId) {
            this.loginId = Objects.requireNonNull(loginId);
            this.password = Objects.requireNonNull(password);
            this.companyId = Objects.requireNonNull(companyId);
        }

        @Override
        public String toString () {
            StringBuilder builder = new StringBuilder();
            builder.append("CredentialsImpl [loginId=")
                    .append(loginId)
                    .append(", password=")
                    .append(password)
                    .append(", companyId=")
                    .append(companyId)
                    .append(", getClass()=")
                    .append(getClass())
                    .append(", hashCode()=")
                    .append(hashCode())
                    .append("]");
            return builder.toString();
        }

    }
}
