/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.util.db;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@TableGenerator(
        name="language_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="language",
        allocationSize = 10
)
@Table(name="language")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LanguageDTO implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(LanguageDTO.class);

    public static final int ENGLISH_LANGUAGE_ID = 1;

    public static final LanguageDTO DefaultLanguage;
    public static final Locale     DefaultLocale;
    static {
        LocaleCache = new LocaleCache();
        DefaultLanguage = new LanguageDTO();
        DefaultLanguage.setId(ENGLISH_LANGUAGE_ID);
        DefaultLanguage.setCode("en");
        DefaultLocale = DefaultLanguage.asLocale();
    }

    private static class LocaleCache {
        private Map<Integer, Locale> cache = new ConcurrentHashMap<Integer, Locale>();

        public Locale findOrCreateLocale (int id, String lang, String country) {
            Locale result = cache.get(id);
            if (result == null) {
                cache.put(id, Locale.forLanguageTag(createLanguageTag (lang, country)));
                result = cache.get(id);
            }
            return result;
        }

        public void updateLocale (int id, String lang, String country) {
            cache.put(id, Locale.forLanguageTag(createLanguageTag (lang, country)));
        }

        private String createLanguageTag (String theCode, String theCountry) {

            logger.debug("createLanguageTag ({}, {})", theCode, theCountry); 
            StringBuilder languageTag = new StringBuilder(theCode);
            if (theCountry != null) {
                languageTag.append("-").append(theCountry);
            }
            return languageTag.toString();
        }
    }
    private static LocaleCache LocaleCache;

    private int id;

    // Locale language
    private String code;
    // Locale country
    private String countryCode;

    private String description;

    public LanguageDTO() {
    }

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator = "language_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name="code", nullable=false, length=2)
    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
        updateLocale();
    }

    @Column(name="description", nullable=false, length=50)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name="country_code", nullable=true, length=2)
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        updateLocale();
    }

    @Transient
    public String getAuditKey(Serializable id) {
        return id.toString();
    }

    @Transient
    public Locale asLocale() {
        return LocaleCache.findOrCreateLocale(id, code, countryCode);
    }

    private void updateLocale() {
        LocaleCache.updateLocale(id, code, countryCode);
    }

    @Override
    public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append("LanguageDTO [id=").append(id).append(", ");
        if (code != null) {
            builder.append("code=").append(code).append(", ");
        }
        if (countryCode != null) {
            builder.append("countryCode=").append(countryCode).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description);
        }
        builder.append("]");
        return builder.toString();
    }
}
