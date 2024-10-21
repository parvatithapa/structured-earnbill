package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.server.user.EntityBL;
import org.codehaus.groovy.grails.web.util.WebUtils;

public class ColorConverter {

//    public static final String BASE_GREEN = "#8ec549";
//    public static final Integer[] BASE_COLOR = new Integer[] {0Xc5, 0Xc4, 0X11};
    public static Integer[] BASE_COLOR;
    private static Integer BASE_COLOR_INT_DEFAULT = 24704;
    private static Integer BASE_COLOR_INT = BASE_COLOR_INT_DEFAULT;

    public static final Integer[] DIFF_BASE = new Integer[] {0x0, 0x0, 0x0};
    public static final Integer[] DIFF_PRIMARY_DARKER = new Integer[] {0x00, 0x13, 0x1a}; //background, text shadow
    public static final Integer[] DIFF_PRIMARY_LIGHTER = new Integer[] {-0x48, -0x2d, -0x24}; //box shadow

    public static final Integer[] DIFF_PRIMARY_SHADOW = new Integer[] {0x0, 0x0b, 0x09};

    public static final Integer[] DIFF_PRIMARY_HOVER_BACKGROUND = new Integer[] {0x00, -0x0b, -0x0f};
    public static final Integer[] DIFF_PRIMARY_HOVER_BORDER = new Integer[] {0x0, 0x1d, 0x26};
    public static final Integer[] DIFF_PRIMARY_HOVER_BOX_SHADOW = new Integer[] {-0x66, -0x40, -0x33};

    public static final Integer[] DIFF_SECONDARY_BACKGROUND = new Integer[] {0x0, -0x3a, -0x3f};  //#009abf
    public static final Integer[] DIFF_SECONDARY_BORDER = new Integer[] {0x0, -0x1b, -0x19};
    public static final Integer[] DIFF_SECONDARY_BOX_SHADOW = new Integer[] {-0x67, -0x63, -0x59};
    public static final Integer[] DIFF_SECONDARY_BOX_SHADOW2 = new Integer[] {0x0, 0x04, 0x0d};

    public static final Integer[] DIFF_SECONDARY_HOVER_BACKGROUND = new Integer[] {0x0, -0x46, -0x4e};
    public static final Integer[] DIFF_SECONDARY_HOVER_BORDER = new Integer[] {0x0, -0x0c, -0x06};
    public static final Integer[] DIFF_SECONDARY_HOVER_BOX_SHADOW = new Integer[] {-0x86, -0x6f, -0x61};

    public static final Integer[] DIFF_TABLE_HOVER_BACKGROUND = new Integer[] {-0xe6, -0x95, -0x79}; //e6f5f9
    public static final Integer[] DIFF_TABLE_HOVER_BORDER = new Integer[] {-0xbf, -0x86, -0x6f}; //bfe6ef

    public static final Integer[] DIFF_HEADER_SEARCH_HOVER = new Integer[] {-0x0e, -0x01, -0x31}; //0e61b1
    public static final Integer[] DIFF_BOX_EDIT_HOVER = new Integer[] {-0xee, -0x97, -0x7f}; //eef7ff

    public static final Integer[] DIFF_HEADER_BORDER = new Integer[] {0x43, 0x0a, 0x0a}; //{-0x42, -0xa9, -0x9a};
    public static final Integer[] DIFF_HEADER_BACKGROUND = new Integer[] {0x01, 0x01, 0x00}; //{-0x00, -0xa0, -0x80};

    public static String ifBase(String base, String notBase) {
        return (BASE_COLOR_INT == BASE_COLOR_INT_DEFAULT) ? base : notBase;
    }

    public static String convertIfNotBase(Integer[] diffHex, String def) {
        if(BASE_COLOR_INT == BASE_COLOR_INT_DEFAULT) {
            return def;
        }
        return convert(diffHex);
    }

    public static String convert(Integer[] diffHex) {
        return "#" + toHexString(BASE_COLOR[0] - diffHex[0]) + toHexString(BASE_COLOR[1] - diffHex[1]) + toHexString(BASE_COLOR[2] - diffHex[2]);
    }

    private static String toHexString(Integer hex) {
        String hexString = hex < 0 ? "00" : hex > 255 ? "ff" : Integer.toHexString(hex);

        return hexString.length() == 1 ? "0" + hexString : hexString;
    }

    private static String toRgb(Integer[] diffHex) {
        return (BASE_COLOR[0] - diffHex[0]) + "," + (BASE_COLOR[1] - diffHex[1]) + "," + (BASE_COLOR[2] - diffHex[2]);
    }

    public static void setBaseColor() {
        BASE_COLOR_INT = new EntityBL((Integer) WebUtils.retrieveGrailsWebRequest().getSession().getAttribute("company_id")).getEntity().getUiColor();

        // Defaults to green  0x6080
        if (BASE_COLOR_INT == null) {
            BASE_COLOR_INT = BASE_COLOR_INT_DEFAULT;
        }

        // Set new base color
        BASE_COLOR = new Integer[] {
            (BASE_COLOR_INT >> 16) & 0xFF,
            (BASE_COLOR_INT >> 8) & 0xFF,
            (BASE_COLOR_INT >> 0) & 0xFF
        };
    }
}
