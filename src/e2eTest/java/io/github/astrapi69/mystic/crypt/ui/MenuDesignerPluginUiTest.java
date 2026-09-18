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
import java.nio.file.Files;

import javax.swing.JMenuBar;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the menu-designer plugin: opens the "Menu Designer" tool, which
 * shows the running menu as xml, validates it, applies it back to the live menu bar and saves it as
 * the layout for the next start - all through the real UI.
 * <p>
 * The tool is for development and an installation does not receive it, so the test asks for it the
 * way a developer does, with the property the plugin looks at.
 */
class MenuDesignerPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** The property the plugin looks at, kept here rather than depending on the plugin's class */
	private static final String DEVELOPER_PROPERTY = "mystic.crypt.ui.menu.designer";

	private String originalProperty;

	@BeforeEach
	void askForTheDevelopmentTool()
	{
		originalProperty = System.getProperty(DEVELOPER_PROPERTY);
		System.setProperty(DEVELOPER_PROPERTY, "true");
	}

	@AfterEach
	void stopAskingForIt()
	{
		if (originalProperty == null)
		{
			System.clearProperty(DEVELOPER_PROPERTY);
		}
		else
		{
			System.setProperty(DEVELOPER_PROPERTY, originalProperty);
		}
	}

	@Test
	void exportsValidatesAppliesAndSavesTheMenuThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(MENU_DESIGNER_ZIP);

		File databaseFile = new File(tempHome, "menu-designer-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Menu Designer", "Menu Designer");

		// the panel exports the running menu when it opens
		String xml = GuiActionRunner.execute(() -> frame.textBox("txtMenuXml").target().getText());
		assertTrue(xml.contains("menubar"), "the editor must show the menu as xml, was: " + xml);
		assertTrue(xml.contains("global.menu.file"),
			"the exported xml must carry the menu ids, was: " + xml);

		GuiActionRunner.execute(() -> frame.button("btnValidate").target().doClick());
		robot.waitForIdle();
		assertEquals("valid",
			GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()),
			"the exported xml must validate");

		// applying it rebuilds the live menu bar - the Help menu must still be the last one
		GuiActionRunner.execute(() -> frame.button("btnApply").target().doClick());
		robot.waitForIdle();
		assertTrue(
			GuiActionRunner
				.execute(() -> frame.label("lblResult").target().getText().contains("applied")),
			"the panel must report that the layout was applied");
		Boolean helpIsLast = GuiActionRunner.execute(() -> {
			JMenuBar menuBar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			return "global.menu.help".equals(menuBar.getMenu(menuBar.getMenuCount() - 1).getName());
		});
		assertTrue(helpIsLast, "after applying the layout the Help menu must still be last");

		// saving writes the layout that the next start picks up
		GuiActionRunner.execute(() -> frame.button("btnSave").target().doClick());
		robot.waitForIdle();
		File layoutFile = new File(tempHome, ".config/mystic-crypt-ui/menubar.xml");
		assertTrue(Files.exists(layoutFile.toPath()),
			"saving must write " + layoutFile + ", status was: "
				+ GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()));
	}
}
