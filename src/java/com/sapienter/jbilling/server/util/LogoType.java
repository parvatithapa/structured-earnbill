package com.sapienter.jbilling.server.util;

/**
 * Created by leandro on 24/05/17.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.groovy.grails.web.context.ServletContextHolder;

import static com.sapienter.jbilling.common.SystemProperties.isBrandingJBilling;
import static com.sapienter.jbilling.common.Util.getSysProp;

public enum LogoType {
    FAVICON("fav-entity-%d.*", "EarnBill.png", "favicon.ico", "favicon.logo.format.error",
            "entity.favicon.config.size.error", "entity.favicon.config.successful"),
    INVOICE("entity-%d.*", "", "", "invoiceDetail.logo.format.error", "entity.logo.config.size.error", "entity.logo.config.successful"),
    NAVIGATION("nav-entity-%d.*", "EarnBill.png", "logo_mark-small.png", "navigation.logo.format.error",
               "entity.navigation.config.size.error", "entity.navigation.config.successful");

    private static final List<String> EXTENSIONS = new ArrayList<>();
    static {
        EXTENSIONS.add("image/jpg");
        EXTENSIONS.add("image/jpeg");
        EXTENSIONS.add("image/png");
    }

    private static final String EXTENSIONS_AS_STRING = String.join(", ", EXTENSIONS);
    private static final long LIMIT_SIZE = (long) (5 * Math.pow(2, 20));
    private static final File LOGOS_DIR = new File(getSysProp("base_dir") + "logos/");
    private static final String EMPTY_STRING = "";
    private static final String BASE_DIR = getSysProp("base_dir") + "logos/";
    private static final String IMG_DIR = ServletContextHolder.getServletContext().getRealPath("/images/");
    private static final String NOT_FOUND_IMAGE = ServletContextHolder.getServletContext().getRealPath("/images/imageNotFound.png");

    private String pattern;
    private String jBillingImg;
    private String appDirectImg;
    private String formatErrorMessage;
    private String sizeErrorMessage;
    private String successfullyMessage;

    LogoType(String pattern, String jBillingImg, String appDirectImg, String formatErrorMessage,
             String sizeErrorMessage, String successfullyMessage) {
        this.pattern = pattern;
        this.jBillingImg = jBillingImg;
        this.appDirectImg = appDirectImg;
        this.formatErrorMessage = formatErrorMessage;
        this.sizeErrorMessage = sizeErrorMessage;
        this.successfullyMessage = successfullyMessage;
    }

    public String getPattern() {
        return pattern;
    }

    public String getJbillingImg() {
        return jBillingImg;
    }

    public String getAppDirectImg() {
        return appDirectImg;
    }

    public String getFormatErrorMessage() { return formatErrorMessage; }

    public String getSizeErrorMessage() { return sizeErrorMessage; }

    public String getSuccessfullyMessage() { return successfullyMessage; }

    public static List<String> getExtensions(){ return EXTENSIONS; }

    public static String getExtensionsAsString(){ return EXTENSIONS_AS_STRING; }

    public String getFileName(Integer entityId){
        String[] list = LOGOS_DIR.list((dir, name) -> name.matches(String.format(getPattern(), entityId)));
        if (list.length > 0) {
            return BASE_DIR + list[0];
        } else {
            return EMPTY_STRING;
        }
    }

    public File getFile(Integer entityId){
        return new File(getFileName(entityId));
    }

    public String getLogo(Integer entityId) {
        String[] list = new String[0];

        if(entityId != null) {
            list = LOGOS_DIR.list((dir, name) -> name.matches(String.format(getPattern(), entityId)));
            if (this.equals(INVOICE) && list.length == 0) {
                return NOT_FOUND_IMAGE;
            }
        }

        if (list.length > 0) {
            return BASE_DIR + list[0];
        } else if (isBrandingJBilling()) {
            return IMG_DIR + getJbillingImg();
        } else {
            return IMG_DIR + getAppDirectImg();
        }

    }

    public String getShortName(){
        if(this.equals(INVOICE)) {
            return EMPTY_STRING;
        } else if (isBrandingJBilling()) {
            return getJbillingImg();
        } else {
            return getAppDirectImg();
        }
    }

    public static File[] getImages(Integer entityId) {
        return new File[] {
            FAVICON.getFile(entityId),
            INVOICE.getFile(entityId),
            NAVIGATION.getFile(entityId)
        };
    }

    public static boolean exceedLimitSize(long size){
        return size > LIMIT_SIZE;
    }
}
