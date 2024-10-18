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

package com.sapienter.jbilling.common;

import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Client miscelaneous utility functions
 */
public class Util {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public final static int SECONDS_IN_MINUTE = 60;
    public final static int MILISECONDS_IN_SECOND = 1000;

    /**
     * Creates a date object with the given parameters only if they belong to a valid day, so February 30th would be
     * returning null.
     *
     * @param year
     * @param month
     * @param day
     * @return null if the parameters are invalid, otherwise the date object
     */
    static public Date getDate (Integer year, Integer month, Integer day) {
        Date retValue = null;
        try {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setLenient(false);
            cal.clear();
            cal.set(year, month - 1, day);

            retValue = cal.getTime();
        } catch (Exception e) {

        }
        return retValue;
    }

    /**
     * Converts a string in the format yyyy-mm-dd to a Date. If the string can't be converted, it returns null
     *
     * @param str
     * @return
     */
    static public Date parseDate (String str) {
        if (str == null || str.length() < 8 || str.length() > 10) {
            return null;
        }

        if (str.charAt(4) != '-' || str.lastIndexOf('-') < 6 || str.lastIndexOf('-') > 7) {
            return null;
        }

        try {
            int year = getYear(str);
            int month = getMonth(str);
            int day = getDay(str);

            return getDate(new Integer(year), new Integer(month), new Integer(day));
        } catch (Exception e) {
            return null;
        }
    }

