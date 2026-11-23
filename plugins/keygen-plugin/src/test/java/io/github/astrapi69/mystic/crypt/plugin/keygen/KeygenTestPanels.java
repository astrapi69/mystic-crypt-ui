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

import java.util.UUID;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;

/**
 * What the keygen plugin's tests share: a panel that has generated a key pair, and throwaway
 * passwords made up per run. A password written into the source looks like a credential to a reader
 * and to the secret scanner, while it guards only what the test itself creates; the host's
 * {@code TestPasswords} lives in a source set a plugin build does not see.
 */
final class KeygenTestPanels
{

	private KeygenTestPanels()
	{
	}

	/**
	 * A panel that has generated a key pair of the given algorithm, with the smallest key size
	 *
	 * @param algorithm
	 *            the algorithm to generate with
	 * @return the panel, holding the generated key pair
	 */
	static GenerateKeysPanel panelWithAKeyPairOf(final KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(algorithm);
		panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		panel.getCryptographyPanel().getBtnGenerate().doClick();
		return panel;
	}

	/**
	 * A panel that has generated an RSA key pair of 1024 bits
	 *
	 * @return the panel, holding the generated key pair
	 */
	static GenerateKeysPanel panelWithAnRsaKeyPair()
	{
		return panelWithAKeyPairOf(KeyPairGeneratorAlgorithm.RSA);
	}

	/**
	 * A password for a key the test creates, different on every call
	 *
	 * @return the password
	 */
	static String throwawayPassword()
	{
		return "throwaway-" + UUID.randomUUID();
	}
}
