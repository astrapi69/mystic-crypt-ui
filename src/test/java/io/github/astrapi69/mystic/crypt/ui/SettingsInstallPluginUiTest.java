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

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test of installing a plugin through the Settings dialog: starting with no plugins
 * installed, "Install from Zip..." on the Plugins tab picks a built plugin zip, and the plugins
 * table must then list the newly installed, started plugin. Exercises the app's own plugin-install
 * path (the pf4j dogfooding).
 */
class SettingsInstallPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "settings-install-e2e-pw-123";

	@Test
	void installingAPluginFromZipListsItInThePluginsTable() throws Exception
	{
		Assumptions.assumeTrue(Files.exists(OBFUSCATION_ZIP),
			"plugin zip " + OBFUSCATION_ZIP + " not built - run 'make plugins' first");

		File databaseFile = new File(tempHome, "settings-install-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		DialogFixture settings = application.openSettingsDialog();
		JTable table = settings.table("tblPlugins").target();

		// no plugins were pre-installed, so the obfuscation plugin must not be listed yet
		Assumptions.assumeTrue(
			GuiActionRunner.execute(() -> findPluginRow(table, "obfuscation")) < 0,
			"test must start with no obfuscation plugin installed");

		// Install from Zip... -> approve the obfuscation plugin zip in the chooser
		SwingUtilities.invokeLater(() -> settings.button("btnInstallPlugin").target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		File zip = OBFUSCATION_ZIP.toFile().getAbsoluteFile();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(zip);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();

		Pause.pause(new Condition("obfuscation plugin listed and started")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> {
					int row = findPluginRow(table, "obfuscation");
					return row >= 0 && "STARTED".equals(String.valueOf(table.getValueAt(row, 2)));
				});
			}
		}, 10000);

		int row = GuiActionRunner.execute(() -> findPluginRow(table, "obfuscation"));
		assertEquals("STARTED",
			GuiActionRunner.execute(() -> String.valueOf(table.getValueAt(row, 2))),
			"the freshly installed plugin must be listed as STARTED");

		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
	}

	private static int findPluginRow(JTable table, String idFragment)
	{
		for (int row = 0; row < table.getRowCount(); row++)
		{
			Object id = table.getValueAt(row, 0);
			if (id != null && id.toString().toLowerCase().contains(idFragment))
			{
				return row;
			}
		}
		return -1;
	}
}
