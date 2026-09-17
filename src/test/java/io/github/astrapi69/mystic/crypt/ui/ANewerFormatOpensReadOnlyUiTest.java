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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.SaveAsApplicationFileAction;
import io.github.astrapi69.mystic.crypt.app.file.xml.PasswordVaultFormat;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;

/**
 * A vault written in a format newer than this build knows opens read-only (#402, the maintainer's
 * Q1): its content is shown, opening it says which format version it needs, and neither Save nor
 * Save As can write it - because reading it skipped what this build does not know, and a write
 * would drop that from the file without anybody noticing.
 * <p>
 * The newer vault is made the way a newer version would leave it: a vault this build wrote, with
 * the version attribute raised by one and an element added that no field here reads.
 */
class ANewerFormatOpensReadOnlyUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final int NEWER = VaultXmlCodec.FORMAT_VERSION + 1;

	private static final String READ_ONLY_TITLE = "Opened read-only";

	private static final String ENTRY_TITLE = "written before the newer version saved it";

	@Test
	@DisplayName("a newer vault opens readable, names the version it needs, and both ways of saving are disabled")
	void aNewerVault_opensReadable_withSaveAndSaveAsDisabled() throws Exception
	{
		File vault = aVaultANewerVersionWrote();
		byte[] asTheNewerVersionLeftIt = Files.readAllBytes(vault.toPath());

		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		String message = messageOfTheDialogTitled(READ_ONLY_TITLE);
		application.dismissMessageDialog(READ_ONLY_TITLE);

		assertTrue(message.contains("format version " + NEWER),
			"the opening names the format the vault needs: " + message);
		assertTrue(application.entryExistsWithTitle(ENTRY_TITLE),
			"read-only is not closed: the content is there to read");
		assertSavingIsDisabled("right after opening");

		GuiActionRunner.execute(() -> ApplicationEventBus.getSaveState()
			.fireEvent(new EventObject<>(RenderMode.EDITABLE)));
		robot.waitForIdle();
		assertSavingIsDisabled(
			"after an edit, which is what enables Save on a vault this build can write");

		File target = GuiActionRunner.execute(() -> new File(
			MysticCryptApplicationFrame.getInstance().getModelObject().getMasterPwFileModelBean()
				.getApplicationFileInfo().getPath(),
			MysticCryptApplicationFrame.getInstance().getModelObject().getMasterPwFileModelBean()
				.getApplicationFileInfo().getName()));
		SwingUtilities.invokeLater(() -> new SaveAsApplicationFileAction("Save As")
			.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "save as")));
		String refusal = messageOfTheDialogTitled(READ_ONLY_TITLE);
		application.dismissMessageDialog(READ_ONLY_TITLE);
		assertTrue(refusal.contains("format version " + NEWER),
			"Save As fired past its disabled menu item - a rebuilt menu bar can re-enable it - says "
				+ "why instead of opening a file chooser: " + refusal);
		assertEquals(target,
			GuiActionRunner.execute(() -> new File(
				MysticCryptApplicationFrame.getInstance().getModelObject()
					.getMasterPwFileModelBean().getApplicationFileInfo().getPath(),
				MysticCryptApplicationFrame.getInstance().getModelObject()
					.getMasterPwFileModelBean().getApplicationFileInfo().getName())),
			"and does not retarget the open vault before refusing");

		assertArrayEquals(asTheNewerVersionLeftIt, Files.readAllBytes(vault.toPath()),
			"and the file is as the newer version left it");
	}

	@Test
	@DisplayName("ending with changes to a read-only vault asks to discard them and never writes")
	void endingWithChanges_asksToDiscard_insteadOfOfferingASave() throws Exception
	{
		File vault = aVaultANewerVersionWrote();
		byte[] asTheNewerVersionLeftIt = Files.readAllBytes(vault.toPath());
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		application.dismissMessageDialog(READ_ONLY_TITLE);
		GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().getModelObject().setDirty(true));

		AtomicBoolean ended = new AtomicBoolean(false);
		SwingUtilities.invokeLater(
			() -> ended.set(MysticCryptApplicationFrame.getInstance().endTheApplication(() -> {
			})));
		JDialog question = awaitAQuestionMentioning("read-only");
		GuiActionRunner
			.execute(() -> ((JOptionPane)robot.finder().findByType(question, JOptionPane.class))
				.setValue(JOptionPane.NO_OPTION));
		robot.waitForIdle();

		assertFalse(ended.get(), "No keeps the application and the changes, as for a locked vault");
		assertArrayEquals(asTheNewerVersionLeftIt, Files.readAllBytes(vault.toPath()),
			"the question offered no save, so nothing could be written");
	}

	private File aVaultANewerVersionWrote() throws Exception
	{
		File vault = new File(tempHome, "newer-format.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		application.saveDatabase();
		shutdownApplication();

		char[] password = MASTER_PASSWORD.toCharArray();
		String xml = new String(PasswordVaultFormat.decrypt(vault, password));
		String written = xml
			.replaceFirst(" formatVersion=\"" + VaultXmlCodec.FORMAT_VERSION + "\"",
				" formatVersion=\"" + NEWER + "\"")
			.replaceFirst("<dirty>",
				"<somethingOnlyANewerVersionKnows>kept by that version</somethingOnlyANewerVersionKnows><dirty>");
		assertTrue(
			written.contains(" formatVersion=\"" + NEWER + "\"")
				&& written.contains("<somethingOnlyANewerVersionKnows>"),
			"the precondition: the vault now looks like a newer version wrote it");
		Files.write(vault.toPath(), PasswordVaultFormat.encrypt(written.toCharArray(), password));
		return vault;
	}

	private void assertSavingIsDisabled(final String when)
	{
		assertFalse(isEnabled(MenuId.SAVE_APPLICATION_FILE.propertiesKey()),
			"Save is disabled " + when);
		assertFalse(isEnabled(MenuId.SAVE_AS_APPLICATION_FILE.propertiesKey()),
			"Save As is disabled " + when);
		assertFalse(isEnabled(MenuId.SAVE_APPLICATION_FILE_TOOL_BAR.propertiesKey()),
			"the toolbar's Save is disabled " + when);
	}

	private static boolean isEnabled(final String name)
	{
		return GuiActionRunner.execute(() -> {
			MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
			Component found = findNamed(frame.getJMenuBar(), name);
			if (found == null)
			{
				found = findNamed(frame.getToolBar(), name);
			}
			if (found == null)
			{
				throw new IllegalStateException("no component named " + name);
			}
			return found.isEnabled();
		});
	}

	private static Component findNamed(final Component root, final String name)
	{
		if (root == null)
		{
			return null;
		}
		if (name.equals(root.getName()))
		{
			return root;
		}
		if (root instanceof JMenuBar || root instanceof MenuElement
			|| root instanceof java.awt.Container)
		{
			Component[] children = root instanceof javax.swing.JMenu menu
				? menu.getMenuComponents()
				: ((java.awt.Container)root).getComponents();
			for (Component child : children)
			{
				Component found = findNamed(child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private String messageOfTheDialogTitled(final String title)
	{
		JDialog dialog = awaitDialog(candidate -> title.equals(candidate.getTitle()), title);
		return GuiActionRunner.execute(() -> String.valueOf(
			((JOptionPane)robot.finder().findByType(dialog, JOptionPane.class)).getMessage()));
	}

	private JDialog awaitAQuestionMentioning(final String text)
	{
		return awaitDialog(candidate -> {
			JOptionPane pane = robot.finder().findByType(candidate, JOptionPane.class);
			return String.valueOf(textOf(pane.getMessage())).contains(text);
		}, "a question mentioning '" + text + "'");
	}

	private static String textOf(final Object message)
	{
		if (message instanceof java.awt.Container container)
		{
			StringBuilder text = new StringBuilder();
			for (Component child : container.getComponents())
			{
				if (child instanceof javax.swing.JLabel label)
				{
					text.append(label.getText());
				}
				text.append(textOf(child));
			}
			return text.toString();
		}
		return String.valueOf(message);
	}

	private JDialog awaitDialog(final java.util.function.Predicate<JDialog> matching,
		final String description)
	{
		JDialog[] found = new JDialog[1];
		Pause.pause(new Condition(description + " is on screen")
		{
			@Override
			public boolean test()
			{
				found[0] = GuiActionRunner.execute(() -> {
					for (Window window : Window.getWindows())
					{
						if (window instanceof JDialog dialog && dialog.isShowing())
						{
							try
							{
								if (matching.test(dialog))
								{
									return dialog;
								}
							}
							catch (RuntimeException notTheOne)
							{
								// a dialog without an option pane is not the one looked for
							}
						}
					}
					return null;
				});
				return found[0] != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
		return found[0];
	}
}
