/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The reproduction and the fix for #357: a database signed in with a master password alone has no
 * private key, and every case that reaches for one anyway must answer false rather than throw.
 */
class ObfuscationKeyAvailabilityTest
{

	@Test
	@DisplayName("a database opened with a master password only has no key to export or import with")
	void keyIsAvailable_isFalse_forTheOrdinaryPasswordOnlyDatabase()
	{
		ApplicationModelBean passwordOnly = ApplicationModelBean.builder()
			.masterPwFileModelBean(MasterPwFileModelBean.builder()
				.masterPw("throwaway".toCharArray()).withMasterPw(true).withKeyFile(false).build())
			.build();

		assertFalse(ObfuscationKeyAvailability.keyIsAvailable(passwordOnly),
			"this is the reproduction: privateKeyInfo is null here, exactly like every database "
				+ "the sign-in dialog offers without a key file - which was the ordinary case "
				+ "that threw a NullPointerException before this fix (#357)");
	}

	@Test
	@DisplayName("a database opened with a key file has one")
	void keyIsAvailable_isTrue_whenAPrivateKeyIsPresent()
	{
		ApplicationModelBean withKey = ApplicationModelBean.builder()
			.masterPwFileModelBean(MasterPwFileModelBean.builder()
				.privateKeyInfo(KeyModel.builder().encoded("not-a-real-key".getBytes())
					.algorithm("RSA").keyType(KeyType.PRIVATE_KEY).build())
				.build())
			.build();

		assertTrue(ObfuscationKeyAvailability.keyIsAvailable(withKey),
			"the presence of the key is all this asks - whether the bytes are a valid key is the "
				+ "encryptor's question, not this one");
	}

	@Test
	@DisplayName("no credentials at all is the same as no key")
	void keyIsAvailable_isFalse_whenThereAreNoCredentials()
	{
		assertFalse(ObfuscationKeyAvailability.keyIsAvailable(null),
			"nothing is signed in, so nothing is available - the null itself must not throw, the "
				+ "same discipline every other null-accepting model method in this codebase keeps");
		assertFalse(
			ObfuscationKeyAvailability
				.keyIsAvailable(ApplicationModelBean.builder().masterPwFileModelBean(null).build()),
			"a model with no credentials bean at all is the same case as one whose credentials "
				+ "carry no key");
	}

	@Test
	@DisplayName("the disabled tooltip and the refusal both name the actual reason")
	void theTextsNameTheReason()
	{
		assertTrue(ObfuscationKeyAvailability.disabledTooltip().contains("key"),
			"a disabled control with no reason is exactly what i18n.md and the lock work have "
				+ "been moving away from");
		assertTrue(ObfuscationKeyAvailability.refusalMessage().contains("key"));
		assertFalse(ObfuscationKeyAvailability.refusalTitle().isBlank());
	}
}
