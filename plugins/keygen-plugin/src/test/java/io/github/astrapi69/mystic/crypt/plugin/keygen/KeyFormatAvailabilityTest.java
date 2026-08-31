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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFormat;

/**
 * PKCS#1 is a real, distinct encoding only for RSA and EC (crypt-data's
 * {@code PrivateKeyExtensions#toPemFormat} falls through to PKCS#8 for everything else, silently -
 * see issue #101). Offering the choice for an algorithm where it changes nothing is misleading, so
 * the box has to close itself down to PKCS#8 exactly where the key size box already closes itself
 * down to one value: outside RSA - here, outside RSA and EC.
 */
class KeyFormatAvailabilityTest
{

	@ParameterizedTest
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "EC" })
	void keyFormatStaysChoosableWhereItMakesADifference(KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		panel.getCmbAlgorithm().setSelectedItem(algorithm);

		assertTrue(panel.getCmbKeyFormat().isEnabled(),
			algorithm + " has a real PKCS#1 encoding, the box must stay open");
	}

	@ParameterizedTest
	@EnumSource(value = KeyPairGeneratorAlgorithm.class,
		names = { "X25519", "X448", "ML_KEM_768", "ML_DSA_65" })
	void keyFormatClosesToPkcs8WhereChoosingPkcs1WouldSilentlyDoNothing(
		KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbKeyFormat().setSelectedItem(KeyFormat.PKCS_1);

		panel.getCmbAlgorithm().setSelectedItem(algorithm);

		assertFalse(panel.getCmbKeyFormat().isEnabled(),
			algorithm + " has no PKCS#1 encoding of its own, the box must not offer it");
		assertEquals(KeyFormat.PKCS_8, panel.getModelObject().getKeyFormat(),
			"switching to " + algorithm + " must not leave a PKCS#1 choice standing that the save "
				+ "path would silently turn into PKCS#8 anyway");
	}

}
