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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.*;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextArea;

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
 */
public class KeyExchangePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

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
		super(new BorderLayout(4, 4));

		bindToModel();

		cmbAlgorithm.setName("cmbAlgorithm");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		lblMyFingerprint.setName("lblMyFingerprint");
		lblTheirFingerprint.setName("lblTheirFingerprint");
		tabs.setName("tabKeyExchange");
		tabs.addTab("Receive", newReceiveTab());
		tabs.addTab("Send", newSendTab());

		JPanel top = new JPanel();
		top.add(new JLabel("Algorithm:"));
		top.add(cmbAlgorithm);
		add(top, BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);
		add(lblResult, BorderLayout.SOUTH);
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
		txtHandshakeIn.setName("txtHandshakeIn");
		txtEncryptedIn.setName("txtEncryptedIn");
		txtMessageReceived.setName("txtMessageReceived");

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(buttonRow(button("btnNewKeyPair", "New key pair", event -> onNewKeyPair()),
			button("btnSaveMyKey", "Save my key...", event -> onSaveMyKey()),
			button("btnLoadMyKey", "Load my key...", event -> onLoadMyKey())),
			across(row++));
		panel.add(new JLabel("1. Hand this public key to the other side:"), across(row++));
		panel.add(scrolled(txtMyPublicKey), across(row++));
		panel.add(buttonRow(button("btnSaveMyPublicKey", "Save public key...",
			event -> onSaveToFile(modelObject.getMyPublicKey(), "public key"))), across(row++));
		panel.add(new JLabel("2. Paste the handshake that came back:"), across(row++));
		panel.add(scrolled(txtHandshakeIn), across(row++));
		panel.add(buttonRow(button("btnLoadHandshake", "Load...",
			event -> onLoadInto(txtHandshakeIn, modelObject::setHandshakeIn)),
			button("btnDecapsulate", "Derive the secret", event -> onDecapsulate())),
			across(row++));
		panel.add(fingerprintRow("My secret:", lblMyFingerprint), across(row++));
		panel.add(new JLabel("3. A message that arrived with it:"), across(row++));
		panel.add(scrolled(txtEncryptedIn), across(row++));
		panel.add(buttonRow(button("btnDecryptMessage", "Read it", event -> onDecryptMessage())),
			across(row++));
		panel.add(scrolled(txtMessageReceived), across(row));
		return panel;
	}

	private JPanel newSendTab()
	{
		txtTheirPublicKey.setName("txtTheirPublicKey");
		txtHandshakeOut.setName("txtHandshakeOut");
		txtMessageToSend.setName("txtMessageToSend");
		txtEncryptedOut.setName("txtEncryptedOut");

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("1. Paste the public key you were given:"), across(row++));
		panel.add(scrolled(txtTheirPublicKey), across(row++));
		panel.add(buttonRow(button("btnLoadTheirPublicKey", "Load...",
			event -> onLoadInto(txtTheirPublicKey, modelObject::setTheirPublicKey)),
			button("btnEncapsulate", "Make a shared secret", event -> onEncapsulate())),
			across(row++));
		panel.add(fingerprintRow("Shared secret:", lblTheirFingerprint), across(row++));
		panel.add(new JLabel("2. Send this handshake back:"), across(row++));
		panel.add(scrolled(txtHandshakeOut), across(row++));
		panel.add(buttonRow(button("btnSaveHandshake", "Save...",
			event -> onSaveToFile(modelObject.getHandshakeOut(), "handshake"))), across(row++));
		panel.add(new JLabel("3. A message to send with it:"), across(row++));
		panel.add(scrolled(txtMessageToSend), across(row++));
		panel.add(buttonRow(button("btnEncryptMessage", "Encrypt it", event -> onEncryptMessage())),
			across(row++));
		panel.add(scrolled(txtEncryptedOut), across(row++));
		panel.add(buttonRow(button("btnSaveEncrypted", "Save...",
			event -> onSaveToFile(modelObject.getEncryptedOut(), "message"))), across(row));
		return panel;
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

	private static JScrollPane scrolled(JMTextArea area)
	{
		area.setLineWrap(true);
		area.setWrapStyleWord(false);
		return new JScrollPane(area);
	}

	private static JPanel fingerprintRow(String label, JLabel fingerprint)
	{
		JPanel row = new JPanel();
		row.add(new JLabel(label));
		fingerprint.setFont(new Font(Font.MONOSPACED, Font.BOLD, fingerprint.getFont().getSize()));
		row.add(fingerprint);
		row.add(new JLabel("(both sides must read the same)"));
		return row;
	}

	private static JPanel buttonRow(JButton... buttons)
	{
		JPanel row = new JPanel();
		for (JButton button : buttons)
		{
			row.add(button);
		}
		return row;
	}

	private static JButton button(String name, String text,
		java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}

	private static GridBagConstraints across(int row)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(2, 6, 2, 6);
		return constraints;
	}
}
