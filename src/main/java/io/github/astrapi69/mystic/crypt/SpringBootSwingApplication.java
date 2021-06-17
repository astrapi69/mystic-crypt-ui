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

import de.alpharogroup.file.search.PathFinder;
import de.alpharogroup.file.system.SystemFileExtensions;
import de.alpharogroup.file.write.WriteFileExtensions;
import de.alpharogroup.json.ObjectToJsonExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import io.github.astrapi69.crypto.algorithm.Algorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.CryptModelFactory;
import io.github.astrapi69.crypto.file.GenericObjectEncryptor;
import io.github.astrapi69.crypto.file.PBEFileEncryptor;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.crypto.model.StringDecorator;
import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFilePanel;
import io.github.astrapi69.swing.base.ApplicationFrame;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.components.factories.JComponentFactory;
import io.github.astrapi69.swing.dialog.factories.JDialogFactory;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.panels.output.ConsolePanel;
import io.github.astrapi69.swing.plaf.LookAndFeels;
import io.github.astrapi69.swing.splashscreen.BaseSplashScreen;
import io.github.astrapi69.swing.splashscreen.SplashScreenModelBean;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.crypto.Cipher;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * The class {@link SpringBootSwingApplication}
 */
@SuppressWarnings("serial") @SpringBootApplication @FieldDefaults(level = AccessLevel.PRIVATE)
public class SpringBootSwingApplication
	extends ApplicationFrame<ApplicationModelBean>
{

	public static final String APPLICATION_NAME = "mystic-crypt-ui";

	/** Constant for the default configuration directory from the current user. current value:".config" */
	public static final String DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME = ".config";

	public static ConfigurableApplicationContext ctx;

	/**
	 * The instance.
	 */
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
	 * @param args the arguments
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
			text = SpringBootSwingApplication.APPLICATION_NAME;
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

	/**
	 * The console internal frame.
	 */
	@Getter JInternalFrame consoleInternalFrame;

	/**
	 * The internal frame.
	 */
	@Getter JInternalFrame internalFrame;

	/**
	 * Instantiates a new main frame.
	 */
	public SpringBootSwingApplication()
	{
		super(Messages.getString("mainframe.title"));
	}

	@Override protected void onBeforeInitialize()
	{
		if (instance == null)
		{
			instance = this;
		}
		// initialize model and model object
		setModel(BaseModel.of(ApplicationModelBean.builder().build()));
		super.onBeforeInitialize();
	}

	@Override protected void onBeforeInitializeComponents()
	{
		// start
		// TODO delete when app-file created
//		generateTempApplicationModelFile();
		generateTempApplicationModelFileWithPassword();
		// TODO delete when app-file created
		// end
		showMasterPwDialog();
		super.onBeforeInitializeComponents();
	}
	private void generateTempApplicationModelFileWithPassword()
	{

		PBEFileEncryptor encryptor;
		String password;
		CryptModel<Cipher, String, String> cryptModel;

		password = "test1234";
		ApplicationModelBean modelObject = getModelObject();
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.masterPw(password.toCharArray()).build();
		modelObject.setMasterPwFileModelBean(masterPwFileModelBean);
		File appConfigDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			SpringBootSwingApplication.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			SpringBootSwingApplication.APPLICATION_NAME);
		File appData = new File(appConfigDir, "app-data-only-pw.json");

		cryptModel = CryptModel.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).build();
		encryptor = RuntimeExceptionDecorator.decorate(() -> new PBEFileEncryptor(cryptModel));
		String json = RuntimeExceptionDecorator.decorate(() -> ObjectToJsonExtensions.toJson(modelObject));
		RuntimeExceptionDecorator.decorate(() -> WriteFileExtensions.string2File(appData, json));
		File encryptedAppData = RuntimeExceptionDecorator.decorate(() -> encryptor.encrypt(appData));
		System.out.println(encryptedAppData.getAbsolutePath());
	}

	private void generateTempApplicationModelFile()
	{
		String key;
		Algorithm algorithm;
		File encrypted;
		GenericObjectEncryptor<ApplicationModelBean, String> encryptor;
		CryptModel<Cipher, String, String> cryptModel;

		ApplicationModelBean modelObject = getModelObject();
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.masterPw("test1234".toCharArray()).build();
		modelObject.setMasterPwFileModelBean(masterPwFileModelBean);
		key = "D1D15ED36B887AF1";
		algorithm = SunJCEAlgorithm.PBEWithMD5AndDES;

		cryptModel = CryptModelFactory.newCryptModel(algorithm, key);

		File appConfigDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			SpringBootSwingApplication.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			SpringBootSwingApplication.APPLICATION_NAME);
		File appData = new File(appConfigDir, "app-data.enc");
		masterPwFileModelBean.setAppDataFile(appData.getAbsolutePath());
		encryptor =
			RuntimeExceptionDecorator.decorate(() -> new GenericObjectEncryptor<>(cryptModel, appData));
		encrypted = RuntimeExceptionDecorator.decorate(() ->encryptor.encrypt(modelObject));
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
		String configurationDirectoryName = "mystic-crypt-ui";
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
		getConsoleOutput();
		setTitle(Messages.getString("mainframe.title"));
	}

	@Override protected LookAndFeels newLookAndFeels()
	{
		return LookAndFeels.METAL;
	}


}
