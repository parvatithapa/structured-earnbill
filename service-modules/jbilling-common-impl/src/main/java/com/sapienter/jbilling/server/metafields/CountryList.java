/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields;

import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

import java.util.Arrays;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author Leandro Zoi
 * @since 11-Sep-2017
 */
public class CountryList {
    private static final SortedMap<String, String> EMPTY_MAP = new TreeMap<>();
    private static final String COUNTRY_VALUE = "%s (%s)";

    private Locale localeLanguage;

    public SortedMap<String, String> getCountries(Integer languageId) {
        LanguageDTO language = new LanguageDAS().find(languageId);
        if (language != null) {
            localeLanguage = language.asLocale();
            return Arrays.stream(Locale.getISOCountries())
                         .collect(Collectors.toMap(
                             country -> country,
                             this::getISOAndCountryName,
                             (v1, v2) -> { throw new IllegalStateException(); },
                             TreeMap::new
                         ));
        } else {
            return EMPTY_MAP;
        }
    }

    private String getISOAndCountryName(String isoString){
        return String.format(COUNTRY_VALUE, isoString,
                StringEscapeUtils.unescapeHtml(new Locale("", isoString).getDisplayCountry(localeLanguage)));
    }
}
