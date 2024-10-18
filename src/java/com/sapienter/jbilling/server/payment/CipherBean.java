package com.sapienter.jbilling.server.payment;

import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.springframework.beans.factory.InitializingBean;

public class CipherBean implements InitializingBean {

	private String encryptionAlgorithm; 
    private Cipher cipher;
    private SecretKeyBean secretKeyBean;
    private Base64Bean base64Bean;

    public String encryptString (String source) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    	if(secretKeyBean.getUsable() == 0){
    		return source;
    	}
        cipher.init(Cipher.ENCRYPT_MODE, secretKeyBean.getSecretKey());
        byte[] byteCipherText = cipher.doFinal(source.getBytes());
        return base64Bean.encode(byteCipherText);
    }
    
    public String decryptString (String encrypted) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    	if(secretKeyBean.getUsable() == 0){
    		return encrypted;
    	}
        cipher.init(Cipher.DECRYPT_MODE, secretKeyBean.getSecretKey());
        byte[] byteCipherText = cipher.doFinal(base64Bean.decode(encrypted));
        return new String(byteCipherText);
    }

    
	@Override
	public void afterPropertiesSet() throws Exception {
		cipher = Cipher.getInstance(encryptionAlgorithm);
	}

	public void setSecretKeyBean(SecretKeyBean secretKeyBean) {
		this.secretKeyBean = secretKeyBean;
	}

	public void setBase64Bean(Base64Bean base64Bean) {
		this.base64Bean = base64Bean;
	}

	public void setEncryptionAlgorithm(String encryptionAlgorithm) {
		this.encryptionAlgorithm = encryptionAlgorithm;
	}

}
