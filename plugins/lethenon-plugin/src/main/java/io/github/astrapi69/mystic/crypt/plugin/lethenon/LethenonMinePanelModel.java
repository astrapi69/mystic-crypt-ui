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

import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;

/**
 * The state of {@link LethenonMinePanel}, in one object rather than scattered across its widgets
 * (architecture.md: every panel holds its state in a model).
 * <p>
 * The answer is kept as the window shows it, one value per field (#604): the sentence that answers
 * what the window was opened for, the block's number, what it paid, the transfers it carries and
 * the pun as it went into the block, and behind "Show details" the chain, the account paid, what
 * the replay checked and all the money there is.
 * <p>
 * The password is a {@code char[]}, never a String, and {@link #setPassword} overwrites the array
 * it replaces - the same rule as the other windows of this plugin and the host's sign-in model
 * (#351).
 */
public class LethenonMinePanelModel
{

	/** The pun mining starts from unless somebody types another, lethenon's own default */
	public static final String DEFAULT_PUN = "watching is not protecting";

	private String chainFile = "";

	private String walletFile = "";

	private transient char[] password = new char[0];

	private ChainKind chainKind = ChainKind.TEST_NETWORK;

	private String pun = DEFAULT_PUN;

	private String answer = "";

	private String block = "";

	private String minedPun = "";

	private String paid = "";

	private String carried = "";

	private String chain = "";

	private String paidTo = "";

	private String chainChecked = "";

	private String supply = "";

	private boolean detailsShown;

	private String resultText = " ";

	/** Empties every value of the answer, so a failure leaves nothing of an earlier block */
	public void clearAnswer()
	{
		answer = "";
		block = "";
		minedPun = "";
		paid = "";
		carried = "";
		chain = "";
		paidTo = "";
		chainChecked = "";
		supply = "";
	}

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

	public void setPassword(char[] password)
	{
		char[] replacement = password == null ? new char[0] : password;
		if (!java.util.Arrays.equals(this.password, replacement))
		{
			SecretBuffers.wipe(this.password);
		}
		this.password = replacement;
	}

	public ChainKind getChainKind()
	{
		return chainKind;
	}

	public void setChainKind(ChainKind chainKind)
	{
		this.chainKind = chainKind;
	}

	public String getPun()
	{
		return pun;
	}

	public void setPun(String pun)
	{
		this.pun = pun;
	}

	public String getAnswer()
	{
		return answer;
	}

	public void setAnswer(String answer)
	{
		this.answer = answer;
	}

	public String getBlock()
	{
		return block;
	}

	public void setBlock(String block)
	{
		this.block = block;
	}

	public String getMinedPun()
	{
		return minedPun;
	}

	public void setMinedPun(String minedPun)
	{
		this.minedPun = minedPun;
	}

	public String getPaid()
	{
		return paid;
	}

	public void setPaid(String paid)
	{
		this.paid = paid;
	}

	public String getCarried()
	{
		return carried;
	}

	public void setCarried(String carried)
	{
		this.carried = carried;
	}

	public String getChain()
	{
		return chain;
	}

	public void setChain(String chain)
	{
		this.chain = chain;
	}

	public String getPaidTo()
	{
		return paidTo;
	}

	public void setPaidTo(String paidTo)
	{
		this.paidTo = paidTo;
	}

	public String getChainChecked()
	{
		return chainChecked;
	}

	public void setChainChecked(String chainChecked)
	{
		this.chainChecked = chainChecked;
	}

	public String getSupply()
	{
		return supply;
	}

	public void setSupply(String supply)
	{
		this.supply = supply;
	}

	public boolean isDetailsShown()
	{
		return detailsShown;
	}

	public void setDetailsShown(boolean detailsShown)
	{
		this.detailsShown = detailsShown;
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
