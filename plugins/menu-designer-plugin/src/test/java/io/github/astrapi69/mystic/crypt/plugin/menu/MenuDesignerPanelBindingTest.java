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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.mystic.crypt.menu.MenuLayoutSupport;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;
import io.github.astrapi69.swing.model.component.JMTextArea;

/**
 * Tests that the xml editor of {@link MenuDesignerPanel} is bound to
 * {@link MenuDesignerPanelModel}: what is typed into the editor is what "Validate" works with, and
 * it is readable in the model without asking the widget.
 * <p>
 * "Validate" is the button this can be proven with in a plain test: it is the only one that does
 * its work without a running application frame, and what it reports depends on nothing but the xml
 * it was given.
 */
@DisplayName("MenuDesignerPanel reads the menu xml from its model")
class MenuDesignerPanelBindingTest
{

	/** The beginning of the message the panel builds from a single validation error */
	private static final String ONE_PROBLEM = "1 problem(s): ";

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void keepTheEditorEmptyOnOpen() throws Exception
	{
		// the panel exports the live menu into the editor on open unless that is switched off;
		// these tests run without a running application frame and decide themselves what the
		// editor holds, so it is switched off through the plugin's own settings
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		PluginSettings.save(configurationDirectory, MenuDesignerSettingsContribution.PLUGIN_ID,
			new MenuDesignerSettingsContribution().getDefaults(),
			Map.of(MenuDesignerSettingsContribution.KEY_EXPORT_ON_OPEN, "false"));
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * Menu xml that is valid because it was written by the exporter the tool itself exports with
	 *
	 * @return a valid menu layout as xml
	 */
	private static String validMenuXml()
	{
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		menu.setName("file");
		JMenuItem menuItem = new JMenuItem("Exit");
		menuItem.setName("exit");
		menu.add(menuItem);
		menuBar.add(menu);
		return MenuLayoutSupport.exportXml(menuBar);
	}

	private static <T extends Component> T named(Container container, String name, Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = named(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void type(MenuDesignerPanel panel, String menuXml)
	{
		named(panel, "txtMenuXml", JTextArea.class).setText(menuXml);
	}

	private static void press(MenuDesignerPanel panel, String buttonName)
	{
		named(panel, buttonName, JButton.class).doClick();
	}

	private static String resultOf(MenuDesignerPanel panel)
	{
		return named(panel, "lblResult", JLabel.class).getText();
	}

	/**
	 * The xml that was typed into the editor is what "Validate" judges - a layout it accepts is
	 * reported as valid
	 */
	@Test
	@DisplayName("valid xml typed into the editor is validated as valid")
	void validateAcceptsTheXmlThatWasTypedIntoTheEditor()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		type(panel, validMenuXml());
		press(panel, "btnValidate");

		assertEquals("valid", resultOf(panel));
	}

	/**
	 * Edge cases: an editor that was never filled, a document that ends in the middle and something
	 * that is no xml at all are all judged on their content, so each one is reported as a problem
	 *
	 * @param typed
	 *            the content of the editor at the moment the button is pressed
	 */
	@ParameterizedTest(name = "[{index}] the editor holding \"{0}\" is reported as a problem")
	@ValueSource(strings = { "", "<menu-info><name>file</name>", "no xml at all" })
	void validateReportsTheProblemOfTheXmlInTheEditor(String typed)
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		type(panel, typed);
		press(panel, "btnValidate");

		assertTrue(resultOf(panel).startsWith(ONE_PROBLEM), () -> "expected a message beginning '"
			+ ONE_PROBLEM + "', the panel reported '" + resultOf(panel) + "'");
	}

	/**
	 * Every press judges what is in the editor at that moment, not what was there when the panel
	 * was built - the value travels into the model with each edit
	 */
	@Test
	@DisplayName("the second press judges the corrected xml, not the one from the first press")
	void validateJudgesTheEditorContentOfEveryPress()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		type(panel, "no xml at all");
		press(panel, "btnValidate");
		assertTrue(resultOf(panel).startsWith(ONE_PROBLEM));

		type(panel, validMenuXml());
		press(panel, "btnValidate");

		assertEquals("valid", resultOf(panel));
	}

	/**
	 * The point of the binding: the state of the panel is readable from the model at any moment,
	 * without asking the editor widget for its content
	 */
	@Test
	@DisplayName("what was typed is readable in the model, not only in the widget")
	void theTypedXmlIsHeldByTheModel()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();
		String menuXml = validMenuXml();

		type(panel, menuXml);

		JMTextArea editor = named(panel, "txtMenuXml", JMTextArea.class);
		assertNotNull(editor, "the xml editor is not a model backed component");
		assertEquals(menuXml, editor.getPropertyModel().getObject());
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	/**
	 * The xml editor expects a specific schema and Reset is destructive - neither is obvious from
	 * the label alone (#161)
	 */
	@Test
	@DisplayName("the editor and every button explain themselves with a tooltip")
	void everyFieldExplainsItselfWithATooltip()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		assertHasTooltip(named(panel, "txtMenuXml", JComponent.class), "menu xml editor");
		assertHasTooltip(named(panel, "btnExport", JComponent.class), "export button");
		assertHasTooltip(named(panel, "btnValidate", JComponent.class), "validate button");
		assertHasTooltip(named(panel, "btnApply", JComponent.class), "apply button");
		assertHasTooltip(named(panel, "btnSave", JComponent.class), "save button");
		assertHasTooltip(named(panel, "btnReset", JComponent.class), "reset button");
		assertHasTooltip(named(panel, "lblResult", JComponent.class), "result label");
	}
}
