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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.security.Security;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.astrapi69.swing.plaf.LookAndFeels;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.mystic.crypt.panels.dbtree.DatabaseTreePanel;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panels.signin.MemoizedSigninModelBean;
import io.github.astrapi69.mystic.crypt.panels.signin.NewMasterPwFileDialog;
import io.github.astrapi69.swing.base.ApplicationFrame;
import io.github.astrapi69.swing.base.BaseDesktopMenu;
import io.github.astrapi69.swing.button.IconButtonFactory;
import io.github.astrapi69.swing.components.factories.JComponentFactory;
import io.github.astrapi69.swing.icon.ImageIconFactory;
import io.github.astrapi69.swing.panels.output.ConsolePanel;
import io.github.astrapi69.swing.panels.tree.JXTreeElement;
import io.github.astrapi69.swing.splashscreen.ProgressBarSplashScreen;
import io.github.astrapi69.swing.splashscreen.SplashScreenModelBean;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import io.github.astrapi69.tree.TreeNode;

/**
 * The class {@link MysticCryptApplicationFrame}
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysticCryptApplicationFrame extends ApplicationFrame<ApplicationModelBean>
{
	public static final String MEMOIZED_SIGNIN_JSON_FILENAME = "memoizedSignin.json";
	@Getter
	private BouncyCastleProvider bouncyCastleProvider;
	/**
	 * initial block
	 */
	{
		bouncyCastleProvider = new BouncyCastleProvider();
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
		while (!frame.isVisible()) {
			ScreenSizeExtensions.showFrame(frame);
		}
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
		File configurationDirectory = getConfigurationDirectory();
		File memoizedSigninFile = new File(configurationDirectory, MEMOIZED_SIGNIN_JSON_FILENAME);
		final MemoizedSigninModelBean memoizedSigninModelBean;
		if (memoizedSigninFile.exists())
		{
			String fromFile = RuntimeExceptionDecorator
				.decorate(() -> ReadFileExtensions.readFromFile(memoizedSigninFile));
			memoizedSigninModelBean = JsonStringToObjectExtensions.toObject(fromFile,
				MemoizedSigninModelBean.class);

		} else {
			memoizedSigninModelBean = MemoizedSigninModelBean.builder().build();
		}
		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder().minPasswordLength(6)
			.withKeyFile(false).withMasterPw(false).showMasterPw(false).build();
		masterPwFileModelBean.merge(memoizedSigninModelBean);
		Model<MasterPwFileModelBean> model = BaseModel.<MasterPwFileModelBean>of(masterPwFileModelBean);
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
		showSplashScreen();
	}

	protected void showSplashScreen()
	{
		if(getModelObject().isShowSplash()) {
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
		// add once the default provider to the Security class
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			if(getBouncyCastleProvider()==null) {
				bouncyCastleProvider = new BouncyCastleProvider();
			}
			Security.addProvider(bouncyCastleProvider);
		}
		// initialize model and model object
		setModel(BaseModel.of(ApplicationModelBean.builder().build()));
		setDefaultLookAndFeel(LookAndFeels.NIMBUS, this);
		super.onBeforeInitialize();
	}

	@Override
	protected void onBeforeInitializeComponents()
	{
		showMasterPwDialog();
		//Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);
		super.onBeforeInitializeComponents();
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
		String configurationDirectoryName = MysticCryptApplicationFrame.APPLICATION_NAME;
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
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Key database",
			true, true, true, true);
		TreeNode<JXTreeElement> rootTreeNode = getModelObject().getRootTreeNode();
		if (rootTreeNode == null)
		{
			JXTreeElement parent = JXTreeElement.builder().name("root")
				.iconPath("io/github/astrapi69/silk/icons/book.png").withText(true).parent(null)
				.node(true).build();

			JXTreeElement firstChild = JXTreeElement.builder().name("mykeys").parent(parent)
				.iconPath("io/github/astrapi69/silk/icons/folder.png")
				.withText(true)
				.node(true).build();
			rootTreeNode = TreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null);
			TreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild, rootTreeNode);
		}
		final DatabaseTreePanel component = new DatabaseTreePanel(BaseModel.of(rootTreeNode));
		internalFrame.add(component, "Center");
		internalFrame.pack();
		setCurrentVisibleInternalFrame(internalFrame);
		setDefaultLookAndFeel(LookAndFeels.NIMBUS, this);
		this.setSize(ScreenSizeExtensions.getScreenWidth(), ScreenSizeExtensions.getScreenHeight());
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
//
//		ImageIcon folderEdit = ImageIconFactory
//			.newImageIcon("io/github/astrapi69/silk/icons/folder_edit.png");
//		JButton btnFolderEdit = IconButtonFactory.newIconButton(folderEdit, "Open application");
//		toolBar.add(btnFolderEdit);
//
//		ImageIcon disk = ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/disk.png");
//		JButton btnDisk = IconButtonFactory.newIconButton(disk, "Save");
//		toolBar.add(btnDisk);
//
//		ImageIcon magnifier = ImageIconFactory
//			.newImageIcon("io/github/astrapi69/silk/icons/magnifier.png");
//		JButton btnMagnifier = IconButtonFactory.newIconButton(magnifier, "Search");
//		toolBar.add(btnMagnifier);
//
//		ImageIcon lock = ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/lock.png");
//		JButton btnLock = IconButtonFactory.newIconButton(lock, "Lock workspace");
//		toolBar.add(btnLock);

		return toolBar;
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
			Model<MasterPwFileModelBean> model = BaseModel.<MasterPwFileModelBean>of(
				MasterPwFileModelBean.builder()
					.applicationFile(selectedApplicationFile)
					.selectedApplicationFilePath(selectedApplicationFilePath)
					.minPasswordLength(6).withKeyFile(false)
					.withMasterPw(false).showMasterPw(false).build());
			NewMasterPwFileDialog dialog = new NewMasterPwFileDialog(this,
				"Create your master key", true,
				model);
			dialog.setSize(840, 520);
			dialog.setVisible(true);
		} else if (returnVal == JFileChooser.CANCEL_OPTION) {
			System.err.println("Cancel was selected");
		}

	}
}
