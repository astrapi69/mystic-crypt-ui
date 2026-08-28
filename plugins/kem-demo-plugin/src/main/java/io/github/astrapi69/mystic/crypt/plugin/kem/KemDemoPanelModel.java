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
package io.github.astrapi69.mystic.crypt.plugin.kem;

/**
 * The state of {@link KemDemoPanel}: the mechanism the exchange runs with and the three values the
 * last run produced, the ciphertext and the two shared secrets, plus the line that reports whether
 * the two secrets came out equal.
 * <p>
 * Every component of the panel is bound to a property here, so what the button works with is what
 * the model holds and not what a widget happens to contain, and the outcome of a run is readable
 * without asking a text area for its content.
 */
public class KemDemoPanelModel
{

	/** The mechanism the exchange runs with; the tool starts with the configured one */
	private String algorithm = KemSettingsContribution.algorithm();

	/** The ciphertext the sender transmits, as hex, empty before the first run */
	private String ciphertext = "";

	/** The shared secret the sender derived, as hex, empty before the first run */
	private String senderSecret = "";

	/** The shared secret the recipient derived, as hex, empty before the first run */
	private String recipientSecret = "";

	/** What the last run left to report, a blank line before the first one */
	private String resultText = " ";

	/**
	 * Gets the mechanism the exchange runs with
	 *
	 * @return the mechanism the exchange runs with
	 */
	public String getAlgorithm()
	{
		return algorithm;
	}

	/**
	 * Sets the mechanism the exchange runs with
	 *
	 * @param algorithm
	 *            the mechanism the exchange runs with
	 */
	public void setAlgorithm(final String algorithm)
	{
		this.algorithm = algorithm;
	}

	/**
	 * Gets the ciphertext of the last run
	 *
	 * @return the ciphertext of the last run, as hex
	 */
	public String getCiphertext()
	{
		return ciphertext;
	}

	/**
	 * Sets the ciphertext of the last run
	 *
	 * @param ciphertext
	 *            the ciphertext of the last run, as hex
	 */
	public void setCiphertext(final String ciphertext)
	{
		this.ciphertext = ciphertext;
	}

	/**
	 * Gets the shared secret the sender derived
	 *
	 * @return the shared secret the sender derived, as hex
	 */
	public String getSenderSecret()
	{
		return senderSecret;
	}

	/**
	 * Sets the shared secret the sender derived
	 *
	 * @param senderSecret
	 *            the shared secret the sender derived, as hex
	 */
	public void setSenderSecret(final String senderSecret)
	{
		this.senderSecret = senderSecret;
	}

	/**
	 * Gets the shared secret the recipient derived
	 *
	 * @return the shared secret the recipient derived, as hex
	 */
	public String getRecipientSecret()
	{
		return recipientSecret;
	}

	/**
	 * Sets the shared secret the recipient derived
	 *
	 * @param recipientSecret
	 *            the shared secret the recipient derived, as hex
	 */
	public void setRecipientSecret(final String recipientSecret)
	{
		this.recipientSecret = recipientSecret;
	}

	/**
	 * Gets what the last run left to report
	 *
	 * @return what the last run left to report
	 */
	public String getResultText()
	{
		return resultText;
	}

	/**
	 * Sets what the last run left to report
	 *
	 * @param resultText
	 *            what the last run left to report
	 */
	public void setResultText(final String resultText)
	{
		this.resultText = resultText;
	}
}
