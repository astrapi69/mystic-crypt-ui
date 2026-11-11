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
package io.github.astrapi69.mystic.crypt.panel.signin.button.state.ok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JButton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * When the OK button of the sign-in dialogs may be pressed, and when the dialog that creates a
 * master key lets a repeated password through (#879)
 */
class BtnOkStateMachineTest
{

	@TempDir
	Path tempDir;

	@Test
	@DisplayName("pinned: a repeat that differs from the password leaves OK enabled (#879)")
	void aRepeatThatDiffers_leavesOkEnabled() throws IOException
	{
		JButton ok = new JButton();
		BtnOkStateMachine stateMachine = stateMachineFor(ok, TestPasswords.throwawayChars(),
			(TestPasswords.throwaway() + "-typo").toCharArray());

		stateMachine.onChangeMasterPasswordLength(stateMachine);

		assertEquals(BtnOkComponentStateEnum.ENABLED, stateMachine.getCurrentState(),
			"pinned: nothing compares the repeat with the password");
		assertTrue(ok.isEnabled());
	}

	/**
	 * A state machine for a new database with a master password, its file already on disk
	 */
	private BtnOkStateMachine stateMachineFor(final JButton ok, final char[] password,
		final char[] repeat) throws IOException
	{
		File applicationFile = Files.createFile(tempDir.resolve("new.mcrdb")).toFile();
		MasterPwFileModelBean model = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(applicationFile)).withMasterPw(true)
			.withKeyFile(false).minPasswordLength(6).masterPw(password).repeatPw(repeat).build();
		return BtnOkStateMachine.builder().component(ok).modelObject(model)
			.currentState(BtnOkComponentStateEnum.DISABLED).build();
	}
}
