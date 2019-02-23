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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.MultiSplitLayout;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import de.alpharogroup.lang.ClassExtensions;
import de.alpharogroup.layout.ReplaceContentExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.mystic.crypt.panels.keygen.GenerateKeysPanel;
import de.alpharogroup.swing.base.ApplicationSplitPaneFrame;
import de.alpharogroup.swing.base.BaseDesktopMenu;
import de.alpharogroup.swing.panels.img.ImagePanel;
import de.alpharogroup.swing.panels.output.ConsolePanel;
import de.alpharogroup.swing.panels.splitpane.JXMultiSplitPanePanel;
import de.alpharogroup.swing.panels.splitpane.SplitFactory;
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

	protected JComponent newLeftComponent()
	{
		leftComponent = new JXPanel();
		leftComponent.setLayout(new BorderLayout());
		JLabel label = new JLabel("Left Component");
		leftComponent.add(label);
		Border border = BorderFactory.createLoweredSoftBevelBorder();
		leftComponent.setBorder(border);
		return leftComponent;
	}

	protected JComponent newTopComponent()
	{
		topComponent = new JXPanel();
		topComponent.setLayout(new BorderLayout());
		Border border = BorderFactory.createLoweredSoftBevelBorder();
		topComponent.setBorder(border);
		replaceTopComponent(new GenerateKeysPanel());
		return topComponent;
	}

	protected JComponent newBottomComponent()
	{
		bottomComponent = new JXPanel();
		bottomComponent.setLayout(new BorderLayout());

		JComponent tab1 = new JXPanel();	
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Info", tab1);
		Border border = BorderFactory.createLoweredSoftBevelBorder();
		bottomComponent.add(tabbedPane);
		bottomComponent.setBorder(border);
		return bottomComponent;
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
		}
	}

	@Override
	protected JXMultiSplitPanePanel<ApplicationModelBean> newMainComponent()
	{

		final JXMultiSplitPanePanel<ApplicationModelBean> multiSplitPanePanel = new JXMultiSplitPanePanel<ApplicationModelBean>()
		{
			@Override
			protected MultiSplitLayout.Node newRootNode(String layoutDefinition)
			{
				return SpringBootSwingApplication.this.newRootNode();
			}
		};
		multiSplitPanePanel.getMultiSplitPane().add(newTopComponent(), "content");
		multiSplitPanePanel.getMultiSplitPane().add(newBottomComponent(), "bottom");
		multiSplitPanePanel.getMultiSplitPane().add(newLeftComponent(), "left");
		return multiSplitPanePanel;
	}

	protected MultiSplitLayout.Split newRootNode()
	{
		MultiSplitLayout.Split col1 = SplitFactory.newSplit(false, 0.75);
		MultiSplitLayout.Leaf content = SplitFactory.newLeaf("content", 0.8);
		MultiSplitLayout.Leaf bottom = SplitFactory.newLeaf("bottom", 0.2);
		col1.setChildren(content, new MultiSplitLayout.Divider(), bottom);
		MultiSplitLayout.Split root = new MultiSplitLayout.Split();
		root.setRowLayout(true);
		MultiSplitLayout.Leaf left = SplitFactory.newLeaf("left", 0.25);
		SplitFactory.setChildren(root, left, new MultiSplitLayout.Divider(), col1);
		return root;
	}

	public void replaceTopComponent(final JComponent newContent)
	{
		ReplaceContentExtensions.replaceContent(topComponent, newContent, false);
	}
}