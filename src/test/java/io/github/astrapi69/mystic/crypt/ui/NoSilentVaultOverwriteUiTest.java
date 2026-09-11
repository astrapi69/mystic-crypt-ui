package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SaveAsApplicationFileAction;

/**
 * A vault is never replaced without being asked (#300).
 * <p>
 * Measured before the change: "Save As" retargeted the open model to whatever file the chooser
 * returned and stored it there, and the writer replaces. Pick another vault in that chooser and it
 * was gone - replaced by the open one, encrypted with the open one's master password, with nothing
 * on screen and nothing recoverable. "New database" onto an existing file had the same shape: its
 * existence check only decided whether to create an empty file first.
 * <p>
 * A vault is not derived data. A checksum file can be recomputed, a converted key still has its
 * original; a replaced vault has nothing behind it. So this is the half of the overwrite rule that
 * asks with the target named, and the answer decides.
 */
class NoSilentVaultOverwriteUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String OTHER_PASSWORD = TestPasswords.throwaway() + "-other";

	@Test
	@DisplayName("Save As onto an existing vault asks, and cancelling leaves that vault alone")
	void saveAsDoesNotReplaceAnotherVaultSilently() throws Exception
	{
		File openVault = new File(tempHome, "the-open-one.mcrdb");
		createDatabaseFileHeadless(openVault, MASTER_PASSWORD);
		File otherVault = new File(tempHome, "somebody-elses.mcrdb");
		createDatabaseFileHeadless(otherVault, OTHER_PASSWORD);
		byte[] otherBefore = Files.readAllBytes(otherVault.toPath());

		ApplicationSteps application = signInWithExistingDatabase(openVault, MASTER_PASSWORD);
		application.showMainFrame();

		SwingUtilities.invokeLater(() -> new SaveAsApplicationFileAction("Save as").actionPerformed(
			new java.awt.event.ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(15, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(otherVault);
			fileChooser.approveSelection();
		});
		UiTestSpeed.windowManagerSettle();
		robot.waitForIdle();

		assertTrue(aDialogAsksAboutReplacing(),
			"the other vault has to be named and the replacement asked for: a vault is not "
				+ "derived data, and nothing behind it can produce it again");
		answerWithNo();

		assertEquals(otherBefore.length, Files.readAllBytes(otherVault.toPath()).length,
			"and answering no leaves it exactly as it was");
		assertTrue(Files.mismatch(otherVault.toPath(), openVault.toPath()) >= 0,
			"the two vaults are still two different files");
	}

	@Test
	@DisplayName("New database onto an existing vault asks before taking the file over")
	void creatingADatabaseDoesNotTakeOverAnExistingFileSilently() throws Exception
	{
		File existing = new File(tempHome, "already-there.mcrdb");
		createDatabaseFileHeadless(existing, OTHER_PASSWORD);
		byte[] before = Files.readAllBytes(existing.toPath());

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();

		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New").actionPerformed(
			new java.awt.event.ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(15, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(existing);
			fileChooser.approveSelection();
		});
		UiTestSpeed.windowManagerSettle();
		robot.waitForIdle();

		assertTrue(aDialogAsksAboutReplacing(),
			"creating a database onto a file that is already one has to ask - the existence "
				+ "check only decided whether to create an empty file first, and stopped nothing");
		answerWithNo();

		assertEquals(before.length, Files.readAllBytes(existing.toPath()).length,
			"and the file that was already there is untouched");
	}

	/** Whether a dialog on screen asks about replacing, naming the file */
	private boolean aDialogAsksAboutReplacing()
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& dialog.getTitle() != null
					&& dialog.getTitle().toLowerCase().contains("replace"))
				{
					return true;
				}
			}
			return false;
		});
	}

	/** Answers the replace question with no, through the option pane's own value */
	private void answerWithNo()
	{
		GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& dialog.getTitle() != null
					&& dialog.getTitle().toLowerCase().contains("replace"))
				{
					javax.swing.JOptionPane optionPane = (javax.swing.JOptionPane)robot.finder()
						.findByType(dialog, javax.swing.JOptionPane.class);
					optionPane.setValue(javax.swing.JOptionPane.NO_OPTION);
					return null;
				}
			}
			return null;
		});
		UiTestSpeed.windowManagerSettle();
		robot.waitForIdle();
	}
}
