package com.sapienter.jbilling.einvoice;

import org.springframework.util.Assert;

public abstract class GSTINValidator {
	private static final String GSTINFORMAT_REGEX = "[0-9]{2}[a-zA-Z]{5}[0-9]{4}[a-zA-Z]{1}[1-9A-Za-z]{1}[Z]{1}[0-9a-zA-Z]{1}";
	private static final String GSTN_CODEPOINT_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private GSTINValidator() {
		throw new UnsupportedOperationException("GSTINValidator Instantiation not supported");
	}

	/**
	 * Method to check if a GSTIN is valid. Checks the GSTIN format and the
	 * check digit is valid for the passed input GSTIN
	 * @param gstin
	 * @return boolean - valid or not
	 */
	public static boolean validGSTIN(String gstin) {
		boolean isValidFormat = false;
		if (checkPattern(gstin, GSTINFORMAT_REGEX)) {
			isValidFormat = verifyCheckDigit(gstin);
		}
		return isValidFormat;

	}

	/**
	 * Method for checkDigit verification.
	 * @param gstinWCheckDigit
	 * @return boolean - valid or not
	 */
	private static boolean verifyCheckDigit(String gstinWCheckDigit) {
		Boolean isCDValid = false;
		String newGstninWCheckDigit = getGSTINWithCheckDigit(
				gstinWCheckDigit.substring(0, gstinWCheckDigit.length() - 1));
		if (gstinWCheckDigit.trim().equals(newGstninWCheckDigit)) {
			isCDValid = true;
		}
		return isCDValid;
	}

	/**
	 * Method to check if an input string matches the regex pattern passed
	 * @param inputval
	 * @param regxpatrn
	 * @return boolean - valid or not
	 */
	private static boolean checkPattern(String inputval, String regxpatrn) {
		boolean result = false;
		if ((inputval.trim()).matches(regxpatrn)) {
			result = true;
		}
		return result;
	}

	/**
	 * Method to get the check digit for the gstin (without checkdigit)
	 * @param gstinWOCheckDigit
	 * @return : GSTIN with check digit
	 */
	private static String getGSTINWithCheckDigit(String gstinWOCheckDigit) {
		int factor = 2;
		int sum = 0;
		int checkCodePoint = 0;
		char[] cpChars;
		char[] inputChars;
		try {
			Assert.hasLength(gstinWOCheckDigit, "GSTIN supplied for checkdigit calculation is null");
			cpChars = GSTN_CODEPOINT_CHARS.toCharArray();
			inputChars = gstinWOCheckDigit.trim().toUpperCase().toCharArray();

			int mod = cpChars.length;
			for (int i = inputChars.length - 1; i >= 0; i--) {
				int codePoint = -1;
				for (int j = 0; j < cpChars.length; j++) {
					if (cpChars[j] == inputChars[i]) {
						codePoint = j;
					}
				}
				int digit = factor * codePoint;
				factor = (factor == 2) ? 1 : 2;
				digit = (digit / mod) + (digit % mod);
				sum += digit;
			}
			checkCodePoint = (mod - (sum % mod)) % mod;
			return gstinWOCheckDigit + cpChars[checkCodePoint];
		} finally {
			inputChars = null;
			cpChars = null;
		}
	}
}
