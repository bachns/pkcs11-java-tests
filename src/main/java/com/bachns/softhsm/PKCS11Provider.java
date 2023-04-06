package com.bachns.softhsm;

import java.security.Provider;
import java.security.Security;

/**
 * @author Bach Nguyen (bachns), created on 06/04/2023.
 */
public class PKCS11Provider {
	public static Provider provider;

	static {
		provider = Security.getProvider("SunPKCS11-SoftHSM");
		if (provider == null) {
			String config = System.getenv("HSM_CONF");
			if (config == null) {
				System.out.println("Not found HSM_CONF, use the default configuration /etc/softhsm/pkcs11.cfg");
				provider = Security.getProvider("SunPKCS11").configure("/etc/softhsm/pkcs11.cfg");
				System.out.printf("Provider name = " + provider.getName());
			}
			else {
				provider = Security.getProvider("SunPKCS11").configure(config);
			}
			Security.addProvider(provider);
		}
	}
}
