/**
 * The MIT License
 * <p>
 * Copyright (C) 2015 Asterios Raptis
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt;

import de.alpharogroup.layout.ScreenSizeExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.ApplicationFrame;
import de.alpharogroup.swing.base.BaseDesktopMenu;
import de.alpharogroup.swing.components.factories.JComponentFactory;
import de.alpharogroup.swing.dialog.factories.JDialogFactory;
import de.alpharogroup.swing.listener.RequestFocusListener;
import de.alpharogroup.swing.panels.output.ConsolePanel;
import de.alpharogroup.swing.plaf.LookAndFeels;
import de.alpharogroup.swing.splashscreen.BaseSplashScreen;
import de.alpharogroup.swing.splashscreen.SplashScreenModelBean;
import de.alpharogroup.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFilePanel;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * The class {@link SpringBootSwingApplication}
 */
@SuppressWarnings("serial") @SpringBootApplication @FieldDefaults(level = AccessLevel.PRIVATE) public class SpringBootSwingApplication
	extends ApplicationFrame<ApplicationModelBean>
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
		String imagePath;
		String text;
		imagePath = Messages.getString("global.icon.app.path");
		if (imagePath == null)
			imagePath = "img/icon.png";
		text = Messages.getString("mainframe.project.name");
		if (text == null)
			text = "mystic-crypt-ui";
		SplashScreenModelBean splashScreenModelBean = SplashScreenModelBean.builder()
			.imagePath(imagePath).text(text).min(0).max(100).showTime(3000).showing(true).build();
		new Thread(() -> {
			Model<SplashScreenModelBean> modelBeanModel = BaseModel.of(splashScreenModelBean);
			new BaseSplashScreen(null, modelBeanModel);
		}).start();

		RuntimeExceptionDecorator.decorate(i -> Thread.sleep(splashScreenModelBean.getShowTime()));

		ConfigurableApplicationContext context = new SpringApplicationBuilder(
			SpringBootSwingApplication.class).headless(false).run(args);
		SpringBootSwingApplication.ctx = context;

		EventQueue.invokeLater(() -> {
			SpringBootSwingApplication springBootSwingApplicationFrame = context
				.getBean(SpringBootSwingApplication.class);
			springBootSwingApplicationFrame.setVisible(true);
		});
	}

	@Override protected void onBeforeInitialize()
	{
		showMasterPwDialog();
		super.onBeforeInitialize();
	}

	private void showMasterPwOptionPane()
	{
		MasterPwFilePanel masterPwFilePanel = new MasterPwFilePanel();
		JOptionPane optionPane = new JOptionPane(masterPwFilePanel, JOptionPane.PLAIN_MESSAGE);

		JDialog dialog = JDialogFactory.newJDialog(this, optionPane, "Enter your credentials");
		dialog.addWindowFocusListener(new RequestFocusListener(masterPwFilePanel.getTxtMasterPw()));
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void showMasterPwDialog()
	{
		MasterPwFileDialog dialog = new MasterPwFileDialog(this, "Enter your credentials", true,
			BaseModel.<MasterPwFileModelBean>of(MasterPwFileModelBean.builder().build()));
		dialog.setSize(820, 380);
		dialog.setVisible(true);
	}

	/** The console internal frame. */
	@Getter JInternalFrame consoleInternalFrame;

	/** The internal frame. */
	@Getter JInternalFrame internalFrame;

	/**
	 * Instantiates a new main frame.
	 */
	public SpringBootSwingApplication()
	{
		super(Messages.getString("mainframe.title"));
	}

	public void getConsoleOutput()
	{
		if (consoleInternalFrame == null)
		{
			consoleInternalFrame = JComponentFactory
				.newInternalFrame("Console", true, true, true, true);
			ConsolePanel consolePanel = new ConsolePanel();
			int screenHeight = ScreenSizeExtensions.getScreenHeight(this);
			int screenWidth = ScreenSizeExtensions.getScreenWidth(this);
			JInternalFrameExtensions.addComponentToFrame(consoleInternalFrame, consolePanel);
			JInternalFrameExtensions
				.addJInternalFrame(SpringBootSwingApplication.getInstance().getMainComponent(),
					consoleInternalFrame);
			consoleInternalFrame.setSize(screenWidth, (screenHeight / 4));
			consoleInternalFrame.setLocation(0, (screenHeight / 4) * 3);
			consoleInternalFrame.setResizable(false);
			consoleInternalFrame.putClientProperty("dragMode", "fixed");
		}
	}

	@Override protected File newConfigurationDirectory(final @NonNull String parent,
		final @NonNull String child)
	{
		String configurationDirectoryName = "mystic-crypt";
		File applicationConfigurationDirectory = new File(
			super.newConfigurationDirectory(parent, child), configurationDirectoryName);
		if (!applicationConfigurationDirectory.exists())
		{
			applicationConfigurationDirectory.mkdir();
		}
		return applicationConfigurationDirectory;
	}

	@Override protected BaseDesktopMenu newDesktopMenu(@NonNull Component applicationFrame)
	{
		return new DesktopMenu(applicationFrame);
	}

	@Override protected String newIconPath()
	{
		return Messages.getString("global.icon.app.path");
	}

	@Override protected void onAfterInitialize()
	{
		super.onAfterInitialize();

		if (instance == null)
		{
			instance = this;
			getConsoleOutput();
		}
		setTitle(Messages.getString("mainframe.title"));
	}

	@Override protected LookAndFeels newLookAndFeels()
	{
		return LookAndFeels.METAL;
	}


}
