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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The view "Before you start" (#540) holds the short checklist and the documents in its model, shows
 * its notice where a user sees it, lists every check in a table nobody can edit, and offers one
 * button per document that names the address it opens.
 */
class LethenonBeforeYouStartPanelBindingTest
{

	@Test
	@DisplayName("the notice that this is not legal advice is visible in a window that shows the view")
	void theNotice_isVisible() throws Exception
	{
		AtomicBoolean showing = new AtomicBoolean();
		String[] noticeText = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			LethenonBeforeYouStartPanel panel = new LethenonBeforeYouStartPanel();
			JFrame frame = new JFrame();
			try
			{
				frame.add(panel);
				frame.pack();
				frame.setVisible(true);
				JLabel notice = componentNamed(panel, "lblNotLegalAdvice", JLabel.class);
				assertNotNull(notice, "the view has its notice");
				showing.set(notice.isShowing() && notice.getWidth() > 0);
				noticeText[0] = notice.getText();
			}
			finally
			{
				frame.dispose();
			}
		});

		assertTrue(showing.get(), "the notice is on the screen, not only in the component tree");
		assertEquals(LaunchChecklist.notice(LethenonMessages::getString), noticeText[0]);
	}

	@Test
	@DisplayName("the table shows every check of the model, and none of its cells can be edited")
	void theTable_showsEveryCheck()
	{
		LethenonBeforeYouStartPanel panel = new LethenonBeforeYouStartPanel();
		JTable table = componentNamed(panel, "tblLaunchChecks", JTable.class);

		assertNotNull(table);
		assertFalse(panel.getModelObject().getChecks().isEmpty());
		assertEquals(panel.getModelObject().getChecks().size(), table.getRowCount());
		assertEquals(LaunchCheckTableModel.COLUMN_COUNT, table.getColumnCount());
		for (int row = 0; row < table.getRowCount(); row++)
		{
			for (int column = 0; column < table.getColumnCount(); column++)
			{
				assertFalse(table.isCellEditable(row, column), row + "/" + column);
			}
		}
		assertEquals(panel.getModelObject().getChecks().getFirst().activity(),
			table.getValueAt(0, LaunchCheckTableModel.ACTIVITY));
	}

	@Test
	@DisplayName("every document has a button that names the address it opens")
	void everyDocument_hasAButton()
	{
		LethenonBeforeYouStartPanel panel = new LethenonBeforeYouStartPanel();

		assertEquals(LaunchDocument.values().length, panel.getModelObject().getDocuments().size());
		for (LaunchDocument document : panel.getModelObject().getDocuments())
		{
			JButton button = componentNamed(panel, document.componentName(), JButton.class);
			assertNotNull(button, document.componentName());
			assertEquals(document.title(LethenonMessages::getString), button.getText());
			assertTrue(button.getToolTipText().contains(document.url()), button.getToolTipText());
		}
	}

	private static <T extends Component> T componentNamed(final Container container,
		final String name, final Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container nested)
			{
				T found = componentNamed(nested, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
