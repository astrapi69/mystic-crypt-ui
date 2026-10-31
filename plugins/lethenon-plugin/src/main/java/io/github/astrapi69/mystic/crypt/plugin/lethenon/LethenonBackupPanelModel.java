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
 * The state of {@link LethenonBackupPanel}, in one object rather than scattered across its widgets
 * (architecture.md): the step, the asked positions and the answers, the choice to store the words,
 * the export file and its checksum, the words of a check, and why the last action was refused.
 * <p>
 * The wallet password and the shown words are {@code char[]} and are overwritten when replaced or
 * cleared. The answers and the words of a check are Strings, because text fields hold them; they
 * are emptied when the assistant ends and when the vault locks.
 */
public class LethenonBackupPanelModel
{

	private BackupStep step = BackupStep.WORDS;

	private char[] password = new char[0];

	private char[] shownWords = new char[0];

	private int[] positions = new int[0];

	private String answer1 = "";

	private String answer2 = "";

	private String answer3 = "";

	private boolean storeWords;

	private String exportFile = "";

	private String checksum = "";

	private String checkWords = "";

	private String checkResult = "";

	private String refusal = "";

	public BackupStep getStep()
	{
		return step;
	}

	public void setStep(BackupStep step)
	{
		this.step = step;
	}

	public char[] getPassword()
	{
		return password;
	}

	public void setPassword(char[] password)
	{
		this.password = SecretFields.replaced(this.password, password);
	}

	public char[] getShownWords()
	{
		return shownWords;
	}

	public void setShownWords(char[] shownWords)
	{
		this.shownWords = SecretFields.replaced(this.shownWords, shownWords);
	}

	public int[] getPositions()
	{
		return positions.clone();
	}

	public void setPositions(int[] positions)
	{
		this.positions = positions == null ? new int[0] : positions.clone();
	}

	public String getAnswer1()
	{
		return answer1;
	}

	public void setAnswer1(String answer1)
	{
		this.answer1 = answer1 == null ? "" : answer1;
	}

	public String getAnswer2()
	{
		return answer2;
	}

	public void setAnswer2(String answer2)
	{
		this.answer2 = answer2 == null ? "" : answer2;
	}

	public String getAnswer3()
	{
		return answer3;
	}

	public void setAnswer3(String answer3)
	{
		this.answer3 = answer3 == null ? "" : answer3;
	}

	/**
	 * @return the three answers in the order of the asked positions
	 */
	public String[] getAnswers()
	{
		return new String[] { answer1, answer2, answer3 };
	}

	public boolean isStoreWords()
	{
		return storeWords;
	}

	public void setStoreWords(boolean storeWords)
	{
		this.storeWords = storeWords;
	}

	public String getExportFile()
	{
		return exportFile;
	}

	public void setExportFile(String exportFile)
	{
		this.exportFile = exportFile == null ? "" : exportFile;
	}

	public String getChecksum()
	{
		return checksum;
	}

	public void setChecksum(String checksum)
	{
		this.checksum = checksum == null ? "" : checksum;
	}

	public String getCheckWords()
	{
		return checkWords;
	}

	public void setCheckWords(String checkWords)
	{
		this.checkWords = checkWords == null ? "" : checkWords;
	}

	public String getCheckResult()
	{
		return checkResult;
	}

	public void setCheckResult(String checkResult)
	{
		this.checkResult = checkResult == null ? "" : checkResult;
	}

	public String getRefusal()
	{
		return refusal;
	}

	public void setRefusal(String refusal)
	{
		this.refusal = refusal == null ? "" : refusal;
	}

	/**
	 * Overwrites the password and the words and empties everything typed, as when the assistant
	 * ends or the vault locks
	 */
	public void clear()
	{
		setPassword(new char[0]);
		setShownWords(new char[0]);
		positions = new int[0];
		answer1 = "";
		answer2 = "";
		answer3 = "";
		storeWords = false;
		exportFile = "";
		checksum = "";
		checkWords = "";
		checkResult = "";
		refusal = "";
	}
}
