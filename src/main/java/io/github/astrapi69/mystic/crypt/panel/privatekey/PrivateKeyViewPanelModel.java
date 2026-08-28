/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.privatekey;

/**
 * The state of {@link PrivateKeyViewPanel}: the private key and the public key in the PEM form the
 * panel shows them in.
 * <p>
 * The two text areas of the panel are bound to this object, so the key texts that were put into the
 * view are readable here at any moment - a test, a second panel or the command line side does not
 * have to reach into a widget for them.
 */
public class PrivateKeyViewPanelModel
{

	/** The private key in its PEM form, empty as long as no key was opened or generated */
	private String privateKeyText = "";

	/** The public key in its PEM form, empty as long as no key was opened or generated */
	private String publicKeyText = "";

	/**
	 * Gets the private key in its PEM form
	 *
	 * @return the private key in its PEM form
	 */
	public String getPrivateKeyText()
	{
		return privateKeyText;
	}

	/**
	 * Sets the private key in its PEM form
	 *
	 * @param privateKeyText
	 *            the private key in its PEM form
	 */
	public void setPrivateKeyText(final String privateKeyText)
	{
		this.privateKeyText = privateKeyText;
	}

	/**
	 * Gets the public key in its PEM form
	 *
	 * @return the public key in its PEM form
	 */
	public String getPublicKeyText()
	{
		return publicKeyText;
	}

	/**
	 * Sets the public key in its PEM form
	 *
	 * @param publicKeyText
	 *            the public key in its PEM form
	 */
	public void setPublicKeyText(final String publicKeyText)
	{
		this.publicKeyText = publicKeyText;
	}
}
