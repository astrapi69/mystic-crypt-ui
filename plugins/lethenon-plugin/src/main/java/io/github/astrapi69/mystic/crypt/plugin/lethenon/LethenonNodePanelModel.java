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
 * The state of {@link LethenonNodePanel}, in one object rather than scattered across its widgets.
 * <p>
 * Every component of the panel is bound to a property here (architecture.md: every panel holds its
 * state in a model), and so is what the node does: whether it is starting or running, the port it
 * listens on, and the status the window shows every second. Starting and stopping run off the event
 * dispatch thread and the status changes while the node runs, so these and the result line are
 * volatile, and a test or a second panel can tell from here when something has ended.
 */
public class LethenonNodePanelModel
{

	/** The port a node listens on unless another is typed: the one lethenon's examples use */
	public static final String DEFAULT_PORT = "18480";

	private String chainFile = "";

	private String port = DEFAULT_PORT;

	private String peers = "";

	private boolean mine;

	private String walletFile = "";

	private char[] password;

	private String pun = LethenonMinePanelModel.DEFAULT_PUN;

	private volatile String status = "";

	private volatile String resultText = " ";

	private volatile boolean starting;

	private volatile boolean running;

	private volatile int listeningPort;

	public String getChainFile()
	{
		return chainFile;
	}

	public void setChainFile(String chainFile)
	{
		this.chainFile = chainFile;
	}

	public String getPort()
	{
		return port;
	}

	public void setPort(String port)
	{
		this.port = port;
	}

	public String getPeers()
	{
		return peers;
	}

	public void setPeers(String peers)
	{
		this.peers = peers;
	}

	public boolean isMine()
	{
		return mine;
	}

	public void setMine(boolean mine)
	{
		this.mine = mine;
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
		this.password = password;
	}

	public String getPun()
	{
		return pun;
	}

	public void setPun(String pun)
	{
		this.pun = pun;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getResultText()
	{
		return resultText;
	}

	public void setResultText(String resultText)
	{
		this.resultText = resultText;
	}

	public boolean isStarting()
	{
		return starting;
	}

	public void setStarting(boolean starting)
	{
		this.starting = starting;
	}

	public boolean isRunning()
	{
		return running;
	}

	public void setRunning(boolean running)
	{
		this.running = running;
	}

	public int getListeningPort()
	{
		return listeningPort;
	}

	public void setListeningPort(int listeningPort)
	{
		this.listeningPort = listeningPort;
	}
}
