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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.swing.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import io.github.astrapi69.crypto.algorithm.AesAlgorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.SecretKeyFactoryExtensions;
import io.github.astrapi69.crypto.file.PBEFileEncryptor;
import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.PublicKeyEncryptor;
import io.github.astrapi69.crypto.key.PublicKeyGenericEncryptor;
import io.github.astrapi69.crypto.key.reader.EncryptedPrivateKeyReader;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.json.ObjectToJsonExtensions;
import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panels.signin.NewMasterPwFileDialog;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.swing.base.ApplicationFrame;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.button.IconButtonFactory;
import io.github.astrapi69.swing.components.factories.JComponentFactory;
import io.github.astrapi69.swing.icon.ImageIconFactory;
import io.github.astrapi69.swing.panels.output.ConsolePanel;
import io.github.astrapi69.swing.plaf.LookAndFeels;
import io.github.astrapi69.swing.splashscreen.ProgressBarSplashScreen;
import io.github.astrapi69.swing.splashscreen.SplashScreenModelBean;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.system.SystemFileExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import io.github.astrapi69.write.WriteFileExtensions;

/**
 * The class {@link MysticCryptApplicationFrame}
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysticCryptApplicationFrame extends ApplicationFrame<ApplicationModelBean>
{
	/**
	 * The main method that start this {@link MysticCryptApplicationFrame}
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
		frame.setVisible(true);
	}

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	public static final String APPLICATION_NAME = "mystic-crypt-ui";

	/**
	 * Constant for the default configuration directory from the current user. current
	 * value:".config"
	 */
	public static final String DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME = ".config";

	/**
	 * The instance.
	 */
	private static MysticCryptApplicationFrame instance;

	/**
	 * Gets the single instance of SpringBootSwingApplication.
	 *
	 * @return single instance of SpringBootSwingApplication
	 */
	public static MysticCryptApplicationFrame getInstance()
	{
		return instance;
	}

	private void showMasterPwDialog()
	{
		Model<MasterPwFileModelBean> model = BaseModel.<MasterPwFileModelBean>of(
			MasterPwFileModelBean.builder().minPasswordLength(6).withKeyFile(false)
				.withMasterPw(false).showMasterPw(false).build());
		MasterPwFileDialog dialog = new MasterPwFileDialog(this,
			"Enter your credentials", true,
			model);
		dialog.setSize(880, 380);
		dialog.setVisible(true);
	}

	/**
	 * The console internal frame.
	 */
	@Getter
	JInternalFrame consoleInternalFrame;

	/**
	 * The internal frame.
	 */
	@Getter
	JInternalFrame internalFrame;

	/**
	 * Instantiates a new main frame.
	 */
	public MysticCryptApplicationFrame()
	{
		super(Messages.getString("mainframe.title"));
		// TODO uncomment after merge into develop branch
//		showSplashScreen();
	}

	protected void showSplashScreen()
	{
		SplashScreenModelBean splashScreenModelBean = SplashScreenModelBean.builder()
			.imagePath(getIconPath()).text(getApplicationName()).min(0).max(100).showTime(1200)
			.showing(true).build();
		Model<SplashScreenModelBean> modelBeanModel = BaseModel.of(splashScreenModelBean);
		Thread splashScreenThread = new Thread(() -> {
			new ProgressBarSplashScreen(MysticCryptApplicationFrame.this, modelBeanModel)
			{
				@Override
				protected void onBeforeInitialize()
				{
					super.onBeforeInitialize();
					MysticCryptApplicationFrame.this.setVisible(false);
				}
			};
		});
		splashScreenThread.start();
	}

	protected String getApplicationName()
	{
		String applicationName = Messages.getString("mainframe.project.name");
		if (applicationName == null)
		{
			applicationName = MysticCryptApplicationFrame.APPLICATION_NAME;
		}
		return applicationName;
	}

	protected String getIconPath()
	{
		String iconPath = Messages.getString("global.icon.app.path");
		if (iconPath == null)
			iconPath = "img/icon.png";
		return iconPath;
	}

	@Override
	protected void onBeforeInitialize()
	{
		if (instance == null)
		{
			instance = this;
		}
		// initialize model and model object
		setModel(BaseModel.of(ApplicationModelBean.builder().build()));
		super.onBeforeInitialize();
	}

	@Override
	protected void onBeforeInitializeComponents()
	{
		// start
		// TODO delete when app-file created
		// generateTempApplicationModelFileWithPassword();
		// generateTempApplicationModelFileWithKeyFile();
		// generateTempApplicationModelFileWithPasswordProtectedKeyFile();
		// TODO delete when app-file created
		// end
		// TODO FIXME uncomment if 'create new master key is finished::::!!!!:::::
		showMasterPwDialog();
		// TODO FIXME uncomment if 'create new master key is finished::::!!!!:::::
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
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		File tempJsonFile = new File(appConfigDir, "app-data-only-pw.json");

		cryptModel = CryptModel.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).build();
		encryptor = RuntimeExceptionDecorator.decorate(() -> new PBEFileEncryptor(cryptModel));
		String json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(modelObject));
		RuntimeExceptionDecorator.decorate(() -> WriteFileExtensions.string2File(tempJsonFile, json));
		File encryptedAppData = RuntimeExceptionDecorator
			.decorate(() -> encryptor.encrypt(tempJsonFile));
		System.out.println(encryptedAppData.getAbsolutePath());
	}

	private void generateTempApplicationModelFileWithPasswordProtectedKeyFile()
	{
		PrivateKey privateKey;
		File pwProtectedPrivateKeyFile;
		String password;
		PublicKey publicKey;
		SecretKey symmetricKey;
		PublicKeyEncryptor encryptor;
		PublicKeyGenericEncryptor<String> genericEncryptor;
		CryptModel<Cipher, PublicKey, byte[]> encryptModel;
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel;

		File appConfigDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		File applicationFile = new File(appConfigDir, "app-data-with-pw-protected-key.json");
		File encryptedAppDataFile = new File(appConfigDir, "app-data-with-pw-protected-key.enc");

		ApplicationModelBean modelObject = getModelObject();
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.applicationFile(applicationFile)
			.build();
		modelObject.setMasterPwFileModelBean(masterPwFileModelBean);

		pwProtectedPrivateKeyFile = new File(appConfigDir, "pwp-private-key-pw-is-secret.der");
		password = "secret";

		privateKey = RuntimeExceptionDecorator.decorate(() -> EncryptedPrivateKeyReader
			.readPasswordProtectedPrivateKey(pwProtectedPrivateKeyFile, password));
		publicKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyExtensions.generatePublicKey(privateKey));
		encryptModel = CryptModel.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();
		symmetricKey = RuntimeExceptionDecorator.decorate(
			() -> SecretKeyFactoryExtensions.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128));
		symmetricKeyModel = CryptModel.<Cipher, SecretKey, String> builder().key(symmetricKey)
			.algorithm(AesAlgorithm.AES).operationMode(Cipher.ENCRYPT_MODE).build();

		encryptor = RuntimeExceptionDecorator
			.decorate(() -> new PublicKeyEncryptor(encryptModel, symmetricKeyModel));
		genericEncryptor = new PublicKeyGenericEncryptor<>(encryptor);

		String json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(modelObject));
		RuntimeExceptionDecorator
			.decorate(() -> WriteFileExtensions.string2File(applicationFile, json));
		byte[] encrypt = RuntimeExceptionDecorator.decorate(() -> genericEncryptor.encrypt(json));
		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encrypt, encryptedAppDataFile));
		System.out.println(encryptedAppDataFile.getAbsolutePath());
	}

	private void generateTempApplicationModelFileWithKeyFile()
	{
		PrivateKey privateKey;
		File privatekeyDerFile;
		PublicKey publicKey;
		SecretKey symmetricKey;
		PublicKeyEncryptor encryptor;
		PublicKeyGenericEncryptor<String> genericEncryptor;
		CryptModel<Cipher, PublicKey, byte[]> encryptModel;
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel;

		File appConfigDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		File applicationFile = new File(appConfigDir, "app-data-with-key.json");
		File encryptedAppDataFile = new File(appConfigDir, "app-data-with-key.enc");

		ApplicationModelBean modelObject = getModelObject();
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.applicationFile(applicationFile)
			.build();
		modelObject.setMasterPwFileModelBean(masterPwFileModelBean);

		privatekeyDerFile = new File(appConfigDir, "private.der");

		privateKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyReader.readPrivateKey(privatekeyDerFile));
		publicKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyExtensions.generatePublicKey(privateKey));
		encryptModel = CryptModel.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();
		symmetricKey = RuntimeExceptionDecorator.decorate(
			() -> SecretKeyFactoryExtensions.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128));
		symmetricKeyModel = CryptModel.<Cipher, SecretKey, String> builder().key(symmetricKey)
			.algorithm(AesAlgorithm.AES).operationMode(Cipher.ENCRYPT_MODE).build();

		encryptor = RuntimeExceptionDecorator
			.decorate(() -> new PublicKeyEncryptor(encryptModel, symmetricKeyModel));
		genericEncryptor = new PublicKeyGenericEncryptor<>(encryptor);
		String json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(modelObject));
		RuntimeExceptionDecorator
			.decorate(() -> WriteFileExtensions.string2File(applicationFile, json));
		byte[] encrypt = RuntimeExceptionDecorator.decorate(() -> genericEncryptor.encrypt(json));
		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encrypt, encryptedAppDataFile));
		System.out.println(encryptedAppDataFile.getAbsolutePath());
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
				MysticCryptApplicationFrame.getInstance().getMainComponent(), consoleInternalFrame);
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
		String configurationDirectoryName = "mystic-crypt-ui";
		File applicationConfigurationDirectory = new File(
			super.newConfigurationDirectory(parent, child), configurationDirectoryName);
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
		getConsoleOutput();
		setTitle(Messages.getString("mainframe.title"));
		setDefaultLookAndFeel(LookAndFeels.NIMBUS, this);
	}


	/**
	 * Sets the given {@link LookAndFeels} to the {@link UIManager}
	 *
	 * @param lookAndFeels
	 *            the look and feels
	 * @exception ClassNotFoundException
	 *                if the <code>LookAndFeel</code> class could not be found
	 * @exception InstantiationException
	 *                if a new instance of the class couldn't be created
	 * @exception IllegalAccessException
	 *                if the class or initializer isn't accessible
	 * @exception UnsupportedLookAndFeelException
	 *                if <code>lnf.isSupportedLookAndFeel()</code> is false
	 * @throws ClassCastException
	 *             if {@code className} does not identify a class that extends {@code LookAndFeel}
	 */
	public static void setLookAndFeel(final @NonNull LookAndFeels lookAndFeels)
		throws ClassNotFoundException, InstantiationException, IllegalAccessException,
		UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel(lookAndFeels.getLookAndFeelName());
	}

	@Override
	protected JToolBar newJToolBar()
	{
		JToolBar toolBar = super.newJToolBar();
		toolBar.setSize(this.getWidth(), 25);

		ImageIcon applicationAdd = ImageIconFactory
			.newImageIcon("io/github/astrapi69/silk/icons/application_add.png");
		JButton btnApplicationAdd = IconButtonFactory.newIconButton(applicationAdd,
			"New application");
		btnApplicationAdd.addActionListener(this::showNewMasterPw);
		toolBar.add(btnApplicationAdd);

		ImageIcon folderEdit = ImageIconFactory
			.newImageIcon("io/github/astrapi69/silk/icons/folder_edit.png");
		JButton btnFolderEdit = IconButtonFactory.newIconButton(folderEdit, "Open application");
		toolBar.add(btnFolderEdit);

		ImageIcon disk = ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/disk.png");
		JButton btnDisk = IconButtonFactory.newIconButton(disk, "Save");
		toolBar.add(btnDisk);

		ImageIcon magnifier = ImageIconFactory
			.newImageIcon("io/github/astrapi69/silk/icons/magnifier.png");
		JButton btnMagnifier = IconButtonFactory.newIconButton(magnifier, "Search");
		toolBar.add(btnMagnifier);

		ImageIcon lock = ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/lock.png");
		JButton btnLock = IconButtonFactory.newIconButton(lock, "Lock workspace");
		toolBar.add(btnLock);

		return toolBar;
	}

	protected void showNewMasterPw(final ActionEvent actionEvent)
	{

		File configDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		JFileChooser fileChooser = new JFileChooser(configDir);

		fileChooser.setDialogTitle("Specify the database file to save");

		final int returnVal = fileChooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedApplicationFile = fileChooser.getSelectedFile();
			String selectedApplicationFilePath = selectedApplicationFile.getAbsolutePath();
			Model<MasterPwFileModelBean> model = BaseModel.<MasterPwFileModelBean>of(
				MasterPwFileModelBean.builder()
					.applicationFile(selectedApplicationFile)
					.selectedApplicationFilePath(selectedApplicationFilePath)
					.minPasswordLength(6).withKeyFile(false)
					.withMasterPw(false).showMasterPw(false).build());
			NewMasterPwFileDialog dialog = new NewMasterPwFileDialog(this,
				"Create new application file with credentials", true,
				model);
			dialog.setSize(840, 520);
			dialog.setVisible(true);
		} else if (returnVal == JFileChooser.CANCEL_OPTION) {
			System.err.println("Cancel was selected");
		}

	}
}
