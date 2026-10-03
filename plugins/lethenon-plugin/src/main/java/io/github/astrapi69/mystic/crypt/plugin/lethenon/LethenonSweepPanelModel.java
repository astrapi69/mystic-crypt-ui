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

import io.github.astrapi69.mystic.crypt.vault.SecretBuffers;

/**
 * The state of {@link LethenonSweepPanel}, in one object rather than scattered across its widgets
 * (architecture.md: every panel holds its state in a model).
 * <p>
 * The password is a {@code char[]}, never a String, and {@link #setPassword} overwrites the array
 * it replaces - the same rule as {@link LethenonSendPanelModel} and the host's sign-in model
 * (#351).
 */
public class LethenonSweepPanelModel
{

	/** The memo lethenon's {@code sweep} signs when none is given, so both write the same */
	public static final String DEFAULT_MEMO = "swept from a one-time destination";

	private String chainFile = "";

	private String walletFile = "";

	private transient char[] password = new char[0];

	private String fee = "0";

	private String memo = DEFAULT_MEMO;

	private String costStatement = "";

	private String report = "";

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

	/**
	 * The password typed so far
	 *
	 * @return the array itself, not a copy; never null
	 */
	public char[] getPassword()
	{
		return password;
	}

	/**
	 * Replaces the password, overwriting the array it replaces unless both hold the same characters
	 * (#351: an equal-content replacement re-enters from the field's own document listener while
	 * its caller still holds the first array)
	 *
	 * @param password
	 *            the new password; null clears it
	 */
	public void setPassword(char[] password)
	{
		char[] replacement = password == null ? new char[0] : password;
		if (!java.util.Arrays.equals(this.password, replacement))
		{
			SecretBuffers.wipe(this.password);
		}
		this.password = replacement;
	}

	public String getFee()
	{
		return fee;
	}

	public void setFee(String fee)
	{
		this.fee = fee;
	}

	public String getMemo()
	{
		return memo;
	}

	public void setMemo(String memo)
	{
		this.memo = memo;
	}

	/**
	 * What sweeping costs, as the window states it before anything is signed
	 *
	 * @return the statement
	 */
	public String getCostStatement()
	{
		return costStatement;
	}

	public void setCostStatement(String costStatement)
	{
		this.costStatement = costStatement;
	}

	public String getReport()
	{
		return report;
	}

	public void setReport(String report)
	{
		this.report = report;
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
