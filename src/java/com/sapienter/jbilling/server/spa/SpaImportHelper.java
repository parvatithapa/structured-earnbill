package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.util.db.LanguageDAS;

/**
 * Created by pablo_galera on 06/02/17.
 */
public class SpaImportHelper {

    private static final String ENGLISH_LANGUAGE_CODE = "en";
    private static final String FRENCH_LANGUAGE_CODE = "fr";

    public static Integer getLanguageId(String language) {
        String languageCode = "";
        if (SpaConstants.FRENCH_LANGUAGE.equals(language)) {
            languageCode = FRENCH_LANGUAGE_CODE;
        } else {
            languageCode = ENGLISH_LANGUAGE_CODE;
        }
        return new LanguageDAS().findByCode(languageCode).getId();
    }

    public static String getLanguageByCode(String language) {
        if (ENGLISH_LANGUAGE_CODE.equals(language)) {
            return SpaConstants.ENGLISH_LANGUAGE;
        } else {
            return SpaConstants.FRENCH_LANGUAGE;
        }
    }


}
