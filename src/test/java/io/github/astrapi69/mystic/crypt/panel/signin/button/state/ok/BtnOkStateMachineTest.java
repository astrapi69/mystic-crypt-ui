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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.swing.JButton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
	@DisplayName("in Create your master key a repeat that differs from the password keeps OK disabled (#879)")
	void aRepeatThatDiffers_keepsOkDisabled() throws IOException
	{
		JButton ok = new JButton();
		BtnOkStateMachine stateMachine = stateMachineFor(ok, true, true,
			TestPasswords.throwawayChars(), (TestPasswords.throwaway() + "-typo").toCharArray());

		stateMachine.onChangeMasterPasswordLength(stateMachine);

		assertEquals(BtnOkComponentStateEnum.DISABLED, stateMachine.getCurrentState(),
			"a typo in the repeat has to stop the database from being written with it");
		assertFalse(ok.isEnabled());
	}

	@ParameterizedTest(name = "{argumentSetName}")
	@MethodSource("repeats")
	@DisplayName("in Create your master key OK is enabled exactly when the repeat is the password")
	void okFollowsTheRepeat(final UnaryOperator<String> repeatOf,
		final BtnOkComponentStateEnum expected) throws IOException
	{
		String password = TestPasswords.throwaway();
		String repeat = repeatOf.apply(password);
		BtnOkStateMachine stateMachine = stateMachineFor(new JButton(), true, true,
			password.toCharArray(), repeat == null ? null : repeat.toCharArray());

		stateMachine.onChangeMasterPasswordLength(stateMachine);

		assertEquals(expected, stateMachine.getCurrentState());
	}

	static Stream<Arguments> repeats()
	{
		return Stream.of(
			Arguments.argumentSet("the same characters",
				(UnaryOperator<String>)password -> new String(password.toCharArray()),
				BtnOkComponentStateEnum.ENABLED),
			Arguments.argumentSet("one character different",
				(UnaryOperator<String>)password -> password.substring(0, password.length() - 1)
					+ (password.endsWith("x") ? "y" : "x"),
				BtnOkComponentStateEnum.DISABLED),
			Arguments.argumentSet("the password and one character more",
				(UnaryOperator<String>)password -> password + "x",
				BtnOkComponentStateEnum.DISABLED),
			Arguments.argumentSet("the password without its last character",
				(UnaryOperator<String>)password -> password.substring(0, password.length() - 1),
				BtnOkComponentStateEnum.DISABLED),
			Arguments.argumentSet("an empty repeat", (UnaryOperator<String>)password -> "",
				BtnOkComponentStateEnum.DISABLED),
			Arguments.argumentSet("no repeat typed at all", (UnaryOperator<String>)password -> null,
				BtnOkComponentStateEnum.DISABLED));
	}

	@Test
	@DisplayName("the sign-in dialog has no repeat, and a repeat left in the model does not count there")
	void theSignInDialog_ignoresTheRepeat() throws IOException
	{
		BtnOkStateMachine stateMachine = stateMachineFor(new JButton(), false, true,
			TestPasswords.throwawayChars(), (TestPasswords.throwaway() + "-other").toCharArray());

		stateMachine.onChangeMasterPasswordLength(stateMachine);

		assertEquals(BtnOkComponentStateEnum.ENABLED, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("a database with a key file only has no password to repeat, so the repeat does not count")
	void aKeyFileOnly_ignoresTheRepeat() throws IOException
	{
		BtnOkStateMachine stateMachine = stateMachineFor(new JButton(), true, false,
			TestPasswords.throwawayChars(), (TestPasswords.throwaway() + "-other").toCharArray());
		stateMachine.getModelObject().setWithKeyFile(true);
		stateMachine.getModelObject().setKeyFileInfo(
			FileInfo.toFileInfo(Files.createFile(tempDir.resolve("key.pem")).toFile()));

		stateMachine.onSetKeyFile(stateMachine);

		assertEquals(BtnOkComponentStateEnum.ENABLED, stateMachine.getCurrentState());
	}

	/**
	 * A state machine for a new database, its file already on disk
	 *
	 * @param repeatRequired
	 *            true for "Create your master key", false for the sign-in dialog
	 * @param withMasterPassword
	 *            whether the database is protected with a master password
	 */
	private BtnOkStateMachine stateMachineFor(final JButton ok, final boolean repeatRequired,
		final boolean withMasterPassword, final char[] password, final char[] repeat)
		throws IOException
	{
		File applicationFile = Files.createFile(tempDir.resolve("new.mcrdb")).toFile();
		MasterPwFileModelBean model = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(applicationFile))
			.withMasterPw(withMasterPassword).withKeyFile(false).minPasswordLength(6)
			.masterPw(password).repeatPw(repeat).build();
		return BtnOkStateMachine.builder().component(ok).modelObject(model)
			.repeatRequired(repeatRequired).currentState(BtnOkComponentStateEnum.DISABLED).build();
	}
}
