package com.fiserv.optis.qarecon.service;

import com.fiserv.dataprotector.config.Configuration;
import com.fiserv.dataprotector.config.StandardConfigurationLoader;
import com.fiserv.dataprotector.exception.ConfigException;
import com.fiserv.voltage.FiservProtector;
import com.fiserv.voltage.ProtectorFactory;
import com.mphasis.xaap.security.EncryptionResult;
import com.mphasis.xaap.security.OptisCryptoProcessor;
import org.bson.Document;

public class CryptService {

    private static FiservProtector fiservProtector;
    private static OptisCryptoProcessor optisCryptoProcessor;
    private static Configuration config;
    public static final String CRYPT_ID = "Binary_GCM_Internal";

    public Document decryptDocumentFields(String encrypText) {
        String decryptedString = null;
        try {
            fiservProtector = ProtectorFactory.getProtector(getConfiguration());
            // decryption
            decryptedString = fiservProtector.access(encrypText, CRYPT_ID);
        } catch (Exception ex) {
            System.out.println("exception occured while decrypting::: " + ex);
            ex.printStackTrace();
        }
        // Convert decrypted string to Document
        return decryptedString != null ? Document.parse(decryptedString) : null;
    }

    private static Configuration getConfiguration() throws ConfigException {
        if (config == null) {
            StandardConfigurationLoader loader = new StandardConfigurationLoader();
            config = loader.load();
        }
        return config;
    }

    public EncryptionResult encryptField(String txtToEncrypt, String cryptField) {
        EncryptionResult encString = null;
        try {
            if (optisCryptoProcessor == null) {
                synchronized (CryptService.class) {
                    if (optisCryptoProcessor == null) {
                        optisCryptoProcessor = new OptisCryptoProcessor();
                    }
                }
            }
            encString = optisCryptoProcessor.encrypt( null, txtToEncrypt, cryptField);
        } catch (Exception ex) {
            System.out.println("exception occured while encrypting::: " + ex);
            ex.printStackTrace();
        }
        return encString;
    }
}