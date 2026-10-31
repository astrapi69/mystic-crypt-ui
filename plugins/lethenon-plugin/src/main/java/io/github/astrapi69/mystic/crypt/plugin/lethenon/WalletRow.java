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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * The row of a lethenon window that says which wallet an action signs with (#706, README defect
 * G1): the wallet in the vault the overview shows, named and never a file, or, while the overview
 * shows none, the wallet file field, which still works for a file not taken over yet. One component
 * for every window that signs, so they cannot drift apart.
 */
final class WalletRow
{

	private final LethenonVault vault;

	private final Locale locale;

	private final JMTextField txtWalletFile;

	private final JMTextField txtFromWallet;

	private final JLabel lblFromWallet = new JLabel();

	private final JLabel lblWalletFile = new JLabel();

	private final JButton btnBrowseWalletFile;

	private Runnable unfollow = () -> {
	};

	/**
	 * Instantiates a new {@link WalletRow}
	 *
	 * @param vault
	 *            the plugin's view of the vault, whose wallet the row follows
	 * @param locale
	 *            the language of the window
	 * @param walletFile
	 *            the window's wallet file field, bound to its model
	 * @param browse
	 *            what the file chooser button does
	 */
	WalletRow(final LethenonVault vault, final Locale locale, final JMTextField walletFile,
		final Runnable browse)
	{
		this.vault = vault;
		this.locale = locale;
		this.txtWalletFile = walletFile;
		txtFromWallet = AnswerFields.readOnly("txtFromWallet", text(
			"lethenon.send.tooltip.from.wallet",
			"the wallet in the vault the overview shows; its password is asked for this action only"));
		lblFromWallet.setText(text("lethenon.send.label.from.wallet", "Wallet:"));
		lblWalletFile.setText(text("lethenon.input.label.wallet.file", "Wallet file:"));
		btnBrowseWalletFile = LethenonSwing.button("btnBrowseWalletFile", "...",
			event -> browse.run(),
			text("lethenon.input.tooltip.browse.wallet.file", "pick your wallet file on your disk"));
	}

	/**
	 * Adds the row to a window's form, following the vault's wallet until {@link #stop()}
	 *
	 * @param form
	 *            the form, laid out by {@link ToolForm}
	 */
	void addTo(final JPanel form)
	{
		form.add(lblFromWallet, "hidemode 3");
		form.add(txtFromWallet, ToolForm.FIELD + ", hidemode 3");
		form.add(lblWalletFile, "hidemode 3");
		form.add(txtWalletFile, "growx, split 2, hidemode 3");
		form.add(btnBrowseWalletFile, "alignx left, width pref!, hidemode 3");
		unfollow = vault.whenTheWalletChanges(wallet -> show());
		show();
	}

	/**
	 * Where the wallet comes from for one action: the vault's wallet, unsealed in memory with the
	 * password, or the wallet file. The caller overwrites the password after the action
	 *
	 * @param password
	 *            the password the person typed
	 * @param walletFile
	 *            the wallet file, used only while the overview shows no wallet
	 * @return the source
	 */
	WalletSource source(final char[] password, final Path walletFile)
	{
		Optional<VaultWallet> chosen = vault.chosen();
		return chosen.isPresent() ? () -> vault.open(chosen.get().id(), password).wallet()
			: () -> LethenonWallets.open(walletFile, password);
	}

	/**
	 * Enables or disables the wallet's fields, for a window where signing is optional
	 *
	 * @param enabled
	 *            whether the wallet is needed
	 */
	void setEnabled(final boolean enabled)
	{
		txtWalletFile.setEnabled(enabled);
		btnBrowseWalletFile.setEnabled(enabled);
		txtFromWallet.setEnabled(enabled);
	}

	/** Stops following the vault's wallet, when the window closes */
	void stop()
	{
		unfollow.run();
	}

	private void show()
	{
		Optional<VaultWallet> chosen = vault.chosen();
		boolean fromTheVault = chosen.isPresent();
		chosen.ifPresent(wallet -> AnswerFields.setText(txtFromWallet, LethenonMessages
			.format("lethenon.send.from.wallet", "{0}, from the vault", locale, wallet.title())));
		lblFromWallet.setVisible(fromTheVault);
		txtFromWallet.setVisible(fromTheVault);
		lblWalletFile.setVisible(!fromTheVault);
		txtWalletFile.setVisible(!fromTheVault);
		btnBrowseWalletFile.setVisible(!fromTheVault);
		if (txtWalletFile.getParent() != null)
		{
			txtWalletFile.getParent().revalidate();
		}
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}
}
