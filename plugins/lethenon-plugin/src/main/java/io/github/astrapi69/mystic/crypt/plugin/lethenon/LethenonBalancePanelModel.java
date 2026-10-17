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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;

/**
 * The state of {@link LethenonBalancePanel}, in one object rather than scattered across its widgets
 * (architecture.md: every panel holds its state in a model).
 * <p>
 * The answer is kept as the window shows it, one value per field (#604): the sentence that answers
 * the question, the address, each account and what it holds, what waits for a sweep, and behind
 * "Show details" what the replay checked and all the money there is. The address and the accounts
 * are kept whole; the window shortens what is too long to read, and its Copy buttons copy these.
 * <p>
 * The password is a {@code char[]}, never a String: an array can be overwritten, a String waits for
 * the garbage collector. {@link #setPassword} overwrites the array it replaces, the way the host's
 * sign-in model does (#351), because a password field hands out a fresh array on every keystroke.
 */
public class LethenonBalancePanelModel
{

	private String chainFile = "";

	private String walletFile = "";

	private transient char[] password = new char[0];

	private String answer = "";

	private String address = "";

	private final Map<String, String> accounts = new LinkedHashMap<>();

	private final Map<String, String> holds = new LinkedHashMap<>();

	private String waitingForSweep = "";

	private String chainChecked = "";

	private String supply = "";

	private boolean detailsShown;

	private String resultText = " ";

	public String getChainFile()
	{
		return chainFile;
	}

	public void setChainFile(String chainFile)
	{
		this.chainFile = chainFile;
	}

	public String getWalletFile()
	{
		return walletFile;
	}

	public void setWalletFile(String walletFile)
	{
		this.walletFile = walletFile;
	}

	public char[] getPassword()
	{
		return password;
	}

	/**
	 * Replaces the password, overwriting the array it replaces unless both hold the same characters
	 * - the host's rule (#351): an equal-content replacement re-enters from the field's own
	 * document listener while its caller still holds the first array
	 *
	 * @param password
	 *            the new password; null clears it
	 */
	public void setPassword(char[] password)
	{
		char[] replacement = password == null ? new char[0] : password;
		if (!Arrays.equals(this.password, replacement))
		{
			SecretBuffers.wipe(this.password);
		}
		this.password = replacement;
	}

	/** @return the sentence that answers what the window was opened for, empty before an answer */
	public String getAnswer()
	{
		return answer;
	}

	public void setAnswer(String answer)
	{
		this.answer = answer;
	}

	/** @return the wallet's published address, whole */
	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	/**
	 * Gets the wallet's account of a signature suite, whole
	 *
	 * @param suite
	 *            the suite's identifier, such as {@code ed25519}
	 * @return the account in hexadecimal, empty before an answer
	 */
	public String getAccount(final String suite)
	{
		return accounts.getOrDefault(suite, "");
	}

	public void setAccount(final String suite, final String account)
	{
		accounts.put(suite, account);
	}

	/**
	 * Gets what the wallet's account of a signature suite holds
	 *
	 * @param suite
	 *            the suite's identifier, such as {@code ed25519}
	 * @return the amount with its unit, empty before an answer
	 */
	public String getHolds(final String suite)
	{
		return holds.getOrDefault(suite, "");
	}

	public void setHolds(final String suite, final String amount)
	{
		holds.put(suite, amount);
	}

	/** @return how many payments wait for a sweep and what they hold together */
	public String getWaitingForSweep()
	{
		return waitingForSweep;
	}

	public void setWaitingForSweep(String waitingForSweep)
	{
		this.waitingForSweep = waitingForSweep;
	}

	/** @return how many blocks, transfers and signatures the replay checked */
	public String getChainChecked()
	{
		return chainChecked;
	}

	public void setChainChecked(String chainChecked)
	{
		this.chainChecked = chainChecked;
	}

	/** @return all the money there is on the chain */
	public String getSupply()
	{
		return supply;
	}

	public void setSupply(String supply)
	{
		this.supply = supply;
	}

	/** @return whether "Show details" is open */
	public boolean isDetailsShown()
	{
		return detailsShown;
	}

	public void setDetailsShown(boolean detailsShown)
	{
		this.detailsShown = detailsShown;
	}

	/** Forgets every value of the last answer, so nothing of it outlives a failed attempt */
	public void clearAnswer()
	{
		answer = "";
		address = "";
		accounts.clear();
		holds.clear();
		waitingForSweep = "";
		chainChecked = "";
		supply = "";
	}

	public String getResultText()
	{
		return resultText;
	}

	public void setResultText(String resultText)
	{
		this.resultText = resultText;
	}
}
