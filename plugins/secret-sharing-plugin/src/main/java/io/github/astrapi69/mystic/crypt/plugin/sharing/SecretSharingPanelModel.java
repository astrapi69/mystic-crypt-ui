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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

/**
 * The state of the {@link SecretSharingPanel}: the secret that is to be split - typed in or taken
 * from a file - how many shares are produced and how many of them are needed, the share lines
 * themselves, the secret rebuilt from them and the file it is written to, and the message shown at
 * the bottom of the panel.
 * <p>
 * Every component of the panel is bound to one of these fields, so the state can be read here at
 * any moment instead of out of the widgets. The typed secret stays a {@code char[]}, it is never
 * held as a {@link String}.
 */
public class SecretSharingPanelModel
{

	/** The secret typed into the password field */
	private char[] secret = new char[0];

	/**
	 * The path of the file that carries the secret, or that the shares are read from and written to
	 */
	private String secretFile = "";

	/** Whether the file is split instead of the typed secret */
	private boolean useFile;

	/** How many shares are needed to rebuild the secret */
	private int threshold;

	/** How many shares are produced */
	private int totalShares;

	/** The share lines, one share per line */
	private String shares = "";

	/** The secret as it was rebuilt from the shares */
	private String rebuiltSecret = "";

	/** The path of the file the rebuilt secret is written to */
	private String rebuiltFile = "";

	/** The message shown at the bottom of the panel */
	private String resultText = " ";

	/**
	 * Gets the secret typed into the password field
	 *
	 * @return the typed secret, empty if nothing was typed
	 */
	public char[] getSecret()
	{
		return secret;
	}

	/**
	 * Sets the secret typed into the password field
	 *
	 * @param secret
	 *            the typed secret
	 */
	public void setSecret(char[] secret)
	{
		this.secret = secret;
	}

	/**
	 * Gets the path of the file that carries the secret, or that the shares are read from and
	 * written to
	 *
	 * @return the path, empty if none was chosen
	 */
	public String getSecretFile()
	{
		return secretFile;
	}

	/**
	 * Sets the path of the file that carries the secret, or that the shares are read from and
	 * written to
	 *
	 * @param secretFile
	 *            the path
	 */
	public void setSecretFile(String secretFile)
	{
		this.secretFile = secretFile;
	}

	/**
	 * Whether the file is split instead of the typed secret
	 *
	 * @return true if the file is split, otherwise false
	 */
	public boolean isUseFile()
	{
		return useFile;
	}

	/**
	 * Sets whether the file is split instead of the typed secret
	 *
	 * @param useFile
	 *            true if the file is split, otherwise false
	 */
	public void setUseFile(boolean useFile)
	{
		this.useFile = useFile;
	}

	/**
	 * Gets how many shares are needed to rebuild the secret
	 *
	 * @return the threshold
	 */
	public int getThreshold()
	{
		return threshold;
	}

	/**
	 * Sets how many shares are needed to rebuild the secret
	 *
	 * @param threshold
	 *            the threshold
	 */
	public void setThreshold(int threshold)
	{
		this.threshold = threshold;
	}

	/**
	 * Gets how many shares are produced
	 *
	 * @return the number of shares
	 */
	public int getTotalShares()
	{
		return totalShares;
	}

	/**
	 * Sets how many shares are produced
	 *
	 * @param totalShares
	 *            the number of shares
	 */
	public void setTotalShares(int totalShares)
	{
		this.totalShares = totalShares;
	}

	/**
	 * Gets the share lines, one share per line
	 *
	 * @return the share lines
	 */
	public String getShares()
	{
		return shares;
	}

	/**
	 * Sets the share lines, one share per line
	 *
	 * @param shares
	 *            the share lines
	 */
	public void setShares(String shares)
	{
		this.shares = shares;
	}

	/**
	 * Gets the secret as it was rebuilt from the shares
	 *
	 * @return the rebuilt secret, empty if nothing was rebuilt
	 */
	public String getRebuiltSecret()
	{
		return rebuiltSecret;
	}

	/**
	 * Sets the secret as it was rebuilt from the shares
	 *
	 * @param rebuiltSecret
	 *            the rebuilt secret
	 */
	public void setRebuiltSecret(String rebuiltSecret)
	{
		this.rebuiltSecret = rebuiltSecret;
	}

	/**
	 * Gets the path of the file the rebuilt secret is written to
	 *
	 * @return the path, empty if none was chosen
	 */
	public String getRebuiltFile()
	{
		return rebuiltFile;
	}

	/**
	 * Sets the path of the file the rebuilt secret is written to
	 *
	 * @param rebuiltFile
	 *            the path
	 */
	public void setRebuiltFile(String rebuiltFile)
	{
		this.rebuiltFile = rebuiltFile;
	}

	/**
	 * Gets the message shown at the bottom of the panel
	 *
	 * @return the message
	 */
	public String getResultText()
	{
		return resultText;
	}

	/**
	 * Sets the message shown at the bottom of the panel
	 *
	 * @param resultText
	 *            the message
	 */
	public void setResultText(String resultText)
	{
		this.resultText = resultText;
	}
}
