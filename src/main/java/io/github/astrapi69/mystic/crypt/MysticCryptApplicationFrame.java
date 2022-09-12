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
import java.io.File;
import java.security.Security;

import javax.swing.JMenu;
import javax.swing.JToolBar;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.icon.ImageIconFactory;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.action.LockWorkspaceAction;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SaveApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.SearchApplicationFileAction;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MemoizedSigninModelBean;
import io.github.astrapi69.swing.base.ApplicationFrame;
import io.github.astrapi69.swing.button.builder.JButtonInfo;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;
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

	/**
	 * The {@link BouncyCastleProvider} object
	 */
	@Getter
	BouncyCastleProvider bouncyCastleProvider;

	LongIdGenerator idGenerator;

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
	 * Gets the single instance of {@link MysticCryptApplicationFrame} object
	 *
	 * @return single instance of {@link MysticCryptApplicationFrame} object
	 */
	public static MysticCryptApplicationFrame getInstance()
	{
		return instance;
	}

	public LongIdGenerator getIdGenerator()
	{
		if (this.idGenerator == null)
		{
			Long lastId = getModelObject().getLastId();
			this.idGenerator = lastId != null ? LongIdGenerator.of(lastId) : LongIdGenerator.of(0L);
		}
		return this.idGenerator;
	}

	protected void showMasterPwDialog()
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
		IModel<MasterPwFileModelBean> model = BaseModel.of(masterPwFileModelBean);
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
				.imagePath(getIconPath())
				.text(Messages.getString("mainframe.project.name",
					MysticCryptApplicationFrame.APPLICATION_NAME))
				.min(0).max(100).showTime(1200).showing(true).build();
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

	protected String getIconPath()
	{
		return Messages.getString("global.icon.app.path", "img/icon.png");
	}

	@Override
	protected void onBeforeInitialize()
	{
		if (instance == null)
		{
			instance = this;
		}
		// add once the default provider to the Security class
		setSecurityProvider();
		// initialize model and model object
		setModel(BaseModel.of(ApplicationModelBean.builder().build()));
		super.onBeforeInitialize();
	}

	private void setSecurityProvider()
	{
		// add once the default provider to the Security class
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			if (getBouncyCastleProvider() == null)
			{
				bouncyCastleProvider = new BouncyCastleProvider();
			}
			Security.addProvider(bouncyCastleProvider);
		}
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
		return FileFactory.newDirectory(super.newConfigurationDirectory(parent, child),
			getApplicationName());
	}

	@Override
	protected String newApplicationName()
	{
		return Messages.getString("mainframe.project.name",
			MysticCryptApplicationFrame.APPLICATION_NAME);
	}

	@Override
	protected JMenu newDesktopMenu(@NonNull Component applicationFrame)
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
		setTitle(Messages.getString("mainframe.title"));
		this.setSize(ScreenSizeExtensions.getScreenWidth(), ScreenSizeExtensions.getScreenHeight());
		DesktopMenu menu = (DesktopMenu)getMenu();
		if (getModelObject().isSignedIn())
		{
			menu.onEnableBySignin();
		}
		else
		{
			menu.onEnableByPublic();
		}
	}

	@Override
	protected JToolBar newJToolBar()
	{
		ApplicationToolbar toolBar = new ApplicationToolbar();
		toolBar.setSize(this.getWidth(), 25);

		toolBar.add(JButtonInfo.builder()
			.icon(
				ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/application_add.png"))
			.toolTipText("New application")
			.actionListener(new NewApplicationFileAction("New Application"))
			.name(MenuId.NEW_DATABASE_TOOL_BAR.propertiesKey()).build().toJButton());

		toolBar.add(JButtonInfo.builder()
			.icon(ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/folder_edit.png"))
			.toolTipText("Open application")
			.actionListener(new NewApplicationFileAction("Open Application"))
			.name(MenuId.OPEN_DATABASE_TOOL_BAR.propertiesKey()).build().toJButton());

		toolBar.add(JButtonInfo.builder()
			.icon(ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/disk.png"))
			.toolTipText("Save").actionListener(new SaveApplicationFileAction("Save"))
			.name(MenuId.SAVE_APPLICATION_FILE_TOOL_BAR.propertiesKey()).build().toJButton());

		toolBar.add(JButtonInfo.builder()
			.icon(ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/magnifier.png"))
			.toolTipText("Search").actionListener(new SearchApplicationFileAction("Search"))
			.name(MenuId.SEARCH_TOOL_BAR.propertiesKey()).build().toJButton());

		toolBar.add(JButtonInfo.builder()
			.icon(ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/lock.png"))
			.toolTipText("Lock workspace").actionListener(new LockWorkspaceAction("Lock workspace"))
			.name(MenuId.LOCK_WORKSPACE_TOOL_BAR.propertiesKey()).build().toJButton());

		return toolBar;
	}
}
