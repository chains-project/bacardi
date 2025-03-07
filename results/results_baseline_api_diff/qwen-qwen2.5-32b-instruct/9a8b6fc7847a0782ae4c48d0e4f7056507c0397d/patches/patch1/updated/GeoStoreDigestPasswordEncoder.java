package it.geosolutions.geostore.core.security.password;

import org.apache.commons.codec.binary.Base64;
import org.jasypt.digest.StandardByteDigester;
import org.jasypt.util.password.StrongPasswordEncryptor;
import static it.geosolutions.geostore.core.security.password.SecurityUtils.toBytes;

/**
 * This Encoder provide encription and check of password using a digest
 * @author Lorenzo Natali (lorenzo.natali at geo-solutions.it)
 *
 */
public class GeoStoreDigestPasswordEncoder extends AbstractGeoStorePasswordEncoder{
	

	/**
	 * The digest is not reversible
	 */
    public GeoStoreDigestPasswordEncoder() {
        setReversible(false);
    }

    @Override
    protected Object createStringEncoder() {
        StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();
        return encryptor;
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            StandardByteDigester digester = new StandardByteDigester();
            {
                digester.setAlgorithm("SHA-256");
                digester.setIterations(100000);
                digester.setSaltSizeBytes(16);
                digester.initialize();
            }
            
            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return new String(Base64.encodeBase64(digester.digest(toBytes(rawPass))));
            }
            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return digester.matches(toBytes(rawPass), Base64.decodeBase64(encPass.getBytes())); 
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.DIGEST;
    }
}