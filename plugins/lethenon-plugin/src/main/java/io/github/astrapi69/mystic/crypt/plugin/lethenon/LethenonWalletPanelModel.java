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

import java.util.List;

import io.github.astrapi69.lethenon.SeedPhrase;

/**
 * The state of {@link LethenonWalletPanel}, in one object rather than scattered across its widgets
 * (architecture.md): which view is shown, the wallets of the vault and the one chosen, what the
 * forms hold and why the last action was refused.
 * <p>
 * Passwords are {@code char[]}, never Strings, and are overwritten when they
 * are replaced or cleared. The words typed for a restore are 24 Strings, one per field of its
 * {@link WordGrid}, because text fields hold them; they are emptied after every restore, and the
 * decision record names that limit.
 */
public class LethenonWalletPanelModel
{

	private WalletView view = WalletView.NO_VAULT;

	private List<VaultWallet> wallets = List.of();

	private VaultWallet chosen;

	private WalletNetwork network = WalletNetwork.TESTNET;

	private char[] password = new char[0];

	private char[] passwordRepeated = new char[0];

	private String walletFile = "";

	private final TypedWords words = new TypedWords();

	private String backupNotice = "";

	private String balance = "";

	private String balanceExact = "";

	private String refusal = "";

	private String notice = "";

	public WalletView getView()
	{
		return view;
	}

	public void setView(WalletView view)
	{
		this.view = view;
	}

	public List<VaultWallet> getWallets()
	{
		return wallets;
	}

	public void setWallets(List<VaultWallet> wallets)
	{
		this.wallets = List.copyOf(wallets);
	}

	public VaultWallet getChosen()
	{
		return chosen;
	}

	public void setChosen(VaultWallet chosen)
	{
		this.chosen = chosen;
	}

	public WalletNetwork getNetwork()
	{
		return network;
	}

	public void setNetwork(WalletNetwork network)
	{
		this.network = network == null ? WalletNetwork.TESTNET : network;
	}

	public char[] getPassword()
	{
		return password;
	}

	public void setPassword(char[] password)
	{
		this.password = SecretFields.replaced(this.password, password);
	}

	public char[] getPasswordRepeated()
	{
		return passwordRepeated;
	}

	public void setPasswordRepeated(char[] passwordRepeated)
	{
		this.passwordRepeated = SecretFields.replaced(this.passwordRepeated, passwordRepeated);
	}

	public String getWalletFile()
	{
		return walletFile;
	}

	public void setWalletFile(String walletFile)
	{
		this.walletFile = walletFile == null ? "" : walletFile;
	}

	/**
	 * Gets the words of the restore, as the restore reads them
	 *
	 * @return the 24 positions in order, separated by one space
	 */
	public String getWords()
	{
		return words.phrase();
	}

	/**
	 * Sets the words of the restore as a paste does: each word of the phrase at its position
	 *
	 * @param words
	 *            the phrase; a missing one empties every position
	 */
	public void setWords(String words)
	{
		this.words.spread(SeedPhrase.parse(words == null ? "" : words));
	}

	/**
	 * Gets the 24 positions the restore's grid writes to (#804)
	 *
	 * @return the positions
	 */
	TypedWords getTypedWords()
	{
		return words;
	}

	public String getBalance()
	{
		return balance;
	}

	public void setBalance(String balance)
	{
		this.balance = balance == null ? "" : balance;
	}

	public String getBalanceExact()
	{
		return balanceExact;
	}

	public void setBalanceExact(String balanceExact)
	{
		this.balanceExact = balanceExact == null ? "" : balanceExact;
	}

	public String getBackupNotice()
	{
		return backupNotice;
	}

	public void setBackupNotice(String backupNotice)
	{
		this.backupNotice = backupNotice == null ? "" : backupNotice;
	}

	public String getRefusal()
	{
		return refusal;
	}

	public void setRefusal(String refusal)
	{
		this.refusal = refusal == null ? "" : refusal;
	}

	public String getNotice()
	{
		return notice;
	}

	public void setNotice(String notice)
	{
		this.notice = notice == null ? "" : notice;
	}

}
