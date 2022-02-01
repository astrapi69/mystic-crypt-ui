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
package io.github.astrapi69.mystic.crypt;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.MenuElement;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.checksum.ObjectChecksumExtensions;
import io.github.astrapi69.crypto.algorithm.Algorithm;
import io.github.astrapi69.io.file.FileExtension;
import io.github.astrapi69.lang.ClassExtensions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.icon.ImageIconFactory;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MemoizedSigninModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.NewMasterPwFileDialog;
import io.github.astrapi69.swing.base.ApplicationFrame;
import io.github.astrapi69.swing.button.IconButtonFactory;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;
import io.github.astrapi69.swing.menu.ParentMenuResolver;
import io.github.astrapi69.swing.splashscreen.ProgressBarSplashScreen;
import io.github.astrapi69.swing.splashscreen.SplashScreenModelBean;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

/**
 * The class {@link MysticCryptApplicationFrame}
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysticCryptApplicationFrame extends ApplicationFrame<ApplicationModelBean>
{
	public static final String MEMOIZED_SIGNIN_JSON_FILENAME = "memoizedSignin.json";
	public static final String APPLICATION_NAME = "mystic-crypt-ui";

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	/**
	 * The instance.
	 */
	private static MysticCryptApplicationFrame instance;
	@Getter
	private BouncyCastleProvider bouncyCastleProvider;

	/**
	 * initial block
	 */
	{
		bouncyCastleProvider = new BouncyCastleProvider();
	}

	/**
	 * Instantiates a new {@link MysticCryptApplicationFrame}
	 */
	public MysticCryptApplicationFrame()
	{
		super(Messages.getString("mainframe.title"));
		showSplashScreen();
	}

	/**
	 * The main method that start this {@link MysticCryptApplicationFrame}
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
		while (!frame.isVisible())
		{
			ScreenSizeExtensions.showFrame(frame);
		}
		 try
		 {
			 File runningJarFile = ClassExtensions.getRunningJarFile(MysticCryptApplicationFrame.class);

			 if(FileExtension.is(runningJarFile, FileExtension.JAR)) {
				 JarFile jarFile = new JarFile(runningJarFile);
				 long checksum = FileChecksumExtensions.getChecksum(runningJarFile, true);
				 // TODO get version and the corresponding checksum
			 }
		 }
		 catch (URISyntaxException e)
		 {
		 e.printStackTrace();
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
	}

	public static File getRunningJarDirectory(Class<?> tClass) throws URISyntaxException
	{
		return new File(tClass.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	/**
	 * Gets the single instance of SpringBootSwingApplication.
	 *
	 * @return single instance of SpringBootSwingApplication
	 */
	public static MysticCryptApplicationFrame getInstance()
	{
		return instance;
	}

	/**
	 * Returns the selected file from the given {@link JFileChooser}, that ends with the first
	 * extension of the {@link FileNameExtensionFilter} from the {@link JFileChooser}
	 *
	 * @param fileChooser
	 *            the file chooser
	 *
	 * @return the file with the first extension of the {@link FileNameExtensionFilter} from the
	 *         {@link JFileChooser}
	 */
	public static File getSelectedFileWithFirstExtension(JFileChooser fileChooser)
	{
		File file = fileChooser.getSelectedFile();
		FileFilter fileFilter = fileChooser.getFileFilter();
		if (fileFilter instanceof FileNameExtensionFilter)
		{
			FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter)fileFilter;
			String[] extensions = fileNameExtensionFilter.getExtensions();
			String fileNameToLowerCase = file.getName().toLowerCase();
			for (String extension : extensions)
			{
				if (fileNameToLowerCase.endsWith('.' + extension.toLowerCase()))
				{
					return file;
				}
			}
			file = new File(file.getAbsolutePath() + '.' + extensions[0]);
		}
		return file;
	}

	private void showMasterPwDialog()
	{
		File configurationDirectory = getConfigurationDirectory();
		File memoizedSigninFile = new File(configurationDirectory, MEMOIZED_SIGNIN_JSON_FILENAME);
		final MemoizedSigninModelBean memoizedSigninModelBean;
		if (memoizedSigninFile.exists())
		{
			String fromFile = RuntimeExceptionDecorator
				.decorate(() -> ReadFileExtensions.readFromFile(memoizedSigninFile));
			memoizedSigninModelBean = JsonStringToObjectExtensions.toObject(fromFile,
				MemoizedSigninModelBean.class);

		}
		else
		{
			memoizedSigninModelBean = MemoizedSigninModelBean.builder().build();
		}
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.minPasswordLength(6).withKeyFile(false).withMasterPw(false).showMasterPw(false)
			.build();
		masterPwFileModelBean.merge(memoizedSigninModelBean);
		IModel<MasterPwFileModelBean> model = BaseModel
			.of(masterPwFileModelBean);
		MasterPwFileDialog dialog = new MasterPwFileDialog(null, "Enter your credentials", true,
			model);
		dialog.setSize(880, 380);
		dialog.setVisible(true);
	}

	protected void showSplashScreen()
	{
		if (getModelObject().isShowSplash())
		{
			SplashScreenModelBean splashScreenModelBean = SplashScreenModelBean.builder()
				.imagePath(getIconPath()).text(getApplicationName()).min(0).max(100).showTime(1200)
				.showing(true).build();
			IModel<SplashScreenModelBean> modelBeanModel = BaseModel.of(splashScreenModelBean);
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
	}

	protected String getApplicationName()
	{
		String applicationName = Messages.getString("mainframe.project.name");
		if (applicationName != null)
		{
			return applicationName;
		}
		return MysticCryptApplicationFrame.APPLICATION_NAME;
	}

	protected String getIconPath()
	{
		String iconPath = Messages.getString("global.icon.app.path");
		if (iconPath != null)
		{
			return iconPath;
		}
		return "img/icon.png";
	}

	@Override
	protected void onBeforeInitialize()
	{
		if (instance == null)
		{
			instance = this;
		}
		// add once the default provider to the Security class
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			if (getBouncyCastleProvider() == null)
			{
				bouncyCastleProvider = new BouncyCastleProvider();
			}
			Security.addProvider(bouncyCastleProvider);
		}
		// initialize model and model object
		setModel(BaseModel.of(ApplicationModelBean.builder().build()));
		super.onBeforeInitialize();
	}

	@Override
	protected void onBeforeInitializeComponents()
	{
		// show login screen dialog ...
		showMasterPwDialog();
		super.onBeforeInitializeComponents();
	}


	@Override
	protected File newConfigurationDirectory(final @NonNull String parent,
		final @NonNull String child)
	{
		File applicationConfigurationDirectory = new File(
			super.newConfigurationDirectory(parent, child), MysticCryptApplicationFrame.APPLICATION_NAME);
		if (!applicationConfigurationDirectory.exists())
		{
			applicationConfigurationDirectory.mkdir();
		}
		return applicationConfigurationDirectory;
	}

	@Override
	protected JMenu newDesktopMenu(@NonNull Component applicationFrame)
	{
		DesktopMenu desktopMenu = new DesktopMenu(applicationFrame);
		JMenuBar menubar = desktopMenu.getMenubar();
		Map<String, Boolean> enabledMenuIdsWithEmptyModel = desktopMenu.getEnabledMenuIdsWithEmptyModel();
		List<MenuElement> allMenuElements = ParentMenuResolver.getAllMenuElements(menubar, true);
		allMenuElements.forEach(menuElement -> {
			String name = menuElement.getComponent().getName();
			if(enabledMenuIdsWithEmptyModel.containsKey(name)){
				menuElement.getComponent().setEnabled(enabledMenuIdsWithEmptyModel.get(name));
			}
		});
		return desktopMenu;
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
		setTitle(Messages.getString("mainframe.title"));
		this.setSize(ScreenSizeExtensions.getScreenWidth(), ScreenSizeExtensions.getScreenHeight());
		JMenuBar menubar = getJMenuBar();
		DesktopMenu menu = (DesktopMenu)getMenu();
		Map<String, Boolean> enabledMenuIdsWithExistingModel = menu
			.getEnabledMenuIdsWithExistingModel();
		List<MenuElement> allMenuElements = ParentMenuResolver.getAllMenuElements(menubar, true);
		allMenuElements.forEach(menuElement -> {
			String name = menuElement.getComponent().getName();
			if (enabledMenuIdsWithExistingModel.containsKey(name))
			{
				menuElement.getComponent().setEnabled(enabledMenuIdsWithExistingModel.get(name));
			}
		});
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
		btnApplicationAdd.setName(MenuId.OPEN_DATABASE_TOOL_BAR.propertiesKey());
		toolBar.add(btnApplicationAdd);

		//
		// ImageIcon folderEdit = ImageIconFactory
		// .newImageIcon("io/github/astrapi69/silk/icons/folder_edit.png");
		// JButton btnFolderEdit = IconButtonFactory.newIconButton(folderEdit, "Open application");
		// toolBar.add(btnFolderEdit);
		//
		// ImageIcon disk =
		// ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/disk.png");
		// JButton btnDisk = IconButtonFactory.newIconButton(disk, "Save");
		// toolBar.add(btnDisk);
		//
		// ImageIcon magnifier = ImageIconFactory
		// .newImageIcon("io/github/astrapi69/silk/icons/magnifier.png");
		// JButton btnMagnifier = IconButtonFactory.newIconButton(magnifier, "Search");
		// toolBar.add(btnMagnifier);
		//
		// ImageIcon lock =
		// ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/lock.png");
		// JButton btnLock = IconButtonFactory.newIconButton(lock, "Lock workspace");
		// toolBar.add(btnLock);

		return toolBar;
	}

	protected void showNewMasterPw(final ActionEvent actionEvent)
	{
		JFileChooser fileChooser = new JFileChooser(getConfigurationDirectory());
		fileChooser.setDialogTitle("Specify the database file to save");
		FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter(
			"Mystic crypt files (*.mcrdb)", "mcrdb");
		fileChooser.setFileFilter(fileNameExtensionFilter);

		final int returnVal = fileChooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedApplicationFile = getSelectedFileWithFirstExtension(fileChooser);
			if (!selectedApplicationFile.exists())
			{
				RuntimeExceptionDecorator
					.decorate(() -> FileFactory.newFile(selectedApplicationFile));
			}
			String selectedApplicationFilePath = selectedApplicationFile.getAbsolutePath();
			IModel<MasterPwFileModelBean> model = BaseModel.of(
				MasterPwFileModelBean.builder().applicationFile(selectedApplicationFile)
					.selectedApplicationFilePath(selectedApplicationFilePath).minPasswordLength(6)
					.withKeyFile(false).withMasterPw(false).showMasterPw(false).build());
			NewMasterPwFileDialog dialog = new NewMasterPwFileDialog(this, "Create your master key",
				true, model);
			dialog.setSize(840, 520);
			dialog.setVisible(true);
		}
		else if (returnVal == JFileChooser.CANCEL_OPTION)
		{
			System.err.println("Cancel was selected");
		}

	}
}
