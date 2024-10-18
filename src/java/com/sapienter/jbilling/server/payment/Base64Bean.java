package com.sapienter.jbilling.server.payment;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.InitializingBean;

public class Base64Bean implements InitializingBean {

	private Base64 base64;

	@Override
	public void afterPropertiesSet() throws Exception {
		base64 = new Base64();
		
	}
	
	public String encode(byte[] bytes){
		return base64.encodeAsString(bytes);
	}
	
	public byte[] decode(String source){
		return base64.decode(source);
	}
}
