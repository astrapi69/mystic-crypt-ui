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

/**
 * The state of {@link LethenonChainPanel}, in one object rather than scattered across its widgets.
 * <p>
 * Every component of the panel is bound to a property here (architecture.md: every panel holds its
 * state in a model), so what the panel currently holds is readable from a test, from a second
 * panel, or from whatever drives it next - not only from inside an action listener.
 * <p>
 * The answer is kept as the window shows it, one value per field (#604): the sentence that says
 * whether the chain is in order, the blocks, transfers and signatures checked and all the money
 * there is, and behind "Show details" the chain library's own sentence about the replay.
 */
public class LethenonChainPanelModel
{

	private String chainFile = "";

	private String answer = "";

	private String blocks = "";

	private String transfers = "";

	private String signatures = "";

	private String supply = "";

	private String summary = "";

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

	/** Empties every value of the answer, so a failure leaves nothing of an earlier one */
	public void clearAnswer()
	{
		answer = "";
		blocks = "";
		transfers = "";
		signatures = "";
		supply = "";
		summary = "";
	}

	public String getAnswer()
	{
		return answer;
	}

	public void setAnswer(String answer)
	{
		this.answer = answer;
	}

	public String getBlocks()
	{
		return blocks;
	}

	public void setBlocks(String blocks)
	{
		this.blocks = blocks;
	}

	public String getTransfers()
	{
		return transfers;
	}

	public void setTransfers(String transfers)
	{
		this.transfers = transfers;
	}

	public String getSignatures()
	{
		return signatures;
	}

	public void setSignatures(String signatures)
	{
		this.signatures = signatures;
	}

	public String getSupply()
	{
		return supply;
	}

	public void setSupply(String supply)
	{
		this.supply = supply;
	}

	public String getSummary()
	{
		return summary;
	}

	public void setSummary(String summary)
	{
		this.summary = summary;
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
