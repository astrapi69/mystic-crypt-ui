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

import javax.swing.SwingUtilities;

import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

/**
 * Reusable steps for driving the "Create your master key" dialog that the sign-in dialog's "New..."
 * flow opens. Same interaction conventions as {@link SignInDialogSteps}
 */
final class CreateMasterKeySteps
{

	private final Robot robot;
	private final DialogFixture dialog;

	CreateMasterKeySteps(Robot robot, DialogFixture dialog)
	{
		this.robot = robot;
		this.dialog = dialog;
	}

	DialogFixture dialog()
	{
		return dialog;
	}

	CreateMasterKeySteps requireApplicationFile(String expectedAbsolutePath)
	{
		dialog.textBox("txtApplicationFile").requireText(expectedAbsolutePath);
		return this;
	}

	CreateMasterKeySteps requireOkDisabled()
	{
		dialog.button("btnOk").requireDisabled();
		UiTestSpeed.step();
		return this;
	}

	CreateMasterKeySteps requireOkEnabled()
	{
		dialog.button("btnOk").requireEnabled();
		UiTestSpeed.step();
		return this;
	}

	CreateMasterKeySteps checkMasterPassword()
	{
		GuiActionRunner.execute(() -> dialog.checkBox("cbxMasterPw").target().doClick());
		dialog.checkBox("cbxMasterPw").requireSelected();
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	CreateMasterKeySteps typeMasterPasswordWithRepeat(String masterPassword)
	{
		GuiActionRunner.execute(() -> {
			dialog.textBox("txtMasterPw").target().setText(masterPassword);
			dialog.textBox("txtRepeatPw").target().setText(masterPassword);
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Ensures the "Key File:" checkbox is selected */
	CreateMasterKeySteps checkKeyFile()
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

	/**
	 * Creates a new private key file through the real user flow: "Create key file..." opens the
	 * "Create new private key" dialog, "Generate key" produces an RSA key (waits until the Save
	 * button enables), the file name is entered and "Save private key" writes the PEM file and
	 * closes the dialog
	 *
	 * @param fileName
	 *            the file name for the new private key (created in the configuration directory)
	 */
	CreateMasterKeySteps createKeyFile(String fileName)
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnCreateKeyFile").target().doClick());

		org.assertj.swing.fixture.DialogFixture keyDialog = org.assertj.swing.finder.WindowFinder
			.findDialog(new org.assertj.swing.core.GenericTypeMatcher<java.awt.Dialog>(
				java.awt.Dialog.class)
			{
				@Override
				protected boolean isMatching(java.awt.Dialog candidate)
				{
					return "Create new private key".equals(candidate.getTitle())
						&& candidate.isShowing();
				}
			}).withTimeout(10, java.util.concurrent.TimeUnit.SECONDS).using(robot);
		UiTestSpeed.step();

		// the Save button's state machine requires file name AND generated key (plus directory
		// and key size, which are pre-filled) - set the name first, then generate. Generating the
		// RSA key runs synchronously in the button's listener on the EDT, so fire it
		// asynchronously and wait for the Save button to enable
		GuiActionRunner
			.execute(() -> keyDialog.textBox("txtFilenameOfPrivateKey").target().setText(fileName));
		robot.waitForIdle();
		UiTestSpeed.step();

		SwingUtilities.invokeLater(() -> keyDialog.button("btnGenerate").target().doClick());
		Pause.pause(new Condition("private key is generated (Save button enabled)")
		{
			@Override
			public boolean test()
			{
				return keyDialog.button("btnSave").target().isEnabled();
			}
		}, 30000);
		UiTestSpeed.step();

		SwingUtilities.invokeLater(() -> keyDialog.button("btnSave").target().doClick());
		Pause.pause(new Condition("create-new-private-key dialog is closed")
		{
			@Override
			public boolean test()
			{
				return !keyDialog.target().isShowing();
			}
		}, 15000);
		UiTestSpeed.step();
		return this;
	}

	/** Clicks Cancel and waits for this dialog to close - the flow-ending button of an abort */
	void cancelAndAwaitClose()
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnCancel").target().doClick());
		Pause.pause(new Condition("create-master-key dialog is closed after cancel")
		{
			@Override
			public boolean test()
			{
				return !dialog.target().isShowing();
			}
		}, 10000);
		UiTestSpeed.step();
	}

	/** Clicks OK (creates and encrypts the database file) and waits for this dialog to close */
	void okAndAwaitClose()
	{
		SwingUtilities.invokeLater(() -> dialog.button("btnOk").target().doClick());
		Pause.pause(new Condition("create-master-key dialog is closed")
		{
			@Override
			public boolean test()
			{
				return !dialog.target().isShowing();
			}
		}, 15000);
		UiTestSpeed.step();
	}
}
