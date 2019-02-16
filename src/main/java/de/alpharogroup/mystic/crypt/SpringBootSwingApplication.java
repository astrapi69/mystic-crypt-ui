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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import de.alpharogroup.swing.base.ApplicationSplitPaneFrame;
import de.alpharogroup.swing.base.BaseDesktopMenu;
import de.alpharogroup.swing.components.factories.JComponentFactory;
import de.alpharogroup.swing.panels.output.ConsolePanel;
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
public class SpringBootSwingApplication extends ApplicationSplitPaneFrame<ApplicationModelBean>
{

	public static ConfigurableApplicationContext ctx;

	/** The instance. */
	private static SpringBootSwingApplication instance;

	@Getter
	JComponent leftComponent;

	@Getter
	JComponent rightComponent;

	@Getter
	JComponent topComponent;

	@Getter
	JComponent bottomComponent;

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

	/**
	 * Instantiates a new {@link SpringBootSwingApplication} frame
	 */
	public SpringBootSwingApplication()
	{
		super(Messages.getString("mainframe.title"));
	}

	protected JComponent newLeftComponent() {
		JScrollPane jp = new JScrollPane(new JLabel("Left Component"));
		return jp;
	}
	
	protected JComponent newTopComponent() {
		JSplitPane topJSplitPane = JComponentFactory.newJSplitPane(JSplitPane.HORIZONTAL_SPLIT, newLeftComponent(), newRightComponent());
		topJSplitPane.setDividerLocation(0.2);
		topJSplitPane.setDividerSize(2);
		return topJSplitPane;
	}
	
	protected JComponent newRightComponent() {
		JScrollPane jp = new JScrollPane(new JLabel("Right Component"));
		return jp;
	}
	
	protected JComponent newBottomComponent() {
		ConsolePanel consolePanel = new ConsolePanel();
		JScrollPane jScrollPane = new JScrollPane(consolePanel);
		jScrollPane.setMinimumSize(new Dimension(800, 200));
		jScrollPane.setMaximumSize(new Dimension(800, 200));
		return jScrollPane;
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
	

	/**
	 * Factory method for create a new {@link JSplitPane} object
	 *
	 * @return the new {@link JSplitPane} object
	 */
	protected JSplitPane newJSplitPane()
	{
		JSplitPane mainSplitPane = JComponentFactory.newJSplitPane(JSplitPane.VERTICAL_SPLIT, newTopComponent(), newBottomComponent());
		mainSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setDividerLocation(0.5);
		mainSplitPane.setDividerSize(2);
		mainSplitPane.setResizeWeight(1.0);
		return mainSplitPane;
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
		}
	}

}