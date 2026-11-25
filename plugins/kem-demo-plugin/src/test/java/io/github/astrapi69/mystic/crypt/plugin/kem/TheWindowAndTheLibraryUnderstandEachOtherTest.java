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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;

import javax.crypto.SecretKey;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The window and the mystic-crypt library understand each other's key exchange (#895): a handshake
 * the library makes for the window's public key is opened by the window, and one the window makes
 * for a key of the library is opened by the library, for every algorithm. The library's class is
 * what the command line uses, so this is the window talking to the command line. Both directions
 * carry a message, which reads only when both sides arrived at the same secret
 */
class TheWindowAndTheLibraryUnderstandEachOtherTest
{

	private static final String MESSAGE = "the umlauts must survive: äöüß";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static List<String> algorithms()
	{
		return io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.algorithms();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("the window opens a handshake and a message the library made for the window's key")
	void theWindowOpensWhatTheLibraryMade(final String algorithm) throws Exception
	{
		KeyExchangePanel window = new KeyExchangePanel();
		named(window, "cmbAlgorithm", JComboBox.class).setSelectedItem(algorithm);
		click(window, "btnNewKeyPair");

		io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.Handshake fromTheLibrary = io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport
			.encapsulate(textOf(window, "txtMyPublicKey"));
		String encrypted = io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.encryptMessage(
			fromTheLibrary.sharedSecret(), MESSAGE.getBytes(StandardCharsets.UTF_8));
		type(window, "txtHandshakeIn", fromTheLibrary.handshake());
		click(window, "btnDecapsulate");
		type(window, "txtEncryptedIn", encrypted);
		click(window, "btnDecryptMessage");

		assertEquals(MESSAGE, textOf(window, "txtMessageReceived"),
			named(window, "lblResult", JLabel.class).getText());
		assertEquals(
			io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport
				.fingerprintOf(fromTheLibrary.sharedSecret()),
			named(window, "lblMyFingerprint", JLabel.class).getText());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("the library opens a handshake and a message the window made for a key of the library")
	void theLibraryOpensWhatTheWindowMade(final String algorithm) throws Exception
	{
		io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.Party ofTheLibrary = io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport
			.newParty(algorithm);
		KeyExchangePanel window = new KeyExchangePanel();
		named(window, "cmbAlgorithm", JComboBox.class).setSelectedItem(algorithm);

		type(window, "txtTheirPublicKey",
			io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.publicKeyOf(ofTheLibrary));
		click(window, "btnEncapsulate");
		type(window, "txtMessageToSend", MESSAGE);
		click(window, "btnEncryptMessage");
		SecretKey opened = io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport
			.decapsulate(ofTheLibrary, textOf(window, "txtHandshakeOut"));

		assertArrayEquals(MESSAGE.getBytes(StandardCharsets.UTF_8),
			io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.decryptMessage(opened,
				textOf(window, "txtEncryptedOut")),
			named(window, "lblResult", JLabel.class).getText());
		assertEquals(io.github.astrapi69.mystic.crypt.key.KeyExchangeSupport.fingerprintOf(opened),
			named(window, "lblTheirFingerprint", JLabel.class).getText());
	}

	private static <T extends Component> T named(final Container container, final String name,
		final Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = named(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static String textOf(final Container panel, final String name)
	{
		return named(panel, name, JTextComponent.class).getText();
	}

	private static void type(final Container panel, final String name, final String text)
	{
		named(panel, name, JTextComponent.class).setText(text);
	}

	private static void click(final Container panel, final String name)
	{
		named(panel, name, JButton.class).doClick();
	}
}
