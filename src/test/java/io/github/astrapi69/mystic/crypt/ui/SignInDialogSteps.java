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
package io.github.astrapi69.mystic.crypt.ui;

import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

/**
 * Reusable steps for driving the "Enter your credentials" sign-in dialog. Each step is one thing a
 * real user does there; tests compose them into complete flows.
 * <p>
 * Clicks whose listener opens a MODAL dialog run via {@code SwingUtilities.invokeLater} - blocking
 * on them (e.g. through {@code GuiActionRunner.execute}) would deadlock the test thread against a
 * dialog it is itself responsible for driving. Text is set through the components' own EDT API
 * (fires the same listeners as typing) because pixel-coordinate robot input is unreliable on this
 * shared, live desktop display
 */
final class SignInDialogSteps
{

	private final Robot robot;
	private final DialogFixture dialog;

	SignInDialogSteps(Robot robot, DialogFixture dialog)
	{
		this.robot = robot;
		this.dialog = dialog;
	}

	DialogFixture dialog()
	{
		return dialog;
	}

	SignInDialogSteps requireOkDisabled()
	{
		dialog.button("btnOk").requireDisabled();
		UiTestSpeed.step();
		return this;
	}

	SignInDialogSteps requireOkEnabled()
	{
		dialog.button("btnOk").requireEnabled();
		UiTestSpeed.step();
		return this;
	}

	SignInDialogSteps checkMasterPassword()
	{
		// ensure-semantics, not toggle: when a memoized sign-in pre-selects the checkbox
		// (returning-user launch), a blind doClick would deselect it again
		GuiActionRunner.execute(() -> {
			javax.swing.JCheckBox checkBox = (javax.swing.JCheckBox)dialog.checkBox("cbxMasterPw")
				.target();
			if (!checkBox.isSelected())
			{
				checkBox.doClick();
			}
		});
		dialog.checkBox("cbxMasterPw").requireSelected();
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	SignInDialogSteps typeMasterPassword(String masterPassword)
	{
		GuiActionRunner
			.execute(() -> dialog.textBox("txtMasterPw").target().setText(masterPassword));
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Ensures the "Key File:" checkbox is selected */
	SignInDialogSteps checkKeyFile()
	{
		GuiActionRunner.execute(() -> {
			javax.swing.JCheckBox checkBox = (javax.swing.JCheckBox)dialog.checkBox("cbxKeyFile")
				.target();
			if (!checkBox.isSelected())
			{
				checkBox.doClick();
			}
		});
		dialog.checkBox("cbxKeyFile").requireSelected();
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Clicks the key file's "Browse..." and selects the given file in the chooser that opens */
	SignInDialogSteps browseKeyFile(File keyFile)
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnKeyFileChooser").target().doClick());
		approveInFileChooser(keyFile);
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Clicks "Browse..." and selects the given file in the save chooser that opens */
	SignInDialogSteps browseApplicationFile(File applicationFile)
	{
		SwingUtilities
			.invokeLater(() -> dialog.button("btnApplicationFileChooser").target().doClick());
		approveInFileChooser(applicationFile);
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Clicks "New..." and selects the new database file in the save chooser that opens; the "Create
	 * your master key" dialog that follows is returned as its own steps object
	 */
	CreateMasterKeySteps startNewDatabase(File newDatabaseFile)
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnNewApplicationFile").target().doClick());
		approveInFileChooser(newDatabaseFile);

		DialogFixture createMasterKeyDialog = WindowFinder
			.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
			{
				@Override
				protected boolean isMatching(Dialog candidate)
				{
					return "Create your master key".equals(candidate.getTitle())
						&& candidate.isShowing();
				}
			}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		UiTestSpeed.step();
		return new CreateMasterKeySteps(robot, createMasterKeyDialog);
	}

	/**
	 * Clicks OK without waiting for anything - for flows where the click is expected to produce an
	 * error dialog instead of a successful sign-in
	 */
	void clickOk()
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnOk").target().doClick());
		UiTestSpeed.step();
	}

	/** Clicks OK and waits for the sign-in dialog to close (successful sign-in) */
	void okAndAwaitSignIn()
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnOk").target().doClick());
		Pause.pause(new Condition("sign-in dialog is closed after successful sign-in")
		{
			@Override
			public boolean test()
			{
				return !dialog.target().isShowing();
			}
		}, 15000);
	}

	/**
	 * Presses Enter in the master-password field and waits for the sign-in dialog to close.
	 * Exercises the field's action listener, which clicks OK when it is enabled (the "Enter
	 * submits" behavior).
	 */
	void enterInMasterPasswordAndAwaitSignIn()
	{
		dialog.textBox("txtMasterPw").focus().pressAndReleaseKeys(KeyEvent.VK_ENTER);
		Pause.pause(new Condition("sign-in dialog is closed after Enter-submit sign-in")
		{
			@Override
			public boolean test()
			{
				return !dialog.target().isShowing();
			}
		}, 15000);
	}

	/** Clicks Cancel - the flow-ending button of an aborted sign-in */
	void cancel()
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnCancel").target().doClick());
	}

	private void approveInFileChooser(File file)
	{
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot);
		UiTestSpeed.step();
		JFileChooser fileChooserTarget = fileChooser.target();
		SwingUtilities.invokeLater(() -> {
			fileChooserTarget.setSelectedFile(file);
			fileChooserTarget.approveSelection();
		});
	}
}
