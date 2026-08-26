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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

/**
 * Everything the {@link ChecksumAndMacPanel} holds: the digest and the inputs of its checksum tab,
 * the code, the key and the inputs of its message authentication code tab, the two computed values
 * and the message shown below the tabs.
 * <p>
 * Every component of the panel is bound to one of these fields, so the state of the window can be
 * read here at any moment instead of out of the widgets when a button is pressed. The text fields
 * start empty rather than {@code null}: a text component without text holds empty text, and the
 * binding would otherwise have to guess which of the two an untouched field means.
 */
public class ChecksumAndMacPanelModel
{

	/** The digest the checksum tab computes with */
	private String digest;

	/** The text the checksum is computed over, when the file is not used */
	private String checksumText = "";

	/** The path of the file the checksum is computed over */
	private String checksumFile = "";

	/** Whether the checksum is computed over the file instead of the text */
	private boolean checksumOverFile;

	/** The computed checksum, as hex */
	private String checksum = "";

	/** The checksum to compare the computed one with, as it was pasted in */
	private String expectedChecksum = "";

	/** The message authentication code the second tab computes */
	private String macAlgorithm;

	/** The key the message authentication code is computed with */
	private char[] macKey = new char[0];

	/** The text the code is computed over, when the file is not used */
	private String macText = "";

	/** The path of the file the code is computed over */
	private String macFile = "";

	/** Whether the code is computed over the file instead of the text */
	private boolean macOverFile;

	/** The computed message authentication code, as hex */
	private String mac = "";

	/** The code to compare the computed one with, as it was pasted in */
	private String expectedMac = "";

	/** What the panel last said about what it did; a space keeps the label its height */
	private String resultMessage = " ";

	/**
	 * Gets the digest the checksum tab computes with
	 *
	 * @return the digest
	 */
	public String getDigest()
	{
		return digest;
	}

	/**
	 * Sets the digest the checksum tab computes with
	 *
	 * @param digest
	 *            the digest, one of {@link ChecksumSupport#DIGESTS}
	 */
	public void setDigest(final String digest)
	{
		this.digest = digest;
	}

	/**
	 * Gets the text the checksum is computed over
	 *
	 * @return the text
	 */
	public String getChecksumText()
	{
		return checksumText;
	}

	/**
	 * Sets the text the checksum is computed over
	 *
	 * @param checksumText
	 *            the text
	 */
	public void setChecksumText(final String checksumText)
	{
		this.checksumText = checksumText;
	}

	/**
	 * Gets the path of the file the checksum is computed over
	 *
	 * @return the path
	 */
	public String getChecksumFile()
	{
		return checksumFile;
	}

	/**
	 * Sets the path of the file the checksum is computed over
	 *
	 * @param checksumFile
	 *            the path
	 */
	public void setChecksumFile(final String checksumFile)
	{
		this.checksumFile = checksumFile;
	}

	/**
	 * Whether the checksum is computed over the file instead of the text
	 *
	 * @return true if the file is used
	 */
	public boolean isChecksumOverFile()
	{
		return checksumOverFile;
	}

	/**
	 * Sets whether the checksum is computed over the file instead of the text
	 *
	 * @param checksumOverFile
	 *            true if the file is used
	 */
	public void setChecksumOverFile(final boolean checksumOverFile)
	{
		this.checksumOverFile = checksumOverFile;
	}

	/**
	 * Gets the computed checksum
	 *
	 * @return the checksum as hex
	 */
	public String getChecksum()
	{
		return checksum;
	}

	/**
	 * Sets the computed checksum
	 *
	 * @param checksum
	 *            the checksum as hex
	 */
	public void setChecksum(final String checksum)
	{
		this.checksum = checksum;
	}

	/**
	 * Gets the checksum the computed one is compared with
	 *
	 * @return the expected checksum as hex
	 */
	public String getExpectedChecksum()
	{
		return expectedChecksum;
	}

	/**
	 * Sets the checksum the computed one is compared with
	 *
	 * @param expectedChecksum
	 *            the expected checksum as hex
	 */
	public void setExpectedChecksum(final String expectedChecksum)
	{
		this.expectedChecksum = expectedChecksum;
	}

	/**
	 * Gets the message authentication code the second tab computes
	 *
	 * @return the algorithm
	 */
	public String getMacAlgorithm()
	{
		return macAlgorithm;
	}

	/**
	 * Sets the message authentication code the second tab computes
	 *
	 * @param macAlgorithm
	 *            the algorithm, one of {@link ChecksumSupport#MACS}
	 */
	public void setMacAlgorithm(final String macAlgorithm)
	{
		this.macAlgorithm = macAlgorithm;
	}

	/**
	 * Gets the key the message authentication code is computed with
	 *
	 * @return the key
	 */
	public char[] getMacKey()
	{
		return macKey;
	}

	/**
	 * Sets the key the message authentication code is computed with
	 *
	 * @param macKey
	 *            the key
	 */
	public void setMacKey(final char[] macKey)
	{
		this.macKey = macKey;
	}

	/**
	 * Gets the text the code is computed over
	 *
	 * @return the text
	 */
	public String getMacText()
	{
		return macText;
	}

	/**
	 * Sets the text the code is computed over
	 *
	 * @param macText
	 *            the text
	 */
	public void setMacText(final String macText)
	{
		this.macText = macText;
	}

	/**
	 * Gets the path of the file the code is computed over
	 *
	 * @return the path
	 */
	public String getMacFile()
	{
		return macFile;
	}

	/**
	 * Sets the path of the file the code is computed over
	 *
	 * @param macFile
	 *            the path
	 */
	public void setMacFile(final String macFile)
	{
		this.macFile = macFile;
	}

	/**
	 * Whether the code is computed over the file instead of the text
	 *
	 * @return true if the file is used
	 */
	public boolean isMacOverFile()
	{
		return macOverFile;
	}

	/**
	 * Sets whether the code is computed over the file instead of the text
	 *
	 * @param macOverFile
	 *            true if the file is used
	 */
	public void setMacOverFile(final boolean macOverFile)
	{
		this.macOverFile = macOverFile;
	}

	/**
	 * Gets the computed message authentication code
	 *
	 * @return the code as hex
	 */
	public String getMac()
	{
		return mac;
	}

	/**
	 * Sets the computed message authentication code
	 *
	 * @param mac
	 *            the code as hex
	 */
	public void setMac(final String mac)
	{
		this.mac = mac;
	}

	/**
	 * Gets the code the computed one is compared with
	 *
	 * @return the expected code as hex
	 */
	public String getExpectedMac()
	{
		return expectedMac;
	}

	/**
	 * Sets the code the computed one is compared with
	 *
	 * @param expectedMac
	 *            the expected code as hex
	 */
	public void setExpectedMac(final String expectedMac)
	{
		this.expectedMac = expectedMac;
	}

	/**
	 * Gets what the panel last said about what it did
	 *
	 * @return the message
	 */
	public String getResultMessage()
	{
		return resultMessage;
	}

	/**
	 * Sets what the panel says about what it did
	 *
	 * @param resultMessage
	 *            the message
	 */
	public void setResultMessage(final String resultMessage)
	{
		this.resultMessage = resultMessage;
	}
}
