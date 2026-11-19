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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Settings > Plugins > Install from Zip... with the zip of a plugin that is already loaded: pf4j
 * refuses to load a plugin twice, so the panel must not ask it to, and must tell the user that the
 * updated plugin takes effect at the next start. Pinned as it is today: the panel asks anyway and
 * shows "Install failed"
 */
class SettingsInstallZipOfALoadedPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String MARKER_ENTRY = "marker-of-the-newer-version.txt";

	@Test
	@DisplayName("installing a changed zip of a loaded plugin puts the new version in place and is refused as a failed install today")
	void aChangedZipOfALoadedPlugin_isInPlaceAndReportedForTheNextStart() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "install-changed-zip.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		Path changedZip = zipWithAnExtraEntry(OBFUSCATION_ZIP);

		String[] outcome = installFromZipInTheSettings(application, changedZip);

		Path unpacked = tempHome.toPath()
			.resolve(".config/mystic-crypt-ui/plugins/obfuscation-plugin-1.0.0");
		assertTrue(Files.exists(unpacked.resolve(MARKER_ENTRY)),
			"the new version is in place for the next start");
		assertEquals("Install failed", outcome[0], "title: " + outcome[1]);
		assertTrue(outcome[1].contains("already loaded"), "message: " + outcome[1]);
	}

	@Test
	@DisplayName("installing the same zip of a loaded plugin again is refused as a failed install today")
	void theSameZipOfALoadedPlugin_isReportedForTheNextStart() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "install-same-zip.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		String[] outcome = installFromZipInTheSettings(application, OBFUSCATION_ZIP);

		assertEquals("Install failed", outcome[0], "title: " + outcome[1]);
		assertTrue(outcome[1].contains("already loaded"), "message: " + outcome[1]);
	}

	/**
	 * Chooses the zip in Settings > Install from Zip..., reads the dialog the panel answers with,
	 * dismisses it and closes the settings
	 *
	 * @return the title and the message of that dialog
	 */
	private String[] installFromZipInTheSettings(final ApplicationSteps application, final Path zip)
	{
		DialogFixture settings = application.openSettingsDialog();
		SwingUtilities.invokeLater(() -> settings.button("btnInstallPlugin").target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		File zipFile = zip.toFile().getAbsoluteFile();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(zipFile);
			fileChooser.approveSelection();
		});
		JOptionPaneFixture answer = JOptionPaneFinder.findOptionPane()
			.withTimeout(15, TimeUnit.SECONDS).using(robot);
		String[] titleAndMessage = GuiActionRunner.execute(() -> new String[] {
				((JDialog)SwingUtilities.getWindowAncestor(answer.target())).getTitle(),
				String.valueOf(answer.target().getMessage()) });
		answer.okButton().click();
		robot.waitForIdle();
		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
		return titleAndMessage;
	}

	/** A copy of the zip, under the same name in a directory of its own, with one more entry */
	private Path zipWithAnExtraEntry(final Path zip) throws Exception
	{
		Path changed = Files.createDirectories(tempHome.toPath().resolve("changed-zip"))
			.resolve(zip.getFileName());
		try (ZipFile source = new ZipFile(zip.toFile());
			OutputStream out = Files.newOutputStream(changed);
			ZipOutputStream target = new ZipOutputStream(out))
		{
			for (var entries = source.entries(); entries.hasMoreElements();)
			{
				ZipEntry entry = entries.nextElement();
				target.putNextEntry(new ZipEntry(entry.getName()));
				source.getInputStream(entry).transferTo(target);
				target.closeEntry();
			}
			target.putNextEntry(new ZipEntry(MARKER_ENTRY));
			target.write(new byte[] { 1 });
			target.closeEntry();
		}
		return changed;
	}

}
