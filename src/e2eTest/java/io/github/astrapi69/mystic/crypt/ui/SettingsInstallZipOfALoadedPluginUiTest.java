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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTable;
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
 * Settings > Plugins > Install from Zip... with a zip named like the zip of a plugin that is loaded
 * (#1045). The zip is checked before anything of the installed plugin is touched: a zip of the
 * plugin itself, changed, is an update for the next start, the same one changes nothing, and a file
 * that is no plugin zip, holds no plugin descriptor or holds another plugin leaves the installed
 * plugin as it is and is refused
 */
class SettingsInstallZipOfALoadedPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String MARKER_ENTRY = "marker-of-the-newer-version.txt";

	private static final String SENTINEL = "not-from-the-zip.txt";

	private static final String UPDATED_TITLE = "Plugin updated at the next start";

	@Test
	@DisplayName("installing a changed zip of a loaded plugin puts the new version in place, reports the update for the next start and leaves the running plugin usable")
	void aChangedZipOfALoadedPlugin_isInPlaceAndReportedForTheNextStart() throws Exception
	{
		ApplicationSteps application = signedInWithTheObfuscationPlugin("changed-zip");
		Path changedZip = zipOfTheObfuscationPlugin(entry -> true, true);

		String[] outcome = installFromZipInTheSettings(application, changedZip, false);

		assertTrue(Files.exists(unpackedPlugin().resolve(MARKER_ENTRY)),
			"the new version is in place for the next start");
		assertEquals(UPDATED_TITLE, outcome[0], "title: " + outcome[1]);
		assertTrue(outcome[1].contains("next start"), "message: " + outcome[1]);
		application.showMainFrame();
		application.openPluginTool("Operated Obfuscation", "Operated Obfuscation");
		assertTrue(application.isInternalFrameShowing("Operated Obfuscation"),
			"the running plugin still loads its classes after its directory was replaced");
	}

	@Test
	@DisplayName("installing the same zip of a loaded plugin again says that nothing changed and leaves the unpacked directory alone")
	void theSameZipOfALoadedPlugin_changesNothing() throws Exception
	{
		ApplicationSteps application = signedInWithTheObfuscationPlugin("same-zip");
		Files.writeString(unpackedPlugin().resolve(SENTINEL), "kept");

		String[] outcome = installFromZipInTheSettings(application, OBFUSCATION_ZIP, false);

		assertEquals(UPDATED_TITLE, outcome[0], "title: " + outcome[1]);
		assertFalse(Files.exists(unpackedPlugin().resolve(SENTINEL)),
			"today the plugin's directory is unpacked again before pf4j refuses to load it");
	}

	@Test
	@DisplayName("installing a changed zip of a loaded but disabled plugin leaves it disabled and says so")
	void aChangedZipOfADisabledPlugin_staysDisabled() throws Exception
	{
		ApplicationSteps application = signedInWithTheObfuscationPlugin("disabled-zip");
		Path changedZip = zipOfTheObfuscationPlugin(entry -> true, true);

		String[] outcome = installFromZipInTheSettings(application, changedZip, true);

		assertEquals(UPDATED_TITLE, outcome[0], "title: " + outcome[1]);
		assertTrue(outcome[1].contains("in use"), "message: " + outcome[1]);
	}

	@Test
	@DisplayName("a file that is no zip is refused and the installed plugin and its zip stay as they were")
	void aFileThatIsNoZip_isRefusedAndTheInstalledPluginStays() throws Exception
	{
		ApplicationSteps application = signedInWithTheObfuscationPlugin("not-a-zip");
		byte[] installedZip = Files.readAllBytes(installedZipFile());
		Path notAZip = Files.createDirectories(tempHome.toPath().resolve("not-a-zip"))
			.resolve(OBFUSCATION_ZIP.getFileName());
		Files.writeString(notAZip, "<html>not a zip</html>");

		String[] outcome = installFromZipInTheSettings(application, notAZip, false);

		assertEquals(UPDATED_TITLE, outcome[0], "title: " + outcome[1]);
		assertFalse(Files.exists(unpackedPlugin().resolve("plugin.properties")),
			"today the installed plugin's directory is gone");
		assertTrue(Files.readString(installedZipFile()).startsWith("<html>"),
			"today the installed zip is overwritten by the file that is no zip");
	}

	@Test
	@DisplayName("a zip without a plugin descriptor is refused and the installed plugin stays as it was")
	void aZipWithoutAPluginDescriptor_isRefusedAndTheInstalledPluginStays() throws Exception
	{
		ApplicationSteps application = signedInWithTheObfuscationPlugin("no-descriptor");
		byte[] installedZip = Files.readAllBytes(installedZipFile());
		Path withoutDescriptor = zipOfTheObfuscationPlugin(
			entry -> !"plugin.properties".equals(entry), false);

		String[] outcome = installFromZipInTheSettings(application, withoutDescriptor, false);

		assertEquals(UPDATED_TITLE, outcome[0], "title: " + outcome[1]);
		assertFalse(Files.exists(unpackedPlugin().resolve("plugin.properties")),
			"today the installed plugin's directory is replaced by one without a descriptor");
	}

	@Test
	@DisplayName("the zip of another plugin under the name of a loaded one is refused and the installed plugin stays as it was")
	void theZipOfAnotherPlugin_isRefusedAndTheInstalledPluginStays() throws Exception
	{
		ApplicationSteps application = signedInWithTheObfuscationPlugin("other-plugin");
		TestPrerequisites.requireBuiltPluginZip(CHECKSUM_ZIP);
		byte[] installedZip = Files.readAllBytes(installedZipFile());
		Path otherPlugin = Files.createDirectories(tempHome.toPath().resolve("other-zip"))
			.resolve(OBFUSCATION_ZIP.getFileName());
		Files.copy(CHECKSUM_ZIP, otherPlugin);

		String[] outcome = installFromZipInTheSettings(application, otherPlugin, false);

		assertEquals(UPDATED_TITLE, outcome[0], "title: " + outcome[1]);
		assertTrue(
			Files.readString(unpackedPlugin().resolve("plugin.properties"))
				.contains("checksum-plugin"),
			"today the installed plugin's directory is replaced by the other plugin");
	}

	private ApplicationSteps signedInWithTheObfuscationPlugin(final String name) throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "install-" + name + ".mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		return signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
	}

	private Path unpackedPlugin()
	{
		return tempHome.toPath()
			.resolve(".config/mystic-crypt-ui/plugins/obfuscation-plugin-1.0.0");
	}

	private Path installedZipFile()
	{
		return tempHome.toPath()
			.resolve(".config/mystic-crypt-ui/plugins/" + OBFUSCATION_ZIP.getFileName());
	}

	/**
	 * Chooses the zip in Settings > Install from Zip..., reads the dialog the panel answers with,
	 * dismisses it and closes the settings
	 *
	 * @return the title and the message of that dialog
	 */
	private String[] installFromZipInTheSettings(final ApplicationSteps application, final Path zip,
		final boolean disableThePluginFirst)
	{
		DialogFixture settings = application.openSettingsDialog();
		if (disableThePluginFirst)
		{
			JTable table = settings.table("tblPlugins").target();
			GuiActionRunner.execute(() -> {
				table.setRowSelectionInterval(0, 0);
				settings.button("btnDisablePlugin").target().doClick();
			});
			robot.waitForIdle();
		}
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

	/**
	 * A copy of the obfuscation plugin's zip under the same name in a directory of its own, with
	 * the entries the filter keeps and, when asked, one more entry
	 */
	private Path zipOfTheObfuscationPlugin(final Predicate<String> keep, final boolean withMarker)
		throws Exception
	{
		Path changed = Files.createDirectories(tempHome.toPath().resolve("changed-zip"))
			.resolve(OBFUSCATION_ZIP.getFileName());
		try (ZipFile source = new ZipFile(OBFUSCATION_ZIP.toFile());
			OutputStream out = Files.newOutputStream(changed);
			ZipOutputStream target = new ZipOutputStream(out))
		{
			for (var entries = source.entries(); entries.hasMoreElements();)
			{
				ZipEntry entry = entries.nextElement();
				if (!keep.test(entry.getName()))
				{
					continue;
				}
				target.putNextEntry(new ZipEntry(entry.getName()));
				source.getInputStream(entry).transferTo(target);
				target.closeEntry();
			}
			if (withMarker)
			{
				target.putNextEntry(new ZipEntry(MARKER_ENTRY));
				target.write(new byte[] { 1 });
				target.closeEntry();
			}
		}
		return changed;
	}

}
