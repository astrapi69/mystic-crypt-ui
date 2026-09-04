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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that the components of {@link KeyExchangePanel} are bound to {@link KeyExchangePanelModel}
 * and that the two tabs together carry out a real exchange: what one tab produces is what the other
 * one consumes, and the message only comes back when both sides hold the same secret.
 */
class KeyExchangePanelBindingTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> algorithms()
	{
		return KeyExchangeSupport.algorithms();
	}

	private static <T extends Component> T named(Container container, String name, Class<T> type)
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

	private static String textOf(Container panel, String name)
	{
		return named(panel, name, JTextComponent.class).getText();
	}

	private static void type(Container panel, String name, String text)
	{
		named(panel, name, JTextComponent.class).setText(text);
	}

	private static void click(Container panel, String name)
	{
		named(panel, name, JButton.class).doClick();
	}

	private static String resultOf(Container panel)
	{
		return named(panel, "lblResult", JLabel.class).getText();
	}

	/**
	 * The exchange through the panel, for every algorithm it offers: the public key of the receiving
	 * tab goes into the sending tab, the handshake comes back, and the message only reads because
	 * both sides arrived at the same secret
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("the two tabs carry out an exchange and the message survives it")
	void theTwoTabsCarryOutAnExchange(String algorithm)
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		String message = "the umlauts must survive: \u00e4\u00f6\u00fc\u00df";

		named(panel, "cmbAlgorithm", JComboBox.class).setSelectedItem(algorithm);
		click(panel, "btnNewKeyPair");
		String published = textOf(panel, "txtMyPublicKey");
		assertFalse(published.isBlank(), resultOf(panel));
		assertEquals(algorithm, KeyExchangeSupport.algorithmOf(published));

		type(panel, "txtTheirPublicKey", published);
		click(panel, "btnEncapsulate");
		type(panel, "txtMessageToSend", message);
		click(panel, "btnEncryptMessage");
		String handshake = textOf(panel, "txtHandshakeOut");
		String encrypted = textOf(panel, "txtEncryptedOut");
		assertFalse(handshake.isBlank() || encrypted.isBlank(), resultOf(panel));
		assertFalse(encrypted.contains("umlauts"), "the message is readable in the encrypted text");

		type(panel, "txtHandshakeIn", handshake);
		click(panel, "btnDecapsulate");
		type(panel, "txtEncryptedIn", encrypted);
		click(panel, "btnDecryptMessage");

		assertEquals(message, textOf(panel, "txtMessageReceived"), resultOf(panel));
		assertEquals(named(panel, "lblTheirFingerprint", JLabel.class).getText(),
			named(panel, "lblMyFingerprint", JLabel.class).getText());
	}

	/**
	 * What the buttons work with is the model, so a key pair made for one algorithm must not be used
	 * with a handshake from another
	 */
	@Test
	@DisplayName("a handshake from another algorithm is refused in the panel too")
	void aForeignHandshakeIsRefused() throws Exception
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		named(panel, "cmbAlgorithm", JComboBox.class).setSelectedItem(KeyExchangeSupport.X25519);
		click(panel, "btnNewKeyPair");
		KeyExchangeSupport.Party other = KeyExchangeSupport
			.newParty(KeyExchangeSupport.ML_KEM_512);

		type(panel, "txtHandshakeIn", KeyExchangeSupport
			.encapsulate(KeyExchangeSupport.publicKeyOf(other)).handshake());
		click(panel, "btnDecapsulate");

		assertTrue(resultOf(panel).startsWith("not derived:"), resultOf(panel));
		assertEquals("-", named(panel, "lblMyFingerprint", JLabel.class).getText());
	}

	/**
	 * Every step says what is missing rather than failing silently or on something further down
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("stepsOutOfOrder")
	@DisplayName("a step taken before the one it needs says what is missing")
	void aStepOutOfOrderSaysWhatIsMissing(String button, String expected)
	{
		KeyExchangePanel panel = new KeyExchangePanel();

		click(panel, button);

		assertTrue(resultOf(panel).contains(expected), resultOf(panel));
	}

	static List<org.junit.jupiter.params.provider.Arguments> stepsOutOfOrder()
	{
		return List.of(
			org.junit.jupiter.params.provider.Arguments.of("btnDecapsulate",
				"make or load a key pair first"),
			org.junit.jupiter.params.provider.Arguments.of("btnEncryptMessage",
				"make a shared secret first"),
			org.junit.jupiter.params.provider.Arguments.of("btnDecryptMessage",
				"derive the shared secret first"),
			org.junit.jupiter.params.provider.Arguments.of("btnEncapsulate", "not made:"));
	}

	/**
	 * The combo box offers exactly the algorithms the tool supports, in the order it offers them
	 */
	@Test
	@DisplayName("the combo box offers the supported algorithms in the offered order")
	void theComboBoxOffersTheSupportedAlgorithms()
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		JComboBox<?> comboBox = named(panel, "cmbAlgorithm", JComboBox.class);

		List<Object> offered = new ArrayList<>();
		for (int index = 0; index < comboBox.getItemCount(); index++)
		{
			offered.add(comboBox.getItemAt(index));
		}

		assertEquals(KeyExchangeSupport.algorithms(), offered);
	}

	/**
	 * Making a second key pair must not leave the first one's secret behind, or the panel would show
	 * a fingerprint that belongs to a key that is gone
	 */
	@Test
	@DisplayName("a new key pair clears what belonged to the previous one")
	void aNewKeyPairClearsThePreviousSecret()
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		click(panel, "btnNewKeyPair");
		String first = textOf(panel, "txtMyPublicKey");
		type(panel, "txtTheirPublicKey", first);
		click(panel, "btnEncapsulate");
		type(panel, "txtHandshakeIn", textOf(panel, "txtHandshakeOut"));
		click(panel, "btnDecapsulate");
		assertNotEquals("-", named(panel, "lblMyFingerprint", JLabel.class).getText());

		click(panel, "btnNewKeyPair");

		assertEquals("-", named(panel, "lblMyFingerprint", JLabel.class).getText());
		assertNotEquals(first, textOf(panel, "txtMyPublicKey"));
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	@DisplayName("every field and button on both tabs explains itself with a tooltip")
	void everyFieldExplainsItselfWithATooltip()
	{
		KeyExchangePanel panel = new KeyExchangePanel();

		assertHasTooltip(named(panel, "cmbAlgorithm", JComponent.class), "algorithm");
		assertHasTooltip(named(panel, "lblResult", JComponent.class), "result label");
		assertHasTooltip(named(panel, "lblMyFingerprint", JComponent.class), "my fingerprint");
		assertHasTooltip(named(panel, "lblTheirFingerprint", JComponent.class), "their fingerprint");

		assertHasTooltip(named(panel, "txtMyPublicKey", JComponent.class), "my public key");
		assertHasTooltip(named(panel, "btnNewKeyPair", JComponent.class), "new key pair button");
		assertHasTooltip(named(panel, "btnSaveMyKey", JComponent.class), "save my key button");
		assertHasTooltip(named(panel, "btnLoadMyKey", JComponent.class), "load my key button");
		assertHasTooltip(named(panel, "btnSaveMyPublicKey", JComponent.class),
			"save my public key button");
		assertHasTooltip(named(panel, "txtHandshakeIn", JComponent.class), "handshake in");
		assertHasTooltip(named(panel, "btnLoadHandshake", JComponent.class), "load handshake button");
		assertHasTooltip(named(panel, "btnDecapsulate", JComponent.class), "decapsulate button");
		assertHasTooltip(named(panel, "txtEncryptedIn", JComponent.class), "encrypted in");
		assertHasTooltip(named(panel, "btnDecryptMessage", JComponent.class),
			"decrypt message button");
		assertHasTooltip(named(panel, "txtMessageReceived", JComponent.class), "message received");

		assertHasTooltip(named(panel, "txtTheirPublicKey", JComponent.class), "their public key");
		assertHasTooltip(named(panel, "btnLoadTheirPublicKey", JComponent.class),
			"load their public key button");
		assertHasTooltip(named(panel, "btnEncapsulate", JComponent.class), "encapsulate button");
		assertHasTooltip(named(panel, "txtHandshakeOut", JComponent.class), "handshake out");
		assertHasTooltip(named(panel, "btnSaveHandshake", JComponent.class), "save handshake button");
		assertHasTooltip(named(panel, "txtMessageToSend", JComponent.class), "message to send");
		assertHasTooltip(named(panel, "btnEncryptMessage", JComponent.class),
			"encrypt message button");
		assertHasTooltip(named(panel, "txtEncryptedOut", JComponent.class), "encrypted out");
		assertHasTooltip(named(panel, "btnSaveEncrypted", JComponent.class), "save encrypted button");
	}

	private static String clipboardText() throws Exception
	{
		return (String)Toolkit.getDefaultToolkit().getSystemClipboard()
			.getData(DataFlavor.stringFlavor);
	}

	/**
	 * What travels through this panel is text a user has to move to another machine or another
	 * program by hand - selecting a multi-line key or handshake correctly is easy to get wrong, so
	 * every area gets a copy button that puts exactly what it holds on the clipboard (#184)
	 */
	@ParameterizedTest
	@CsvSource({ "txtMyPublicKey, btnCopyMyPublicKey", "txtHandshakeIn, btnCopyHandshakeIn",
			"txtEncryptedIn, btnCopyEncryptedIn", "txtMessageReceived, btnCopyMessageReceived",
			"txtTheirPublicKey, btnCopyTheirPublicKey", "txtHandshakeOut, btnCopyHandshakeOut",
			"txtMessageToSend, btnCopyMessageToSend", "txtEncryptedOut, btnCopyEncryptedOut" })
	@DisplayName("the copy button next to an area puts exactly that area's text on the clipboard")
	void theCopyButtonPutsTheAreasTextOnTheClipboard(String areaName, String copyButtonName)
		throws Exception
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		String value = "sample-" + areaName;
		type(panel, areaName, value);

		click(panel, copyButtonName);

		assertEquals(value, clipboardText());
	}

	@Test
	@DisplayName("the panel explains what it does before the algorithm field")
	void thePanelExplainsWhatItDoesBeforeTheAlgorithmField()
	{
		KeyExchangePanel panel = new KeyExchangePanel();

		JLabel intro = named(panel, "lblIntro", JLabel.class);

		assertTrue(intro != null && intro.getText() != null && !intro.getText().isBlank(),
			"the panel must explain what Receive and Send do before the tabs");
	}
}
