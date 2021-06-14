package io.github.astrapi69.mystic.crypt.panels.signin;

import org.junit.jupiter.api.Test;

import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultPrivateKeyHolderTest
{

	@Test void getDefaultPrivateKey() throws Exception
	{
		PrivateKey defaultPrivateKey = DefaultPrivateKeyHolder.getDefaultPrivateKey();
		assertNotNull(defaultPrivateKey);
	}

	@Test void getPasswordProtectedPrivateKey() throws Exception
	{
		PrivateKey passwordProtectedPrivateKey = DefaultPrivateKeyHolder
			.getPasswordProtectedPrivateKey("secret");
		assertNotNull(passwordProtectedPrivateKey);
	}
}
