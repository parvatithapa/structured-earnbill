package com.sapienter.jbilling.server.payment;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.InitializingBean;

public class SecretKeyBean implements InitializingBean {

	private SecretKey secretKey;

	 private String keyPath;
	 private String keyPassword;
	 private String keyStoreType;
	 private String keyAlias;
	 private int usable = 1 ;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(secretKey == null){
    		FileInputStream inputStream = null;
    		
    		try{
    			inputStream = new FileInputStream(keyPath);
    			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
    			keyStore.load(inputStream, keyPassword.toCharArray());
    			Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
    			secretKey = new SecretKeySpec(key.getEncoded(), key.getAlgorithm());
    			
    		} catch(Exception e){
    			usable = 0;
    		}
    		finally{
    			try {
    				if(inputStream!=null){
    					inputStream.close();
    				}
				} catch (Exception e) {
					usable = 0;
				}
    			
    		}
    	}

	}

	public SecretKey getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public void setKeyPath(String keyPath) {
		this.keyPath = keyPath;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}

	public int getUsable() {
		return usable;
	}

	public void setUsable(int usable) {
		this.usable = usable;
	}

}
