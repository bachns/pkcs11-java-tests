package com.bachns.softhsm;
import static java.lang.System.getenv;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;


/**
 * @author <a href="mailto:bachns@outlook.com">Bach Nguyen</a>
 * Created on 19/08/2022
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("checkEnvironmentVariables")
class PKCS11ProviderTest {
	private static final String KEY_ALIAS;
	private static final char[] KEY_PASS;
	private static final char[] TOKEN_PASS;

	public static boolean checkEnvironmentVariables() {
		Set<String> envVariables = System.getenv().keySet();
		List<String> list = Arrays.asList("HSM_CONF", "HSM_KEY_ALIAS",
				"HSM_KEY_PASS", "HSM_TOKEN_PASS");
		boolean containsAll = envVariables.containsAll(list);
		if (!containsAll) {
			System.err.println("The tests were skipped due to missing one of the environment variables: " +
					String.join(", ", list));
			return false;
		}
		return true;
	}

	static {
		KEY_ALIAS = Optional.ofNullable(getenv("HSM_KEY_ALIAS")).orElse("");
		KEY_PASS = Optional.ofNullable(getenv("HSM_KEY_PASS")).orElse("").toCharArray();
		TOKEN_PASS = Optional.ofNullable(getenv("HSM_TOKEN_PASS")).orElse("").toCharArray();
	}

	@Test
	@Order(1)
	public void testSunPKCS11SoftHSMProvider() {
		assertNotNull(PKCS11Provider.provider);
		List<String> providers = Arrays.stream(Security.getProviders())
				.map(Provider::getName).toList();
		providers.forEach(System.out::println);
		assertThat(providers, hasItems("SunPKCS11-SoftHSM"));
	}
	@Test
	@Order(2)
	public void testSoftHSMConfiguration()
			throws KeyStoreException, CertificateException, IOException,
			NoSuchAlgorithmException, UnrecoverableKeyException {
		KeyStore keyStore = KeyStore.getInstance("PKCS11", PKCS11Provider.provider);
		keyStore.load(null, TOKEN_PASS);
		assertNotNull(keyStore.getKey(KEY_ALIAS, KEY_PASS));
		assertNotNull(keyStore.getCertificate(KEY_ALIAS));
	}

	@Test
	@Order(3)
	public void testSignatureAlgorithms() {
		List<String> signatureAlgorithms = PKCS11Provider.provider.getServices().stream()
				.filter(s -> s.getType().contentEquals("Signature"))
				.map(Service::getAlgorithm).toList();
		assertThat(signatureAlgorithms,
				hasItems(
						"SHA256withRSA", "SHA384withRSA", "SHA512withRSA",
						"SHA256withRSASSA-PSS", "SHA384withRSASSA-PSS", "SHA512withRSASSA-PSS",
						"SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA")
		);
	}

	@Test
	@Order(4)
	public void testVerifyRSAKeyPair()
			throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, NoSuchProviderException, InvalidKeyException, SignatureException {
		KeyStore keyStore = KeyStore.getInstance("PKCS11", PKCS11Provider.provider);
		keyStore.load(null, TOKEN_PASS);
		PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, KEY_PASS);
		Certificate certificate = keyStore.getCertificate(KEY_ALIAS);

		byte[] data = UUID.randomUUID().toString().getBytes();
		Signature signer = Signature.getInstance("SHA256withRSA", "SunPKCS11-SoftHSM");
		signer.initSign(privateKey);
		signer.update(data);
		byte[] signature = signer.sign();

		Signature verifier = Signature.getInstance("SHA256withRSA", "SunPKCS11-SoftHSM");
		verifier.initVerify(certificate);
		verifier.update(data);
		assertTrue(verifier.verify(signature));
	}
}