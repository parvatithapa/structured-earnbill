package com.sapienter.jbilling.server.diameter;

import static org.junit.Assert.*;

import org.junit.Test;

public class PhoneNumbeExtractorTest {

	@Test
	public void testNumberExtractor() {
        //Invalid numbers
		assertNull(SipNumberExtractor.extract(null));
		assertNull(SipNumberExtractor.extract(""));
		assertNull(SipNumberExtractor.extract("invalidsip"));
		assertNull(SipNumberExtractor.extract("sip:"));
		assertNull(SipNumberExtractor.extract("sip:123456"));

        //Valid numbers
		assertEquals("123456", SipNumberExtractor.extract("sip:+123456"));
		assertEquals("123456", SipNumberExtractor.extract("sip:+1.234.56"));
		assertEquals("123456", SipNumberExtractor.extract(" sip:+1.234.56"));
		assertEquals("447890123456", SipNumberExtractor.extract("sip:+447890123456@crocodile-sip.net"));
		assertEquals("18005555555", SipNumberExtractor.extract("sip:+1(800)555-5555@voip-provider.com"));
        assertEquals("441489760000", SipNumberExtractor.extract("sip:+441489760000;postd=789@example.com"));
        assertEquals("john@crocodile-sip.net", SipNumberExtractor.extract("sip:john@crocodile-sip.net"));
        assertEquals("john@crocodile-sip.net", SipNumberExtractor.extract("sips:john@crocodile-sip.net"));
        assertEquals("447890123456", SipNumberExtractor.extract("sips:+447890123456@crocodile-sip.net"));
        assertEquals("alice@atlanta.com", SipNumberExtractor.extract("sip:alice:password@atlanta.com:port;uri-parameters?headers"));
        assertEquals("alice@atlanta.com", SipNumberExtractor.extract("sip:alice:password@atlanta.com;uri-parameters?headers"));
        assertEquals("alice@atlanta.com", SipNumberExtractor.extract("sip:alice:password@atlanta.com?headers"));
	}
}
