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
 * The state of {@link LethenonSyncPanel}, in one object rather than scattered across its widgets.
 * <p>
 * Every component of the panel is bound to a property here (architecture.md: every panel holds its
 * state in a model), and so is whether a sync is running: it runs off the event dispatch thread,
 * and a test or a second panel can tell from here when it has ended.
 * <p>
 * The answer is kept as the window shows it, one value per field (#604): the sentence that says
 * whether the chain file is up to date, how many blocks were taken, the last block and the node,
 * and behind "Show details" the chain and whether its first block was taken on first use.
 */
public class LethenonSyncPanelModel
{

	private String chainFile = "";

	private String node = "";

	private String proxy = "";

	private String answer = "";

	private String taken = "";

	private String lastBlock = "";

	private String from = "";

	private String chain = "";

	private String firstUse = "";

	private boolean detailsShown;

	private String resultText = " ";

	private volatile boolean synchronising;

	public String getChainFile()
	{
		return chainFile;
	}

	public void setChainFile(String chainFile)
	{
		this.chainFile = chainFile;
	}

	public String getNode()
	{
		return node;
	}

	public void setNode(String node)
	{
		this.node = node;
	}

	public String getProxy()
	{
		return proxy;
	}

	public void setProxy(String proxy)
	{
		this.proxy = proxy;
	}

	/** Empties every value of the answer, so a failure leaves nothing of an earlier one */
	public void clearAnswer()
	{
		answer = "";
		taken = "";
		lastBlock = "";
		from = "";
		chain = "";
		firstUse = "";
	}

	public String getAnswer()
	{
		return answer;
	}

	public void setAnswer(String answer)
	{
		this.answer = answer;
	}

	public String getTaken()
	{
		return taken;
	}

	public void setTaken(String taken)
	{
		this.taken = taken;
	}

	public String getLastBlock()
	{
		return lastBlock;
	}

	public void setLastBlock(String lastBlock)
	{
		this.lastBlock = lastBlock;
	}

	public String getFrom()
	{
		return from;
	}

	public void setFrom(String from)
	{
		this.from = from;
	}

	public String getChain()
	{
		return chain;
	}

	public void setChain(String chain)
	{
		this.chain = chain;
	}

	public String getFirstUse()
	{
		return firstUse;
	}

	public void setFirstUse(String firstUse)
	{
		this.firstUse = firstUse;
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

	public boolean isSynchronising()
	{
		return synchronising;
	}

	public void setSynchronising(boolean synchronising)
	{
		this.synchronising = synchronising;
	}
}
