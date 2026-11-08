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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.io.File;
import java.nio.file.Path;

import javax.swing.BoxLayout;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Whether a window shows its answer once there is one (#835): the window is packed when it opens,
 * while only the sentence stands in the place of the answer
 */
class AnswerFitsTheWindowTest
{

	@TempDir
	File configuration;

	@TempDir
	Path directory;

	@BeforeEach
	void aConfigurationOfItsOwn()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.getAbsolutePath());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private static JInternalFrame packedAround(final JPanel content)
	{
		JInternalFrame frame = new JInternalFrame("a window", true, true, true, true);
		frame.add(content);
		frame.pack();
		return frame;
	}

	private static AnswerArea anAnswerOfTwentyRows(final JPanel form)
	{
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.add(AnswerArea.toCome("what will show here", "where the answer appears"));
		form.add(new JTextArea(20, 40));
		AnswerArea area = AnswerArea.of(form, 0, new JPanel());
		area.show(false, false);
		return area;
	}

	private LethenonChainPanel aChainWindow(final String chainFile)
	{
		LethenonChainPanel panel = new LethenonChainPanel();
		panel.getModelObject().setChainFile(chainFile);
		return panel;
	}

	private String aChainOfTwoBlocks() throws Exception
	{
		Path chainFile = directory.resolve("chain.lethenon");
		new ChainFile(chainFile).write(
			LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, Bytes.of(new byte[] { 9 })));
		return chainFile.toString();
	}

	private static void assertTheWindowShowsAll(final JInternalFrame frame)
	{
		assertTrue(frame.getHeight() >= frame.getPreferredSize().height,
			"the window is shorter than what it shows: " + frame.getHeight() + " of "
				+ frame.getPreferredSize().height);
		assertTrue(frame.getWidth() >= frame.getPreferredSize().width,
			"the window is narrower than what it shows: " + frame.getWidth() + " of "
				+ frame.getPreferredSize().width);
	}

	@Test
	@DisplayName("an answer area that starts showing its answer grows the window to it (#835)")
	void showingTheAnswer_growsTheWindowToIt()
	{
		JPanel form = new JPanel();
		AnswerArea area = anAnswerOfTwentyRows(form);
		JInternalFrame frame = packedAround(form);
		int packed = frame.getHeight();

		area.show(true, false);

		assertTheWindowShowsAll(frame);
		assertTrue(frame.getHeight() > packed, "the window did not grow");
	}

	@Test
	@DisplayName("a window the person made larger keeps its size (#835)")
	void aWindowMadeLarger_keepsItsSize()
	{
		JPanel form = new JPanel();
		AnswerArea area = anAnswerOfTwentyRows(form);
		JInternalFrame frame = packedAround(form);
		frame.setSize(2000, 2000);

		area.show(true, false);
		area.show(false, false);

		assertEquals(new Dimension(2000, 2000), frame.getSize());
	}

	@Test
	@DisplayName("a window on a desktop grows no further than the desktop's edge (#835)")
	void aWindowOnADesktop_growsNoFurtherThanItsEdge()
	{
		JPanel form = new JPanel();
		AnswerArea area = anAnswerOfTwentyRows(form);
		JInternalFrame frame = packedAround(form);
		JDesktopPane desktop = new JDesktopPane();
		desktop.setSize(1000, frame.getHeight() + 40);
		desktop.add(frame);
		frame.setLocation(0, 10);

		area.show(true, false);

		assertEquals(desktop.getHeight() - 10, frame.getHeight());
	}

	@Test
	@DisplayName("after a check the chain window shows its whole answer (#835)")
	void afterACheck_theChainWindowShowsItsAnswer() throws Exception
	{
		LethenonChainPanel panel = aChainWindow(aChainOfTwoBlocks());
		JInternalFrame frame = packedAround(panel);

		panel.onVerify();

		assertTheWindowShowsAll(frame);
	}

	@Test
	@DisplayName("opening the details after a check grows the chain window to them (#835)")
	void openingTheDetails_growsTheChainWindow() throws Exception
	{
		LethenonChainPanel panel = aChainWindow(aChainOfTwoBlocks());
		JInternalFrame frame = packedAround(panel);
		panel.onVerify();

		panel.onDetails();

		assertTheWindowShowsAll(frame);
	}

	@Test
	@DisplayName("a failure of the check is shown whole, in a window packed before it (#835)")
	void aFailure_isShownWhole()
	{
		LethenonChainPanel panel = aChainWindow(directory.resolve("not-there.lethenon").toString());
		JInternalFrame frame = packedAround(panel);

		panel.onVerify();

		assertTrue(Windows.shows(panel, "txtFailureCause"), "no failure shows");
		assertTheWindowShowsAll(frame);
	}
}
