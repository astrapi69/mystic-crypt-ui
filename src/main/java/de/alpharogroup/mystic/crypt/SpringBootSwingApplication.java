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
import java.io.File;

import javax.swing.JInternalFrame;

import de.alpharogroup.swing.plaf.LookAndFeels;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import de.alpharogroup.layout.ScreenSizeExtensions;
import de.alpharogroup.swing.base.ApplicationFrame;
import de.alpharogroup.swing.base.BaseDesktopMenu;
import de.alpharogroup.swing.components.factories.JComponentFactory;
import de.alpharogroup.swing.panels.output.ConsolePanel;
import de.alpharogroup.swing.utils.JInternalFrameExtensions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 * The class {@link SpringBootSwingApplication}
 */
@SuppressWarnings("serial")
@SpringBootApplication
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpringBootSwingApplication extends ApplicationFrame<ApplicationModelBean>
{

	public static ConfigurableApplicationContext ctx;

	/** The instance. */
	private static SpringBootSwingApplication instance;

	/**
	 * Gets the single instance of SpringBootSwingApplication.
	 *
	 * @return single instance of SpringBootSwingApplication
	 */
	public static SpringBootSwingApplication getInstance()
	{
		return instance;
	}

	/**
	 * The main method that start this {@link SpringBootSwingApplication}
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(
			SpringBootSwingApplication.class).headless(false).run(args);
		SpringBootSwingApplication.ctx = ctx;

		EventQueue.invokeLater(() -> {
			SpringBootSwingApplication ex = ctx.getBean(SpringBootSwingApplication.class);
			ex.setVisible(true);
		});
	}

	/** The console internal frame. */
	@Getter
	JInternalFrame consoleInternalFrame;

	/** The internal frame. */
	@Getter
	JInternalFrame internalFrame;

	/**
	 * Instantiates a new main frame.
	 */
	public SpringBootSwingApplication()
	{
		super("Untitled");
	}

	public void getConsoleOutput()
	{
		if (consoleInternalFrame == null)
		{
			consoleInternalFrame = JComponentFactory.newInternalFrame("Console", true, true, true,
				true);
			ConsolePanel consolePanel = new ConsolePanel();
			int screenHeight = ScreenSizeExtensions.getScreenHeight(this);
			int screenWidth = ScreenSizeExtensions.getScreenWidth(this);
			JInternalFrameExtensions.addComponentToFrame(consoleInternalFrame, consolePanel);
			JInternalFrameExtensions.addJInternalFrame(
				SpringBootSwingApplication.getInstance().getMainComponent(), consoleInternalFrame);
			consoleInternalFrame.setSize(screenWidth, (screenHeight / 4));
			consoleInternalFrame.setLocation(0, (screenHeight / 4) * 3);
			consoleInternalFrame.setResizable(false);
			consoleInternalFrame.putClientProperty("dragMode", "fixed");
		}
	}

	@Override
	protected File newConfigurationDirectory(final @NonNull String parent,
		final @NonNull String child)
	{
		File applicationConfigurationDirectory = new File(
			super.newConfigurationDirectory(parent, child), "mystic-crypt");
		if (!applicationConfigurationDirectory.exists())
		{
			applicationConfigurationDirectory.mkdir();
		}
		return applicationConfigurationDirectory;
	}

	@Override
	protected BaseDesktopMenu newDesktopMenu(@NonNull Component applicationFrame)
	{
		return new DesktopMenu(applicationFrame);
	}

	@Override
	protected String newIconPath()
	{
		return Messages.getString("global.icon.app.path");
	}

	@Override
	protected void onAfterInitialize()
	{
		super.onAfterInitialize();

		if (instance == null)
		{
			instance = this;
			getConsoleOutput();
		}
		setTitle(Messages.getString("mainframe.title"));
	}

	@Override
	protected LookAndFeels newLookAndFeels()
	{
		return LookAndFeels.METAL;
	}


}
