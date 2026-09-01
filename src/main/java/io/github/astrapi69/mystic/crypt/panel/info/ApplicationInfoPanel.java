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
package io.github.astrapi69.mystic.crypt.panel.info;

import java.awt.Cursor;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import io.github.astrapi69.browser.BrowserControlExtensions;
import io.github.astrapi69.icon.ImageIconFactory;
import net.miginfocom.swing.MigLayout;

/**
 * What the Help &gt; Info dialog shows: the application icon, name and version, copyright and
 * license, and links to the project on GitHub and its wiki - replacing the library's
 * {@code InfoPanel}, whose layout and fixed four-field content are not overridable, with a panel
 * this application controls
 */
public class ApplicationInfoPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	public ApplicationInfoPanel(final ApplicationInfo info)
	{
		setLayout(new MigLayout("insets 24, wrap 1", "[center]"));

		add(iconLabel(), "align center, gapbottom 12");

		JLabel applicationName = new JLabel(info.applicationName());
		applicationName.setFont(applicationName.getFont().deriveFont(Font.BOLD, 18f));
		add(applicationName, "align center");

		add(new JLabel("Version " + info.version()), "align center, gapbottom 12");
		add(new JLabel(info.copyright()), "align center");
		add(new JLabel(info.licenseSummary()), "align center, gapbottom 12");

		add(linkButton("Project on GitHub", info.githubUrl()), "align center");
		add(linkButton("Wiki", info.wikiUrl()), "align center");
	}

	private JLabel iconLabel()
	{
		ImageIcon icon = ImageIconFactory.newImageIcon("img/icon.png");
		JLabel label = new JLabel(icon);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		return label;
	}

	private JButton linkButton(final String text, final String url)
	{
		JButton link = new JButton("<html><a href=''>" + text + "</a></html>");
		link.setName("btn" + text.replaceAll("\\s", ""));
		link.setBorderPainted(false);
		link.setContentAreaFilled(false);
		link.setFocusPainted(false);
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.addActionListener(
			event -> BrowserControlExtensions.displayURLonStandardBrowser(this, url));
		return link;
	}

}