    public static Date addDays (Date date, int days) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime((null == date) ? new Date() : date);
        calendar.add(GregorianCalendar.DATE, days);
        return calendar.getTime();
    }

    /**
     * Recives date in sql format yyyy-mm-dd and extracts the day
     * @param str
     * @return
     */
    static public int getDay (String str) throws SessionInternalError {
        // from the last '-' to the end
        try {
            return Integer.valueOf(str.substring(str.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            throw new SessionInternalError("Cant get the day from " + str);
        }
    }

    static public int getMonth (String str) throws SessionInternalError {
        // from the first '-' to the second '-'
        try {
            return Integer.valueOf(str.substring(str.indexOf('-') + 1, str.lastIndexOf('-')));
        } catch (NumberFormatException e) {
            throw new SessionInternalError("Cant get the month from " + str);
        }

    }

    static public int getYear (String str) throws SessionInternalError {
        // from the begining to the first '-'
        try {
            return Integer.valueOf(str.substring(0, str.indexOf('-')));
        } catch (NumberFormatException e) {
            throw new SessionInternalError("Cant get the year from " + str);
        }
    }

    /**
     * Compares to dates, contemplating the posibility of null values. If both are null, they are consider equal.
     *
     * @param date1
     * @param date2
     * @return true if equal, otherwise false.
     */

    static public boolean equal (Date date1, Date date2) {
        boolean retValue;
        if (date1 == null && date2 == null) {
            retValue = true;
        } else if ((date1 == null && date2 != null) || (date1 != null && date2 == null)) {
            retValue = false;
        } else {
            retValue = (date1.compareTo(date2) == 0);
        }

        return retValue;
    }

    static public Date truncateDate (Date arg) {
        if (arg == null)
            return null;
        GregorianCalendar cal = new GregorianCalendar();

        cal.setTime(arg);
        cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
        cal.set(GregorianCalendar.MINUTE, 0);
        cal.set(GregorianCalendar.SECOND, 0);
        cal.set(GregorianCalendar.MILLISECOND, 0);

        return cal.getTime();
    }

    /**
     * Takes a date and returns it as String with the format 'yyyy-mm-dd'
     *
     * @param date
     * @return
     */
    static public String parseDate (Date date) {
        GregorianCalendar cal = new GregorianCalendar();

        cal.setTime(date);
        return cal.get(GregorianCalendar.YEAR) + "-" + (cal.get(GregorianCalendar.MONTH) + 1) + "-"
                + cal.get(GregorianCalendar.DATE);
    }

    /**
     * Checks if the passed string can be converted into a date.
     * If the string can't be converted, it returns false
     * @param str : String to be parsed
     * @param df  : Date Time Formatter in which parsing would be tried
     * @return true: if str can be converted to date, false otherwise
     */
    static public boolean canParseDate(String str, DateTimeFormatter df) {
        try {
            logger.debug("Trying to parse {} to date in format {}", str,  df);
            df.parseDateTime(str);
        } catch (IllegalArgumentException e) {
            logger.debug("Cannot parse the given {} into a date for format {}",str,df);
            // Eat the Exception & Leave
            return false;
        }
        return true;
    }

    /**
     * Return true if the OS is Unix/Linux
     * @return
     */
    public static boolean isUnix() {
        String osName = System.getProperty("os.name");
        if (osName == null) {
            throw new SessionInternalError("os.name not found");
        }
        osName = osName.toLowerCase(Locale.ENGLISH);

        return (osName.contains("linux")
                || osName.contains("mpe/ix")
                || osName.contains("freebsd")
                || osName.contains("irix")
                || osName.contains("digital unix")
                || osName.contains("unix"));
    }


    /**
     * Executes a command on the OS.
     *
     * @param command - Command to execute
     * @param workingDir - Working directory
     * @return command output
     */
    public static String executeCommand(String[] command, File workingDir) {
        return executeCommand(command, workingDir, false);
    }
    /**
     * Executes a command on the OS.
     *
     * @param command - Command to execute
     * @param workingDir - Working directory
     * @param trimOutput - trim the output value
     *
     * @return command output
     */
    public static String executeCommand(String[] command, File workingDir, boolean trimOutput) {
        StringBuilder output = new StringBuilder();

        Process p;
        try {
            StringBuilder stdOut = new StringBuilder();
            StringBuilder stdErr = new StringBuilder();

            p = Runtime.getRuntime().exec(command, null, workingDir);

            Thread stdoutGobbler = new Thread(new StdInputStreamGobbler(p.getInputStream(), stdOut));
            Thread stderrGobbler = new Thread(new StdInputStreamGobbler(p.getErrorStream(), stdErr));

            stdoutGobbler.start();
            stderrGobbler.start();

            p.waitFor(360, TimeUnit.SECONDS);

            stdoutGobbler.join();
            stderrGobbler.join();

            if (trimOutput) {
                output.append(StringUtils.trim(stdOut.toString()));
            } else {
                output.append("[Output]").append(stdOut);
                output.append("[Error]").append(stdErr);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return output.toString();
    }

    /**
     * Extract a Zip file to a folder
     * @param zipFile - file to extract.
     * @param targetFolder - including path separator
     * @return Nr of files in zip
     * @throws Exception
     */
    public static int extractZipFile(File zipFile, File targetFolder) throws Exception {
        int entries = 0;
        try (ZipInputStream zipIs = new ZipInputStream(new FileInputStream(zipFile))) {
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry;
            while((zipEntry = zipIs.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                File newFile = new File(targetFolder, fileName);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zipIs.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                entries++;
            }
        }
        return entries;
    }

    public static String gpgDecrypt(String encryptedFile, String decryptedFile, String passphrase) throws Exception {
        ArrayList<String> command = new ArrayList<>();
        command.add("gpg");
        if(passphrase != null && passphrase.length() > 0) {
            command.add("--passphrase");
            command.add(passphrase);
        }
        command.add("--output");
        command.add(decryptedFile);
        command.add("--batch");
        command.add("--no-tty");
        command.add("--decrypt");
        command.add(encryptedFile);

        logger.debug("Decrypting: {}", encryptedFile);

        String output = executeCommand(command.toArray(new String[command.size()]), null);
        return output;
    }

    public static void gzip(String dir) {
        if (SystemUtils.IS_OS_UNIX) {
            gzipInUnix(dir);
        } else {
            Path path = Paths.get(dir);
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path,
                    entry -> !entry.getFileName().toString().endsWith(".gz"))) {

                StreamSupport.stream(dirStream.spliterator(), false)
                    .forEach(p -> {
                        try {
                            Path out = Paths.get(p.toString() + ".gz");
                            try (
                                    FileInputStream fis = new FileInputStream(p.toFile());
                                    GZIPOutputStream gzipOS = new GZIPOutputStream(Files.newOutputStream(out))
                            ) {
                                final byte[] bytes = new byte[1024];
                                int length;
                                while ((length = fis.read(bytes)) >= 0) {
                                    gzipOS.write(bytes, 0, length);
                                }
                            }
                            Files.delete(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void gzipInUnix(String dir) {
        executeCommand(new String[] {
                "sh", "-c", "gzip -q *"
        }, new File(dir));
    }

    /**
     * Delete a file or folder recursively.
     *
     * @param file
     * @throws Exception
     */
    public static void deleteRecursive(File file) throws Exception {
        if(file.exists()) {
            if(file.isFile()) {
                file.delete();
            } else if(file.isDirectory()) {
                for(File f : file.listFiles()) {
                    deleteRecursive(f);
                }
                file.delete();
            }
        }
    }

    /**
     * Calculate the MD5 checksum of a file.
     *
     * @param file
     * @return MD5 checksum as byte array
     * @throws Exception
     */
    public static byte[] calcMD5ChecksumByte(File file) throws Exception {
        InputStream fis =  new FileInputStream(file);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    /**
     * Calculate the MD5 checksum of a file.
     *
     * @param file
     * @return MD5 checksum as String
     * @throws Exception
     */
    public static String calcMD5Checksum(File file) throws Exception {
        byte[] b = calcMD5ChecksumByte(file);
        StringBuilder result = new StringBuilder();

        for (int i=0; i < b.length; i++) {
            result.append(Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 ));
        }
        return result.toString();
    }

    /**
     * Returns the payment method for the given credit card. If this credit card has been obscured (by the
     * { com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask} plug-in) then the payment type
     * cannot detected and this method will return PAYMENT_METHOD_GATEWAY_KEY.
     *
     * @param creditCardNumber
     *            credit card number to parse
     * @return payment method
     */
    static public Integer getPaymentMethod (char[] creditCardNumber) {
        Integer type = null;

        if (creditCardNumber.length > 0) {
            switch (creditCardNumber[0]) {
            case '4':
                String comparisonValue = new String(creditCardNumber, 0, 4);

                if (comparisonValue.equals("4026") || new String(creditCardNumber, 0, 6).equals("417500")
                        || comparisonValue.equals("4508") || comparisonValue.equals("4844")
                        || comparisonValue.equals("4913") || comparisonValue.equals("4917")) {
                    type = Constants.PAYMENT_METHOD_VISA_ELECTRON;
                } else {
                    type = Constants.PAYMENT_METHOD_VISA;
                }
                break;
            case '5':
                switch (creditCardNumber[1]) {
                case '1':
                case '2':
                case '3':
                case '4': // DINERS for US & Canada = MASTERCARD International
                case '5': // DINERS for US & Canada = MASTERCARD International
                    type = Constants.PAYMENT_METHOD_MASTERCARD;
                    break;
                case '0':
                    String comparisonValueCaseFive = new String(creditCardNumber, 2, 2);

                    if (comparisonValueCaseFive.equals("18") || comparisonValueCaseFive.equals("20")
                            || comparisonValueCaseFive.equals("38")) {
                        type = Constants.PAYMENT_METHOD_MAESTRO;
                    }
                    break;
                }
                break;
            case '3':
                // both diners and american express start with a 3
                if (creditCardNumber[1] == '7' || creditCardNumber[1] == '4') {
                    type = Constants.PAYMENT_METHOD_AMEX;
                } else if (creditCardNumber[1] == '5') {
                    try {
                        String comparisonValueCaseThree = new String(creditCardNumber, 0, 4);
                        int startNumber = Integer.valueOf(comparisonValueCaseThree);
                        if (startNumber >= 3528 && startNumber <= 3589) {
                            type = Constants.PAYMENT_METHOD_JCB;
                        }
                    } catch (Exception ex) {
                        // do nothing
                    }
                } else if (creditCardNumber[1] == '0' || creditCardNumber[1] == '6') {
                    type = Constants.PAYMENT_METHOD_DINERS;
                }
                break;
            case '6':
                String comparisonValueThree = new String(creditCardNumber, 0, 3);
                String comparisonValueFour = new String(creditCardNumber, 0, 4);

                if (comparisonValueThree.equals("637") || comparisonValueThree.equals("638")
                        || comparisonValueThree.equals("639")) {
                    type = Constants.PAYMENT_METHOD_INSTAL_PAYMENT;
                } else if (comparisonValueFour.equals("6304")
                        || // also LASER card
                        comparisonValueFour.equals("6759") || comparisonValueFour.equals("6761")
                        || comparisonValueFour.equals("6762") || comparisonValueFour.equals("6763")) {
                    type = Constants.PAYMENT_METHOD_MAESTRO;
                } else if (comparisonValueFour.equals("6706") || comparisonValueFour.equals("6771")
                        || comparisonValueFour.equals("6709")) {
                    type = Constants.PAYMENT_METHOD_LASER;
                } else if (comparisonValueFour.equals("6011") || new String(creditCardNumber, 0, 2).equals("65")
                        || comparisonValueThree.equals("644") || comparisonValueThree.equals("645")
                        || comparisonValueThree.equals("646") || comparisonValueThree.equals("647")
                        || comparisonValueThree.equals("648") || comparisonValueThree.equals("649")
                        || comparisonValueThree.equals("622")) {
                    if (comparisonValueThree.equals("622")) {
                        try {
                            int startNumber = Integer.valueOf(new String(creditCardNumber, 0, 6));
                            if (startNumber >= 622126 && startNumber <= 622925) {
                                type = Constants.PAYMENT_METHOD_DISCOVER;
                            }
                        } catch (Exception ex) {
                            // do nothing
                        }
                    } else {
                        type = Constants.PAYMENT_METHOD_DISCOVER;
                    }
                }
                break;
            }
        }
        /*
         * This isn't 100% accurate as obscured credit card numbers may not always mean that a gateway key is present.
         * We should be checking CreditCardDTO to ensure that gatewayKey is not null when an obscured credit card number
         * is encountered.
         */
        for(char value : creditCardNumber){
            if (value == '*' || (value == '.' && type == null)){
                type = Constants.PAYMENT_METHOD_GATEWAY_KEY;
                break;
            }
        }

        return type;
    }

    static public String truncateString (String str, int length) {
        if (str == null)
            return null;
        String retValue;
        if (str.length() <= length) {
            retValue = str;
        } else {
            retValue = str.substring(0, length);
        }

        return retValue;
    }

    public static String getSysProp (String key) {
        try {
            return SystemProperties.getSystemProperties().get(key);
        } catch (Exception e) {
            logger.error("Cannot read property '{}' from {}", key, SystemProperties.PROPERTIES_FILE, e);
            return null;
        }
    }

    public static Map<String, String> getMatchingProp(String regex) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            map = SystemProperties.getSystemProperties().matchingProp(regex);
        } catch (Exception e) {
            logger.debug("Caught Exception While trying to get matching properties  " + e.getMessage());
        }
        return map;
    }

    /**
     * Gets a boolean system property. It returns true by default, and on any error.
     *
     * @param key
     *            boolean system property
     * @return boolean property value
     */
    public static boolean getSysPropBooleanTrue (String key) {
        try {
            return Boolean.parseBoolean(SystemProperties.getSystemProperties().get(key, "true"));
        } catch (Exception e) {
            logger.error("Cannot read property '{}' from {}", key, SystemProperties.PROPERTIES_FILE);
        }

        return true; // default if not found
    }

    /**
     * Calculate the Luhn check digit for the string
     * @param number
     * @return
     */
    public static int calcLuhnCheckDigit(String number) {
        int sum = 0;

        //loop through all digits in the number
        for (int i = 0; i < number.length(); i++) {

            //get the current digit
            int digit = Integer.valueOf(number.substring(i, i + 1));

            //if number is even
            if ((i % 2) == 0) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = 1 + (digit % 10);
                }
            }
            sum += digit;
        }

        //Calculate required digit to make sum % 10 == 0
        int mod = sum % 10;
        return ((mod == 0) ? 0 : 10 - mod);
    }

    /**
     * Credit Card Validate Reference: http://www.ling.nwu.edu/~sburke/pub/luhn_lib.pl
     */
    public static boolean luhnCheck (String cardNumber) throws SessionInternalError {
        // just in case the card number is formated and may contain spaces
        cardNumber = getDigitsOnly(cardNumber);
        // mod 10 validation
        if (isLuhnNum(cardNumber)) {
            int no_digit = cardNumber.length();
            int oddoeven = no_digit & 1;

            int sum = 0;
            int digit = 0;
            int addend = 0;
            boolean timesTwo = false;
            for (int i = cardNumber.length() - 1; i >= 0; i--) {
                digit = Integer.parseInt(cardNumber.substring(i, i + 1));
                if (timesTwo) {
                    addend = digit * 2;
                    if (addend > 9) {
                        addend -= 9;
                    }
                } else {
                    addend = digit;
                }
                sum += addend;
                timesTwo = !timesTwo;
            }
            if (sum == 0)
                return false;
            if (sum % 10 == 0)
                return true;
        }
        ;
        return false;
    }

    private static String getDigitsOnly (String s) {
        StringBuffer digitsOnly = new StringBuffer();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (Character.isDigit(c)) {
                digitsOnly.append(c);
            }
        }
        return digitsOnly.toString();
    }

    private static boolean isLuhnNum (String argvalue) {
        if (argvalue.length() == 0) {
            return false;
        }
        for (int n = 0; n < argvalue.length(); n++) {
            char c = argvalue.charAt(n);
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static BigDecimal parseBigDecimal(String data) {
        if (data == null) {
            return null;
        }
        try {
            return new BigDecimal(data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseInteger(String data) {
        if (data == null) {
            return null;
        }
        try {
            return Integer.parseInt(data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Date parseDate(Long data) {
        if (data == null) {
            return null;
        }

        return new Date(data);
    }

    public static Long parseLong(String data) {
        if (data == null) {
            return null;
        }
        try {
            return Long.parseLong(data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * logback style ("... {} ...") message parameters support. based on log4j LogSF implementation
     *
     * @param pattern
     * @param argument
     * @return
     */
    public static String S (final String pattern, final Object argument) {
        if (pattern != null) {
            //
            // if there is an escaped brace, delegate to multi-param formatter
            if (pattern.indexOf("\\{") >= 0) {
                return S(pattern, new Object[] { argument });
            }
            int pos = pattern.indexOf("{}");
            if (pos >= 0) {
                return pattern.substring(0, pos) + argument + pattern.substring(pos + 2);
            }
        }
        return pattern;
    }

    /**
     * logback style ("... {} ...") message parameters support. based on log4j LogSF implementation. multi-param
     * version.
     *
     * @param pattern
     * @param arguments
     * @return
     */
    public static String S (final String pattern, final Object... arguments) {
        if (pattern != null) {
            String retval = "";
            int count = 0;
            int prev = 0;
            int pos = pattern.indexOf("{");
            while (pos >= 0) {
                if (pos == 0 || pattern.charAt(pos - 1) != '\\') {
                    retval += pattern.substring(prev, pos);
                    if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '}') {
                        if (arguments != null && count < arguments.length) {
                            retval += arguments[count++];
                        } else {
                            retval += "{}";
                        }
                        prev = pos + 2;
                    } else {
                        retval += "{";
                        prev = pos + 1;
                    }
                } else {
                    retval += pattern.substring(prev, pos - 1) + "{";
                    prev = pos + 1;
                }
                pos = pattern.indexOf("{", prev);
            }
            return retval + pattern.substring(prev);
        }
        return null;
    }

    public static Integer convertFromMsToMinutes(Integer duration) {
        return (duration!=null) ? duration/MILISECONDS_IN_SECOND/SECONDS_IN_MINUTE : BigDecimal.ZERO.intValue();
    }

    public static Integer convertFromMinutesToMs(Integer duration) {
        return (duration!=null) ? BigDecimal.valueOf(MILISECONDS_IN_SECOND * SECONDS_IN_MINUTE * duration).intValue() : BigDecimal.ZERO.intValue();
    }

	public static BigDecimal string2decimal(String number) {
		if (StringUtils.isEmpty(number))
			return null;
		try {
			return new BigDecimal(number);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public static final String mapOrderPeriods(Integer periodId,Integer periodDay,String description,Timestamp nextInvoiceDate){

		if(Constants.PERIOD_UNIT_WEEK == periodId.intValue()){
			 return String.format("%s %s", MainSubscriptionWS.weekDaysMap.get(periodDay), description);
		}
		else if(Constants.PERIOD_UNIT_MONTH == periodId.intValue()){
			 return String.format("%s %s",MainSubscriptionWS.monthDays.get(periodDay-1),description);
		}
		else if(Constants.PERIOD_UNIT_DAY == periodId.intValue()){
			 return String.valueOf(description);
		}
		else if(Constants.PERIOD_UNIT_YEAR == periodId.intValue()){
			 return String.format("%s, %s %s",description
					 						,MainSubscriptionWS.yearMonthsMap.get((nextInvoiceDate != null)?nextInvoiceDate.getMonth()+1:1)
					 					    ,(nextInvoiceDate != null)?nextInvoiceDate.getDate():1);
		}
		else if(Constants.PERIOD_UNIT_SEMI_MONTHLY == periodId.intValue()){
			 return String.format("%s %s", MainSubscriptionWS.semiMonthlyDaysMap.get(periodDay), description);
		}

		return null;
	}

    public static final String formatRateForDisplay(BigDecimal rate) {

        String outputString= "0.0000";

        if ( null != rate ) {

            if (BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP).compareTo(rate.setScale(10, RoundingMode.HALF_UP)) == 0) {
                //the price is zero, show '0.0000'
            } else {
                BigDecimal tempRate = rate.setScale(4, RoundingMode.HALF_UP);//correct the rate

                //check if you lose precision
                if (BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP).compareTo(tempRate) == 0) {
                    outputString = rate.toPlainString(); //show original
                } else {
                    outputString = tempRate.toPlainString(); //show formatted, standard 4 decimal places
                }
            }
        }
        return outputString;
    }

    /**
     * This method was added because there was problem with Amex card number which is 15 digit.
     * The 15 digits Amex card number was oscuring except last three digits, & same it was displaying on UI.
     * Whether with other card's which are 16 digits was obscuring except last four digits.
     * This method accepts any number of digits card number & obscures with '*' except last four digit,
     * which is the standard format of display obscured card number.
     * @param cardNumber
     * @return obscured card number
     */
    public static final String getObscuredCardNumber(char[] cardNumber) {
	  return StringUtils.leftPad(new String(cardNumber,cardNumber.length-4,4),Constants.OBSCURED_CARD_LENGTH,'*');
    }
/**
 * New obscured format 6d...3d to get Card type from stored card initial digits for ObscuredvalidationRule on cardnumber
 * @param cardNumber
 * @return
 */
    public static final String getObscuredCardNumberNew(char[] cardNumber) {
        String ccNumber = String.valueOf(cardNumber);
        if (ccNumber.contains("...")) {
            return ccNumber;
        }
        String obscuredCcNumber = "";
        obscuredCcNumber = ccNumber.substring(0, 6);
        obscuredCcNumber = obscuredCcNumber.concat("...").concat(ccNumber.substring(cardNumber.length - 3));
        return obscuredCcNumber;
    }

    /**
     * Used as a helper for numeric equality checks.
     * If both are null, this returns true. If only one of them is
     * null this returns false. In every other case it checks the
     * BigDecimal equality of the numbers.
     *
     * @param actual
     * @param expected
     * @return
     */
    public static boolean decimalEquals(BigDecimal actual, BigDecimal expected){
        if (actual == expected) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        return getScaledDecimal(actual).equals(getScaledDecimal(expected));
    }

    /**
     * Returns a scaled <code>decimal</code> number
     * according to our decimal scale and rounding strategy
     * defined in CommonConstants.BIGDECIMAL_SCALE and CommonConstants.BIGDECIMAL_ROUND.
     *
     * @param decimal unscaled number
     * @return scaled number
     * @see CommonConstants
     */
    public static BigDecimal getScaledDecimal(BigDecimal decimal){
        if (null == decimal){
            return null;
        }
        return decimal.setScale(CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
    }

    private static class StdInputStreamGobbler implements Runnable {
        private final InputStream    is;
        private final StringBuilder  output;

        public StdInputStreamGobbler (InputStream is, StringBuilder  output) {
            this.is = is;
            this.output = output;
        }

        @Override
        public void run () {
            try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader = new BufferedReader(isr)) {
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    output.append(System.lineSeparator());
                    line = reader.readLine();
                }
            } catch (IOException e) {
                logger.error("Error while executeOnSystem", e);
            }
        }
    }
}
