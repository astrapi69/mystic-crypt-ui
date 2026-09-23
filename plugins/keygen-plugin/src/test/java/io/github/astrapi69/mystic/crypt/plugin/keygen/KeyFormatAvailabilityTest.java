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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFormat;

/**
 * PKCS#1 is a real, distinct encoding for RSA, DSA, EC and RSASSA-PSS; crypt-data's
 * {@code PrivateKeyExtensions#toPemFormat} falls through to PKCS#8 for everything else, silently -
 * see issue #101 and crypt-data#42. Offering the choice where it changes nothing is misleading, so
 * the box closes itself down to PKCS#8 exactly where the key size box already closes itself down to
 * one value.
 * <p>
 * {@code GenerateKeysPanel} names only RSA and EC in that check, which is correct here and only
 * here: {@code SUPPORTED_ALGORITHMS} offers RSA, EC, X25519, X448, ML-KEM-768 and ML-DSA-65, so DSA
 * and RSASSA-PSS can never reach the box. A case for either would pass whatever the code did -
 * selecting an algorithm the combo does not carry changes nothing - so widening this test would add
 * a row that proves nothing. If either is ever added to SUPPORTED_ALGORITHMS, the check has to grow
 * with it.
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

	/**
	 * A box that closes itself says why it did. The command line answers the same request with a
	 * sentence since mystic-crypt 13.0 - "PKCS#1 was asked for, but a 'XDH' private key has no
	 * traditional form" - and a user who finds the box greyed out here is asking the same question
	 * as the one who typed that command (#435)
	 *
	 * @param algorithm
	 *            an algorithm whose private key has no traditional form
	 */
	@ParameterizedTest
	@EnumSource(value = KeyPairGeneratorAlgorithm.class,
		names = { "X25519", "X448", "ML_KEM_768", "ML_DSA_65" })
	void theClosedBoxSaysWhyItIsClosed(KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		panel.getCmbAlgorithm().setSelectedItem(algorithm);

		String reason = panel.getCmbKeyFormat().getToolTipText();
		assertNotNull(reason, algorithm + ": a disabled box with no explanation is a dead end");
		assertTrue(reason.contains("PKCS#8"),
			"the reason names the encoding the key does have: " + reason);
		assertTrue(reason.contains(algorithm.getAlgorithm()),
			"and the algorithm it is talking about: " + reason);
	}

	/**
	 * The other half: where the choice is real, the box explains the choice rather than refusing
	 * it, and switching back from an algorithm that has none restores that text
	 *
	 * @param algorithm
	 *            an algorithm whose private key has a traditional form
	 */
	@ParameterizedTest
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "EC" })
	void anOpenBoxExplainsTheChoiceInsteadOfRefusingIt(KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.X25519);

		panel.getCmbAlgorithm().setSelectedItem(algorithm);

		String tooltip = panel.getCmbKeyFormat().getToolTipText();
		assertNotNull(tooltip, algorithm + ": the box keeps explaining what it offers");
		assertFalse(tooltip.contains("no traditional form"),
			algorithm + " can be written both ways, so the refusal has to be gone again: "
				+ tooltip);
	}

}
