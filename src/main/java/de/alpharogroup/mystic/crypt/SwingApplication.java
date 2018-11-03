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
package de.alpharogroup.mystic.crypt;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.swingx.JXFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import de.alpharogroup.lang.ClassExtensions;
import de.alpharogroup.layout.ScreenSizeExtensions;
import de.alpharogroup.swing.components.factories.JComponentFactory;
import de.alpharogroup.swing.desktoppane.SingletonDesktopPane;
import de.alpharogroup.swing.plaf.LookAndFeels;
import de.alpharogroup.swing.utils.JInternalFrameExtensions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


/**
 * The Class SwingApplication.
 */
@SuppressWarnings("serial")
@SpringBootApplication
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwingApplication extends JXFrame
{

	public static void main(String[] args)
	{

		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SwingApplication.class)
			.headless(false).run(args);
		SwingApplication.ctx = ctx;

		EventQueue.invokeLater(() -> {
			SwingApplication ex = ctx.getBean(SwingApplication.class);
			ex.setVisible(true);
		});
	}

	/** The instance. */
	private static SwingApplication instance;
	public static ConfigurableApplicationContext ctx;

	/**
	 * Gets the single instance of SwingApplication.
	 *
	 * @return single instance of SwingApplication
	 */
	public static SwingApplication getInstance()
	{
		return instance;
	}

	/** The current look and feels. */
	@Getter
	@Setter
	LookAndFeels currentLookAndFeels = LookAndFeels.SYSTEM;

	/** The current visible internal frame. */
	@Getter
	@Setter
	JInternalFrame currentVisibleInternalFrame;

	/** The desktop pane. */
	@Getter
	final JDesktopPane desktopPane = SingletonDesktopPane.getInstance();

	/** The internal frame. */
	@Getter
	JInternalFrame internalFrame;

	/** The menubar. */
	@Getter
	JMenuBar menubar;

	/** The toolbar. */
	@Getter
	JToolBar toolbar;

	@Getter
	DesktopMenu menu;

	/**
	 * Instantiates a new main frame.
	 */
	public SwingApplication()
	{
		super(Messages.getString("mainframe.title"));
		if (instance == null)
		{
			instance = this;
		}
		initComponents();
	}

	/**
	 * Inits the components.
	 */
	protected void initComponents()
	{
		menu = new DesktopMenu();
		setJMenuBar(menu.getMenubar());
		toolbar = new JToolBar(); // create the tool bar
		setToolBar(toolbar);

		getContentPane().add(desktopPane);
		try
		{
			String iconPath = Messages.getString("global.icon.app.path");
			BufferedImage appIcon = ImageIO.read(ClassExtensions.getResourceAsStream(iconPath));
			setIconImage(appIcon);
		}
		catch (IOException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice[] gs = ge.getScreenDevices();
		setSize(ScreenSizeExtensions.getScreenWidth(gs[0]),
			ScreenSizeExtensions.getScreenHeight(gs[0]));
		setVisible(true);

		// Set default look and feel...
		setDefaultLookAndFeel(LookAndFeels.SYSTEM, this);

	}

	protected LookAndFeels setDefaultLookAndFeel(LookAndFeels lookAndFeels, Component component)
	{
		try
		{
			LookAndFeels.setLookAndFeel(lookAndFeels, this);
			setCurrentLookAndFeels(lookAndFeels);
		}
		catch (final ClassNotFoundException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}
		catch (final InstantiationException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}
		catch (final IllegalAccessException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}
		catch (final UnsupportedLookAndFeelException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>"
				+ "<p>" + e.getMessage();
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
			log.error(e.getMessage(), e);
		}
		return lookAndFeels;
	}

	/**
	 * Replace the current internal frame with a new internal frame with the given {@link Component}
	 * as content.
	 *
	 * @param title
	 *            the title
	 * @param component
	 *            the component
	 */
	public void replaceInternalFrame(final String title, final Component component)
	{
		if (getCurrentVisibleInternalFrame() != null)
		{
			getCurrentVisibleInternalFrame().dispose();
		}
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame(title, true, true,
			true, true);
		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		JInternalFrameExtensions.addJInternalFrame(desktopPane, internalFrame);
		setCurrentVisibleInternalFrame(internalFrame);
	}

}