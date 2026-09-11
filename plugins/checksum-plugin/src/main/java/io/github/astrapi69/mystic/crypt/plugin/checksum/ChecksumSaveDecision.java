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
 * Whether a computed checksum can be written to a checksum file, and what to say when it cannot
 * (#320).
 * <p>
 * A checksum file has two columns, the checksum and the name of the file it belongs to, so a
 * checksum of typed text has nothing to write in the second one. That is a fact about checksum
 * files rather than about this panel, and it is decided here so the button's enabled state and the
 * handler's refusal cannot drift apart - they were two copies of the same rule, and the copy in the
 * handler ran only after somebody had pressed a button that was never going to work.
 * <p>
 * No Swing type appears here: this is a question about the model, and it is answered without a
 * display.
 */
public final class ChecksumSaveDecision
{

	private ChecksumSaveDecision()
	{
	}

	/**
	 * Why a checksum file cannot be written yet, or null when it can be.
	 *
	 * @param checksumOverFile
	 *            whether the checksum was taken over a file rather than over typed text
	 * @param checksum
	 *            the computed checksum, if there is one
	 * @return the reason, ready to be shown, or null when there is nothing in the way
	 */
	public static String refusalFor(final boolean checksumOverFile, final String checksum)
	{
		if (!checksumOverFile)
		{
			return ChecksumMessages.getString("checksum.and.mac.save.needs.a.file",
				"a checksum file names the file it belongs to, so tick 'use the file instead of "
					+ "the text' and compute again");
		}
		if (checksum == null || checksum.isBlank())
		{
			return ChecksumMessages.getString("checksum.and.mac.save.needs.a.checksum",
				"there is nothing to save yet - compute the checksum first");
		}
		return null;
	}

	/**
	 * Whether saving is refused for a reason the user can only fix by changing what the panel is
	 * computing over - which is the reason the button is switched off rather than left to refuse.
	 * <p>
	 * "Nothing computed yet" is deliberately NOT in here: it is fixed by pressing Compute, the
	 * button right next to it, and an enabled button that says so is the better hint. The reserved
	 * question in #320, decided in the issue.
	 *
	 * @param checksumOverFile
	 *            whether the checksum was taken over a file rather than over typed text
	 * @return true when the panel is in a state where saving cannot work at all
	 */
	public static boolean savingIsImpossible(final boolean checksumOverFile)
	{
		return !checksumOverFile;
	}
}
