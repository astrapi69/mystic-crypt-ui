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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextArea;
import net.miginfocom.swing.MigLayout;

/**
 * Tool panel for a key exchange between two people, where each side holds only its own half.
 * <p>
 * One tab is the receiving side and one is the sending side, because that is how the two roles are
 * actually split - normally the two tabs are open on two different machines. The demo next to this
 * tool plays both sides at once, which shows the mathematics; this one is for using it.
 *
 * <pre>
 * Receive  make a key pair, hand out the public key,
 *          and later turn the handshake that comes back into the shared secret
 * Send     take someone's public key, encapsulate against it,
 *          and send back the handshake
 * </pre>
 *
 * What travels is text, so it can be mailed or pasted; every field also loads from and saves to a
 * file. The secret itself is never shown, only its fingerprint - if both sides read out the same
 * eight characters, they hold the same secret.
 * <p>
 * Every component is bound to {@link KeyExchangePanelModel}, so what a button works with is what
 * the model holds.
 * <p>
 * The frame and both tabs are built from {@link ToolForm}, the one layout every tool window in this
 * application uses.
 */
public class KeyExchangePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/**
	 * A key or a handshake is the longest thing a tab holds, so its row takes twice the height a
	 * larger window has left over
	 */
	private static final String KEY_ROW = "span 2, grow, pushx, pushy 200";

	/** A message is shorter than a key, so its row takes three quarters of that */
	private static final String MESSAGE_ROW = "span 2, grow, pushx, pushy 150";

	private final KeyExchangePanelModel modelObject = new KeyExchangePanelModel();
	private final JMComboBox<String, ?> cmbAlgorithm = new JMComboBox<>(
		KeyExchangeSupport.algorithms().toArray(new String[0]));
	private final JMTextArea txtMyPublicKey = new JMTextArea(3, 54);
	private final JMTextArea txtHandshakeIn = new JMTextArea(3, 54);
	private final JMTextArea txtEncryptedIn = new JMTextArea(3, 54);
	private final JMTextArea txtMessageReceived = new JMTextArea(3, 54);
	private final JMTextArea txtTheirPublicKey = new JMTextArea(3, 54);
	private final JMTextArea txtHandshakeOut = new JMTextArea(3, 54);
	private final JMTextArea txtMessageToSend = new JMTextArea(3, 54);
	private final JMTextArea txtEncryptedOut = new JMTextArea(3, 54);
	private final JLabel lblMyFingerprint = new JLabel("-");
	private final JLabel lblTheirFingerprint = new JLabel("-");
	private final JLabel lblResult = new JLabel(" ");
	private final JTabbedPane tabs = new JTabbedPane();

	public KeyExchangePanel()
	{
		// one layout for every tool window: the algorithm on the first row, the two tabs taking the
		// height that is left, the line that reports what happened at the bottom
		super(ToolForm.newLayout());

		bindToModel();

		cmbAlgorithm.setName("cmbAlgorithm");
		cmbAlgorithm.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.algorithm",
			"the key exchange mechanism used by both tabs"));
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		lblResult.setToolTipText(
			KemDemoMessages.getString("key.exchange.tooltip.result", "what the last action did"));
		lblMyFingerprint.setName("lblMyFingerprint");
		lblMyFingerprint.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.my.fingerprint",
			"the fingerprint of your derived secret - read it out to the other side, it must match theirs"));
		lblTheirFingerprint.setName("lblTheirFingerprint");
		lblTheirFingerprint.setToolTipText(
			KemDemoMessages.getString("key.exchange.tooltip.their.fingerprint",
				"the fingerprint of the shared secret you derived - read it out to the other side, it must match theirs"));
		tabs.setName("tabKeyExchange");
		tabs.addTab("Receive", scrollable(newReceiveTab()));
		tabs.addTab("Send", scrollable(newSendTab()));

		JTextComponent lblIntro = ToolForm.intro(KemDemoMessages.getString("key.exchange.intro",
			"A key exchange between two people, each holding only their own half - normally the two "
				+ "tabs below run on two different machines. Receive: make a key pair and hand out "
				+ "your public key. Send: take someone's public key and encapsulate a shared secret "
				+ "against it. The shared secret itself is never shown, only its fingerprint - read "
				+ "it aloud to the other side to confirm both of you derived the same one."));
		lblIntro.setName("lblIntro");

		add(lblIntro, ToolForm.INTRO_ROW);
		add(new JLabel("Algorithm:"));
		add(cmbAlgorithm, ToolForm.FIELD);
		add(tabs, ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
	}

	/** Binds every entry component to its field in {@link KeyExchangePanelModel} */
	private void bindToModel()
	{
		cmbAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getAlgorithm, modelObject::setAlgorithm));
		txtMyPublicKey.setPropertyModel(
			LambdaModel.of(modelObject::getMyPublicKey, modelObject::setMyPublicKey));
		txtHandshakeIn.setPropertyModel(
			LambdaModel.of(modelObject::getHandshakeIn, modelObject::setHandshakeIn));
		txtEncryptedIn.setPropertyModel(
			LambdaModel.of(modelObject::getEncryptedIn, modelObject::setEncryptedIn));
		txtMessageReceived.setPropertyModel(
			LambdaModel.of(modelObject::getMessageReceived, modelObject::setMessageReceived));
		txtTheirPublicKey.setPropertyModel(
			LambdaModel.of(modelObject::getTheirPublicKey, modelObject::setTheirPublicKey));
		txtHandshakeOut.setPropertyModel(
			LambdaModel.of(modelObject::getHandshakeOut, modelObject::setHandshakeOut));
		txtMessageToSend.setPropertyModel(
			LambdaModel.of(modelObject::getMessageToSend, modelObject::setMessageToSend));
		txtEncryptedOut.setPropertyModel(
			LambdaModel.of(modelObject::getEncryptedOut, modelObject::setEncryptedOut));
	}

	private JPanel newReceiveTab()
	{
		txtMyPublicKey.setName("txtMyPublicKey");
		txtMyPublicKey.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.my.public.key",
			"your public key - hand this to the other side, it is safe to share"));
		txtHandshakeIn.setName("txtHandshakeIn");
		txtHandshakeIn.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.handshake.in",
			"the handshake the other side sent back after encapsulating against your public key"));
		txtEncryptedIn.setName("txtEncryptedIn");
		txtEncryptedIn.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.encrypted.in",
			"a message the other side encrypted with the shared secret"));
		txtMessageReceived.setName("txtMessageReceived");
		txtMessageReceived.setToolTipText(
			KemDemoMessages.getString("key.exchange.tooltip.message.received", "the decrypted message"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(ToolForm.buttons(
			button("btnNewKeyPair", "New key pair", event -> onNewKeyPair(),
				KemDemoMessages.getString("key.exchange.tooltip.new.key.pair.button",
					"generates a fresh key pair for you to receive with")),
			button("btnSaveMyKey", "Save my key...", event -> onSaveMyKey(),
				KemDemoMessages.getString("key.exchange.tooltip.save.my.key.button",
					"saves your private key to a file - whoever gets it can read everything ever sent to this key pair")),
			button("btnLoadMyKey", "Load my key...", event -> onLoadMyKey(),
				KemDemoMessages.getString("key.exchange.tooltip.load.my.key.button",
					"loads a previously saved private key, replacing the current one"))),
			ToolForm.BUTTON_ROW);
		panel.add(new JLabel("1. Hand this public key to the other side:"), ToolForm.WIDE);
		panel.add(scrolled(txtMyPublicKey), KEY_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnSaveMyPublicKey", "Save public key...",
					event -> onSaveToFile(modelObject.getMyPublicKey(), "public key"),
					KemDemoMessages.getString("key.exchange.tooltip.save.my.public.key.button",
						"saves your public key to a file, to hand to the other side")),
				CopyButtons.copyButton("btnCopyMyPublicKey", txtMyPublicKey,
					KemDemoMessages.getString("key.exchange.tooltip.copy.my.public.key",
						"copies your public key to the clipboard"))),
			ToolForm.BUTTON_ROW);
		panel.add(new JLabel("2. Paste the handshake that came back:"), ToolForm.WIDE);
		panel.add(scrolled(txtHandshakeIn), KEY_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnLoadHandshake", "Load...",
					event -> onLoadInto(txtHandshakeIn, modelObject::setHandshakeIn),
					KemDemoMessages.getString("key.exchange.tooltip.load.handshake.button",
						"loads a handshake from a file")),
				button("btnDecapsulate", "Derive the secret", event -> onDecapsulate(),
					KemDemoMessages.getString("key.exchange.tooltip.decapsulate.button",
						"derives the shared secret from the handshake above, using your private key")),
				CopyButtons.copyButton("btnCopyHandshakeIn", txtHandshakeIn,
					KemDemoMessages.getString("key.exchange.tooltip.copy.handshake.in",
						"copies the pasted handshake to the clipboard"))),
			ToolForm.BUTTON_ROW);
		panel.add(fingerprintRow("My secret:", lblMyFingerprint), ToolForm.WIDE);
		panel.add(new JLabel("3. A message that arrived with it:"), ToolForm.WIDE);
		panel.add(scrolled(txtEncryptedIn), MESSAGE_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnDecryptMessage", "Read it", event -> onDecryptMessage(),
					KemDemoMessages.getString("key.exchange.tooltip.decrypt.message.button",
						"decrypts the message above with the derived shared secret")),
				CopyButtons.copyButton("btnCopyEncryptedIn", txtEncryptedIn,
					KemDemoMessages.getString("key.exchange.tooltip.copy.encrypted.in",
						"copies the encrypted message to the clipboard"))),
			ToolForm.BUTTON_ROW);
		panel.add(scrolled(txtMessageReceived), MESSAGE_ROW);
		panel.add(
			ToolForm.buttons(CopyButtons.copyButton("btnCopyMessageReceived", txtMessageReceived,
				KemDemoMessages.getString("key.exchange.tooltip.copy.message.received",
					"copies the decrypted message to the clipboard"))),
			ToolForm.BUTTON_ROW);
		return panel;
	}

	private JPanel newSendTab()
	{
		txtTheirPublicKey.setName("txtTheirPublicKey");
		txtTheirPublicKey.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.their.public.key",
			"the public key you were given by the other side"));
		txtHandshakeOut.setName("txtHandshakeOut");
		txtHandshakeOut.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.handshake.out",
			"the handshake to send back to the other side"));
		txtMessageToSend.setName("txtMessageToSend");
		txtMessageToSend.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.message.to.send",
			"the message to encrypt and send along with the handshake"));
		txtEncryptedOut.setName("txtEncryptedOut");
		txtEncryptedOut.setToolTipText(KemDemoMessages.getString("key.exchange.tooltip.encrypted.out",
			"the encrypted message to send"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(new JLabel("1. Paste the public key you were given:"), ToolForm.WIDE);
		panel.add(scrolled(txtTheirPublicKey), KEY_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnLoadTheirPublicKey", "Load...",
					event -> onLoadInto(txtTheirPublicKey, modelObject::setTheirPublicKey),
					KemDemoMessages.getString("key.exchange.tooltip.load.their.public.key.button",
						"loads a public key from a file")),
				button("btnEncapsulate", "Make a shared secret", event -> onEncapsulate(),
					KemDemoMessages.getString("key.exchange.tooltip.encapsulate.button",
						"derives a fresh shared secret against the public key above and produces the handshake to send back")),
				CopyButtons.copyButton("btnCopyTheirPublicKey", txtTheirPublicKey,
					KemDemoMessages.getString("key.exchange.tooltip.copy.their.public.key",
						"copies the pasted public key to the clipboard"))),
			ToolForm.BUTTON_ROW);
		panel.add(fingerprintRow("Shared secret:", lblTheirFingerprint), ToolForm.WIDE);
		panel.add(new JLabel("2. Send this handshake back:"), ToolForm.WIDE);
		panel.add(scrolled(txtHandshakeOut), KEY_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnSaveHandshake", "Save...",
					event -> onSaveToFile(modelObject.getHandshakeOut(), "handshake"),
					KemDemoMessages.getString("key.exchange.tooltip.save.handshake.button",
						"saves the handshake to a file")),
				CopyButtons.copyButton("btnCopyHandshakeOut", txtHandshakeOut,
					KemDemoMessages.getString("key.exchange.tooltip.copy.handshake.out",
						"copies the handshake to the clipboard"))),
			ToolForm.BUTTON_ROW);
		panel.add(new JLabel("3. A message to send with it:"), ToolForm.WIDE);
		panel.add(scrolled(txtMessageToSend), MESSAGE_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnEncryptMessage", "Encrypt it", event -> onEncryptMessage(),
					KemDemoMessages.getString("key.exchange.tooltip.encrypt.message.button",
						"encrypts the message above with the shared secret")),
				CopyButtons.copyButton("btnCopyMessageToSend", txtMessageToSend,
					KemDemoMessages.getString("key.exchange.tooltip.copy.message.to.send",
						"copies the message to the clipboard"))),
			ToolForm.BUTTON_ROW);
		panel.add(scrolled(txtEncryptedOut), MESSAGE_ROW);
		panel.add(
			ToolForm.buttons(
				button("btnSaveEncrypted", "Save...",
					event -> onSaveToFile(modelObject.getEncryptedOut(), "message"),
					KemDemoMessages.getString("key.exchange.tooltip.save.encrypted.button",
						"saves the encrypted message to a file")),
				CopyButtons.copyButton("btnCopyEncryptedOut", txtEncryptedOut,
					KemDemoMessages.getString("key.exchange.tooltip.copy.encrypted.out",
						"copies the encrypted message to the clipboard"))),
			ToolForm.BUTTON_ROW);
		return panel;
	}

	/**
	 * Puts a tab into a scroll pane that lets it grow.
	 * <p>
	 * The two things a window has to do are in tension here: use the height it is given, and never
	 * put a field out of reach. The rows carry a vertical weight so a tall window shows more of the
	 * keys, and a window too short for all of it scrolls instead of cutting the last field off.
	 *
	 * @param content
	 *            the tab
	 * @return the tab, in a scroll pane
	 */
	private static JScrollPane scrollable(JPanel content)
	{
		JScrollPane scrollPane = new JScrollPane(new GrowingContent(content));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		return scrollPane;
	}

	/**
	 * A tab that fills its viewport while there is room and keeps its own height once there is not,
	 * which is what makes the scroll pane above do both things
	 */
	private static final class GrowingContent extends JPanel implements Scrollable
	{
		private static final long serialVersionUID = 1L;

		private GrowingContent(JPanel content)
		{
			super(new BorderLayout());
			add(content, BorderLayout.CENTER);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
			int direction)
		{
			return orientation == SwingConstants.VERTICAL
				? visibleRect.height
				: visibleRect.width;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return getParent() instanceof JViewport viewport
				&& viewport.getHeight() >= getPreferredSize().height;
		}
	}

	private void onNewKeyPair()
	{
		run("made", () -> {
			KeyExchangeSupport.Party party = KeyExchangeSupport
				.newParty(modelObject.getAlgorithm());
			modelObject.setParty(party);
			modelObject.setMyPrivateKey(KeyExchangeSupport.privateKeyOf(party));
			show(txtMyPublicKey, KeyExchangeSupport.publicKeyOf(party),
				modelObject::setMyPublicKey);
			modelObject.setRecipientSecret(null);
			lblMyFingerprint.setText("-");
			return "made a " + modelObject.getAlgorithm() + " key pair";
		});
	}

	private void onDecapsulate()
	{
		run("derived", () -> {
			if (modelObject.getParty() == null)
			{
				throw new IllegalStateException("make or load a key pair first");
			}
			modelObject.setRecipientSecret(KeyExchangeSupport.decapsulate(modelObject.getParty(),
				modelObject.getHandshakeIn()));
			lblMyFingerprint
				.setText(KeyExchangeSupport.fingerprintOf(modelObject.getRecipientSecret()));
			return "derived the shared secret";
		});
	}

	private void onEncapsulate()
	{
		run("made", () -> {
			KeyExchangeSupport.Handshake handshake = KeyExchangeSupport
				.encapsulate(modelObject.getTheirPublicKey());
			modelObject.setSenderSecret(handshake.sharedSecret());
			show(txtHandshakeOut, handshake.handshake(), modelObject::setHandshakeOut);
			lblTheirFingerprint
				.setText(KeyExchangeSupport.fingerprintOf(handshake.sharedSecret()));
			return "made a shared secret for "
				+ KeyExchangeSupport.algorithmOf(modelObject.getTheirPublicKey());
		});
	}

	private void onEncryptMessage()
	{
		run("encrypted", () -> {
			if (modelObject.getSenderSecret() == null)
			{
				throw new IllegalStateException("make a shared secret first");
			}
			show(txtEncryptedOut,
				KeyExchangeSupport.encryptMessage(modelObject.getSenderSecret(),
					modelObject.getMessageToSend().getBytes(StandardCharsets.UTF_8)),
				modelObject::setEncryptedOut);
			return "encrypted " + modelObject.getMessageToSend().length() + " characters";
		});
	}

	private void onDecryptMessage()
	{
		run("read", () -> {
			if (modelObject.getRecipientSecret() == null)
			{
				throw new IllegalStateException("derive the shared secret first");
			}
			show(txtMessageReceived,
				new String(KeyExchangeSupport.decryptMessage(modelObject.getRecipientSecret(),
					modelObject.getEncryptedIn()), StandardCharsets.UTF_8),
				modelObject::setMessageReceived);
			return "read " + modelObject.getMessageReceived().length() + " characters";
		});
	}

	private void onSaveMyKey()
	{
		if (modelObject.getMyPrivateKey().isEmpty())
		{
			showResult("not saved: make a key pair first");
			return;
		}
		if (JOptionPane.showConfirmDialog(this,
			"This file holds the private half. Whoever has it can read everything\n"
				+ "that is ever sent to this key. Save it?",
			"Save the private key", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
		{
			return;
		}
		onSaveToFile(modelObject.getMyPrivateKey(), "private key");
	}

	private void onLoadMyKey()
	{
		run("loaded", () -> {
			File file = chooseFile(false);
			if (file == null)
			{
				return modelObject.getResultText();
			}
			String stored = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
			KeyExchangeSupport.Party party = KeyExchangeSupport.partyFrom(stored);
			modelObject.setParty(party);
			modelObject.setMyPrivateKey(stored);
			modelObject.setAlgorithm(party.algorithm());
			cmbAlgorithm.setSelectedItem(party.algorithm());
			show(txtMyPublicKey, KeyExchangeSupport.publicKeyOf(party),
				modelObject::setMyPublicKey);
			modelObject.setRecipientSecret(null);
			lblMyFingerprint.setText("-");
			return "loaded a " + party.algorithm() + " key pair";
		});
	}

	private void onSaveToFile(String content, String what)
	{
		run("saved", () -> {
			if (content == null || content.isBlank())
			{
				throw new IllegalStateException("there is no " + what + " to save yet");
			}
			File file = chooseFile(true);
			if (file == null)
			{
				return modelObject.getResultText();
			}
			Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
			return "saved the " + what + " to " + file.getName();
		});
	}

	private void onLoadInto(JMTextArea target, java.util.function.Consumer<String> intoModel)
	{
		run("loaded", () -> {
			File file = chooseFile(false);
			if (file == null)
			{
				return modelObject.getResultText();
			}
			show(target, Files.readString(file.toPath(), StandardCharsets.UTF_8).trim(), intoModel);
			return "loaded " + file.getName();
		});
	}

	private File chooseFile(boolean toSave)
	{
		JFileChooser chooser = new JFileChooser();
		int answer = toSave ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
		return answer == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}

	/** Puts a value into the model and shows it from its beginning */
	private void show(JMTextArea area, String value, java.util.function.Consumer<String> intoModel)
	{
		intoModel.accept(value);
		area.setText(value);
		area.setCaretPosition(0);
	}

	private void showResult(String resultText)
	{
		modelObject.setResultText(resultText);
		lblResult.setText(modelObject.getResultText());
	}

	private void run(String what, KeyExchangeOperation operation)
	{
		try
		{
			showResult(operation.execute());
		}
		catch (Exception exception)
		{
			showResult("not " + what + ": " + message(exception));
		}
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() == null || exception.getMessage().isBlank()
			? exception.getClass().getSimpleName()
			: exception.getMessage();
	}

	/** What a button of this panel does: it produces the line shown at the bottom, or it throws */
	@FunctionalInterface
	private interface KeyExchangeOperation
	{
		String execute() throws Exception;
	}

	/**
	 * Wraps a text area of this panel in the scroll pane the shared layout shows it in, so the area
	 * keeps a usable minimum width when the window is narrower than the tab wants
	 *
	 * @param area
	 *            the area to wrap
	 * @return the scroll pane around it
	 */
	private static JScrollPane scrolled(JMTextArea area)
	{
		area.setLineWrap(true);
		area.setWrapStyleWord(false);
		return ToolForm.scrolled(area);
	}

	/**
	 * The line that reports a shared secret: its fingerprint next to what it belongs to, sitting at
	 * the left edge like the button rows above it
	 *
	 * @param label
	 *            what the fingerprint belongs to
	 * @param fingerprint
	 *            the label that carries the fingerprint
	 * @return the row to add to the tab
	 */
	private static JPanel fingerprintRow(String label, JLabel fingerprint)
	{
		JPanel row = new JPanel(new MigLayout("insets 0, gap 6", "[]", "[]"));
		row.setOpaque(false);
		row.add(new JLabel(label));
		fingerprint.setFont(new Font(Font.MONOSPACED, Font.BOLD, fingerprint.getFont().getSize()));
		row.add(fingerprint);
		row.add(new JLabel("(both sides must read the same)"));
		return row;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}
}
