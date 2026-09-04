/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.keygen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;

import javax.swing.text.BadLocationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that both text areas of {@link EnDecryptPanel} are bound to the model of the panel: what is
 * typed lands in the model, an encrypt or decrypt callback takes its input from there, and what it
 * writes back into a text area is in the model as well.
 * <p>
 * The panel under test reverses the text instead of enciphering it - what is proven here is where
 * the text travels, not what happens to it. Without the binding the callback would be handed a null
 * from the model and nothing would come back.
 */
class EnDecryptPanelBindingTest
{

	/** A panel whose two buttons read what they work with from the model of the panel */
	private static final class ReversingEnDecryptPanel extends EnDecryptPanel
	{

		private static final long serialVersionUID = 1L;

		@Override
		protected void onDecrypt(final ActionEvent actionEvent)
		{
			getTxtToEncrypt().setText(reversed(getModelObject().getRightContent()));
			getTxtEncrypted().setText("");
		}

		@Override
		protected void onEncrypt(final ActionEvent actionEvent)
		{
			getTxtEncrypted().setText(reversed(getModelObject().getLeftContent()));
			getTxtToEncrypt().setText("");
		}
	}

	private static String reversed(final String text)
	{
		return new StringBuilder(text).reverse().toString();
	}

	/**
	 * Encrypting takes the text from the model, and the result is in the model too - the text area
	 * is only what shows it
	 */
	@Test
	void encryptTakesTheTextToEncryptFromTheModelOfThePanel()
	{
		ReversingEnDecryptPanel panel = new ReversingEnDecryptPanel();

		panel.getTxtToEncrypt().setText("the secret text");
		assertEquals("the secret text", panel.getModelObject().getLeftContent(),
			"what was typed did not reach the model");

		panel.getBtnEncrypt().doClick();

		assertEquals("txet terces eht", panel.getTxtEncrypted().getText());
		assertEquals("txet terces eht", panel.getModelObject().getRightContent(),
			"what the button wrote back did not reach the model");
		assertEquals("", panel.getModelObject().getLeftContent(),
			"the cleared text area left its old content in the model");
	}

	/**
	 * Decrypting takes the encrypted text from the model, and the plain text is in the model too
	 */
	@Test
	void decryptTakesTheEncryptedTextFromTheModelOfThePanel()
	{
		ReversingEnDecryptPanel panel = new ReversingEnDecryptPanel();

		panel.getTxtEncrypted().setText("txet terces eht");
		assertEquals("txet terces eht", panel.getModelObject().getRightContent(),
			"what was typed did not reach the model");

		panel.getBtnDecrypt().doClick();

		assertEquals("the secret text", panel.getTxtToEncrypt().getText());
		assertEquals("the secret text", panel.getModelObject().getLeftContent(),
			"what the button wrote back did not reach the model");
		assertEquals("", panel.getModelObject().getRightContent(),
			"the cleared text area left its old content in the model");
	}

	/**
	 * The model follows every single edit, not only a whole text that is set at once
	 */
	@Test
	void theModelFollowsEveryEditOfBothTextAreas() throws BadLocationException
	{
		EnDecryptPanel panel = new EnDecryptPanel();

		panel.getTxtToEncrypt().getDocument().insertString(0, "typed", null);
		panel.getTxtEncrypted().getDocument().insertString(0, "pasted", null);

		assertEquals("typed", panel.getModelObject().getLeftContent());
		assertEquals("pasted", panel.getModelObject().getRightContent());

		panel.getTxtToEncrypt().getDocument().remove(0, "typed".length());

		assertEquals("", panel.getModelObject().getLeftContent());
	}

	/**
	 * Before anything is typed the model holds empty texts, so a callback that reads it never has
	 * to guard against a null
	 */
	@Test
	void theModelStartsWithEmptyTextsOnBothSides()
	{
		EnDecryptPanel panel = new EnDecryptPanel();

		assertEquals("", panel.getModelObject().getLeftContent());
		assertEquals("", panel.getModelObject().getRightContent());
	}

	/**
	 * The names the UI tests and the plugins that share this panel look the components up by
	 */
	@Test
	void theComponentsKeepTheNamesTheyAreLookedUpBy()
	{
		EnDecryptPanel panel = new EnDecryptPanel();

		assertEquals("txtToEncrypt", panel.getTxtToEncrypt().getName());
		assertEquals("txtEncrypted", panel.getTxtEncrypted().getName());
		assertEquals("btnEncrypt", panel.getBtnEncrypt().getName());
		assertEquals("btnDecrypt", panel.getBtnDecrypt().getName());
	}

	/**
	 * A disabled button that explains itself only on hover reads as broken until someone thinks to
	 * hover it - the reason has to be readable without hovering anything (#189)
	 */
	@Test
	@DisplayName("the reason the buttons are out of reach is shown, not only on the buttons' tooltip")
	void theUnavailableReasonIsVisibleWithoutHoveringAnything()
	{
		EnDecryptPanel panel = new EnDecryptPanel();

		panel.setEnDecryptAvailable(false, "only RSA can do this");

		assertEquals("only RSA can do this", panel.getLblReason().getText());
	}

	@Test
	@DisplayName("the reason disappears once encrypting and decrypting are available again")
	void theReasonIsClearedOnceAvailableAgain()
	{
		EnDecryptPanel panel = new EnDecryptPanel();
		panel.setEnDecryptAvailable(false, "only RSA can do this");

		panel.setEnDecryptAvailable(true, "only RSA can do this");

		assertTrue(panel.getLblReason().getText().isBlank(),
			"the old reason must not linger once the buttons work again");
	}
}
