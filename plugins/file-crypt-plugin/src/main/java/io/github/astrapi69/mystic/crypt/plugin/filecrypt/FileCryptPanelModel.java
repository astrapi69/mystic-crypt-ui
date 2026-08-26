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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

/**
 * The state of {@link FileCryptPanel}: the two paths and the passphrase pair of the file tab, the
 * two texts and the passphrase pair of the text tab, and the message shown below the tabs.
 * <p>
 * Every field is kept current by the model backed component it is bound to, so the panel reads what
 * the user entered from here instead of from the widgets. A passphrase stays a {@code char[]} and
 * is never turned into a {@link String} on the way in.
 */
public class FileCryptPanelModel
{

	/** The file that is read on encrypting and on decrypting */
	private String sourceFile = "";

	/** The file the result is written to, empty when the tool may choose the name itself */
	private String targetFile = "";

	/** The passphrase of the file tab */
	private char[] filePassphrase = new char[0];

	/** The repetition of the file passphrase, asked for on encrypting */
	private char[] filePassphraseRepeated = new char[0];

	/** The text that is encrypted */
	private String plainText = "";

	/** The encrypted text in its Base64 form */
	private String encryptedText = "";

	/** The passphrase of the text tab */
	private char[] textPassphrase = new char[0];

	/** The repetition of the text passphrase, asked for on encrypting */
	private char[] textPassphraseRepeated = new char[0];

	/** The message shown below the tabs */
	private String resultText = " ";

	public String getSourceFile()
	{
		return sourceFile;
	}

	public void setSourceFile(String sourceFile)
	{
		this.sourceFile = sourceFile;
	}

	public String getTargetFile()
	{
		return targetFile;
	}

	public void setTargetFile(String targetFile)
	{
		this.targetFile = targetFile;
	}

	public char[] getFilePassphrase()
	{
		return filePassphrase;
	}

	public void setFilePassphrase(char[] filePassphrase)
	{
		this.filePassphrase = filePassphrase;
	}

	public char[] getFilePassphraseRepeated()
	{
		return filePassphraseRepeated;
	}

	public void setFilePassphraseRepeated(char[] filePassphraseRepeated)
	{
		this.filePassphraseRepeated = filePassphraseRepeated;
	}

	public String getPlainText()
	{
		return plainText;
	}

	public void setPlainText(String plainText)
	{
		this.plainText = plainText;
	}

	public String getEncryptedText()
	{
		return encryptedText;
	}

	public void setEncryptedText(String encryptedText)
	{
		this.encryptedText = encryptedText;
	}

	public char[] getTextPassphrase()
	{
		return textPassphrase;
	}

	public void setTextPassphrase(char[] textPassphrase)
	{
		this.textPassphrase = textPassphrase;
	}

	public char[] getTextPassphraseRepeated()
	{
		return textPassphraseRepeated;
	}

	public void setTextPassphraseRepeated(char[] textPassphraseRepeated)
	{
		this.textPassphraseRepeated = textPassphraseRepeated;
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
