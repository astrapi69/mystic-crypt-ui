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
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.Serial;
import java.security.Security;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.JMenuBar;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import io.github.astrapi69.awt.window.adapter.CloseWindow;
import io.github.astrapi69.file.create.DirectoryFactory;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.icon.ImageIconFactory;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.OpenDatabaseTreeFrameAction;
import io.github.astrapi69.mystic.crypt.action.SaveApplicationFileAction;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.menu.MenuLayoutSupport;
import io.github.astrapi69.mystic.crypt.panel.search.SearchToolbarPanel;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MemoizedSigninModelBean;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.mystic.crypt.settings.GeneralSettingsPanel;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.mystic.crypt.ui.screen.ScreenPlacement;
import io.github.astrapi69.swing.base.ApplicationPanelFrame;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.button.builder.JButtonInfo;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.model.component.JMTextField;
import io.github.astrapi69.swing.panel.desktoppane.JDesktopPanePanel;
import io.github.astrapi69.swing.panel.label.LabelPanel;
import io.github.astrapi69.swing.plaf.LookAndFeels;
import io.github.astrapi69.swing.splashscreen.ProgressBarSplashScreen;
import io.github.astrapi69.swing.splashscreen.SplashScreenModelBean;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;

/**
 * The class {@link MysticCryptApplicationFrame}
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Log
public class MysticCryptApplicationFrame extends ApplicationPanelFrame<ApplicationModelBean>
{
	public static final String MEMOIZED_SIGNIN_JSON_FILENAME = "memoizedSignin.json";
	public static final String APPLICATION_NAME = "mystic-crypt-ui";

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * The instance.
	 */
	private static MysticCryptApplicationFrame instance;

	/**
	 * The {@link BouncyCastleProvider} object
	 */
	BouncyCastleProvider bouncyCastleProvider;

	LongIdGenerator idGenerator;
	JDesktopPanePanel<ApplicationModelBean> desktopPanePanel;

	ApplicationPanel applicationPanel;

	FrameMode frameMode;

	PluginManager pluginManager;

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
			// LongIdGenerator.getNextId() returns its seed value on the first call
			// (AtomicLong.getAndIncrement()), so seed with lastId + 1 to avoid reissuing
			// the id that was already used for the last created node
			this.idGenerator = lastId != null
				? LongIdGenerator.of(lastId + 1)
				: LongIdGenerator.of(0L);
		}
		return this.idGenerator;
	}

	public void setIdGenerator(@NonNull final LongIdGenerator idGenerator)
	{
		this.idGenerator = idGenerator;
	}

	/**
	 * The screen the sign-in dialog was shown on, which is the screen this window opens on. Null
	 * until the dialog has been shown.
	 */
	private transient java.awt.GraphicsConfiguration signinScreen;

	protected void showMasterPwDialog()
	{
		File configurationDirectory = getConfigurationDirectory();
		File memoizedSigninFile = new File(configurationDirectory, MEMOIZED_SIGNIN_JSON_FILENAME);
		final MemoizedSigninModelBean memoizedSigninModelBean;
		if (memoizedSigninFile.exists())
		{
			String fromFile = RuntimeExceptionDecorator
				.decorate(() -> ReadFileExtensions.fromFile(memoizedSigninFile));
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
		RuntimeExceptionDecorator
			.decorate(() -> LookAndFeels.setLookAndFeel(LookAndFeels.NIMBUS, dialog));
		dialog.setSize(920, 380);
		dialog.setVisible(true);
		signinScreen = dialog.getGraphicsConfiguration();
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
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		setModel(BaseModel.of(applicationModelBean));
		// sets the configuration directory that the plugins directory is nested under
		super.onBeforeInitialize();
		File pluginsDirectory = DirectoryFactory.newDirectory(getConfigurationDirectory(),
			"plugins");
		pluginManager = new DefaultPluginManager(pluginsDirectory.toPath());
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
		return DirectoryFactory.newDirectory(super.newConfigurationDirectory(parent, child),
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
		desktopPanePanel = (JDesktopPanePanel<ApplicationModelBean>)getMainComponent();
		frameMode = FrameMode.DESKTOP_PANE;
		// best-practice menu layout: consolidate Look and Feel + View Mode under a "View" menu
		((DesktopMenu)getMenu()).reorganizeMenus();
		// start and load all plugins of application
		pluginManager.loadPlugins();
		pluginManager.startPlugins();
		((DesktopMenu)getMenu())
			.addPluginsMenu(pluginManager.getExtensions(PluginMenuContribution.class));
		// a user defined menu layout, if there is one, wins over the programmatic menu - applied
		// last so it can also rearrange the contributed plugin items
		applyPersistedMenuLayout();
		setTitle(Messages.getString("mainframe.title"));
		setDefaultLookAndFeel(LookAndFeels.NIMBUS, this);
		// apply a persisted look-and-feel choice on top of the Nimbus default
		GeneralSettingsPanel.applyLookAndFeel(
			MysticCryptSettings.load(getConfigurationDirectory()).getLookAndFeel());
		// the sign-in dialog has already been on screen at this point, and the screen it was on is
		// the one the user is sitting in front of - filling the first screen instead is how the
		// application used to open somewhere else entirely on a desk with two monitors
		ScreenPlacement.fillScreen(this, signinScreen);
		onEnableMenu();
		onWindowClosing();
	}

	/**
	 * Replaces the menu bar with the one described by the persisted menu layout, when the
	 * configuration directory holds one. Without a layout file, or when it cannot be applied, the
	 * programmatically built menu stays in place
	 */
	public void applyPersistedMenuLayout()
	{
		JMenuBar currentMenuBar = currentMenuBar();
		JMenuBar menuBar = MenuLayoutSupport.applyPersistedLayout(currentMenuBar,
			getConfigurationDirectory());
		if (menuBar != currentMenuBar)
		{
			setJMenuBar(menuBar);
			menuBar.revalidate();
			menuBar.repaint();
		}
	}

	/**
	 * Exports the menu bar that is currently in place as menu xml
	 *
	 * @return the current menu as xml
	 */
	public String exportCurrentMenuXml()
	{
		return MenuLayoutSupport.exportXml(currentMenuBar());
	}

	/**
	 * Replaces the menu bar with the one described by the given menu xml. Every item keeps the
	 * action it currently has; items with an unknown action id are built but disabled
	 *
	 * @param xml
	 *            the menu xml describing the wanted layout
	 */
	public void applyMenuXml(final @NonNull String xml)
	{
		JMenuBar menuBar = MenuLayoutSupport.build(xml, currentMenuBar());
		setJMenuBar(menuBar);
		menuBar.revalidate();
		menuBar.repaint();
	}

	/**
	 * Saves the given menu xml as the layout that is applied on the next start
	 *
	 * @param xml
	 *            the menu xml
	 * @return the file the layout was written to
	 * @throws java.io.IOException
	 *             if writing fails
	 */
	public java.nio.file.Path saveMenuLayout(final @NonNull String xml) throws java.io.IOException
	{
		return MenuLayoutSupport.save(xml, getConfigurationDirectory());
	}

	/**
	 * Removes a persisted menu layout, so the next start builds the standard menu again
	 *
	 * @return true if a layout file was removed
	 * @throws java.io.IOException
	 *             if deleting fails
	 */
	public boolean resetMenuLayout() throws java.io.IOException
	{
		return java.nio.file.Files
			.deleteIfExists(MenuLayoutSupport.layoutFile(getConfigurationDirectory()));
	}

	private JMenuBar currentMenuBar()
	{
		JMenuBar menuBar = getJMenuBar();
		return menuBar != null ? menuBar : ((DesktopMenu)getMenu()).getMenubar();
	}

	/**
	 * Rebuilds the "Plugins" menu from the currently resolved plugin contributions - call after
	 * enabling, disabling or installing plugins so the menu reflects the change
	 */
	public void refreshPluginsMenu()
	{
		((DesktopMenu)getMenu())
			.addPluginsMenu(pluginManager.getExtensions(PluginMenuContribution.class));
		// a persisted arrangement of the plugin entries applies here too - enabling or installing a
		// plugin rebuilds the "Plugins" menu from scratch, and without this the rebuilt menu falls
		// back to the alphabetical default and silently drops what the user had arranged
		applyPersistedMenuLayout();
	}

	public void onEnableMenu()
	{
		DesktopMenu menu = (DesktopMenu)getMenu();
		if (getModelObject().isSignedIn())
		{
			menu.onEnableBySignin();
			applicationPanel = new ApplicationPanel(getModel());
			FrameMode remembered = MysticCryptSettings.load(getConfigurationDirectory())
				.getViewMode();
			applyViewMode(remembered);
			if (FrameMode.DESKTOP_PANE.equals(remembered))
			{
				// the desktop view starts empty, and an empty desktop with the database nowhere in
				// sight is not what someone who chose that view asked for
				OpenDatabaseTreeFrameAction.openDatabaseTreeFrame();
			}
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
			.icon(ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/disk.png"))
			.toolTipText("Save").actionListener(new SaveApplicationFileAction("Save"))
			.name(MenuId.SAVE_APPLICATION_FILE_TOOL_BAR.propertiesKey()).build().toJButton());
		SearchToolbarPanel searchPanel = new SearchToolbarPanel();
		JMTextField searchField = searchPanel.getTxtToolbarSearch();
		// named with the toolbar search id so the menu's enable walks switch it with the rest;
		// disabled until those walks run, because there is nothing to search before signing in.
		// The panel goes onto the toolbar, the field goes into the registry the walks read - a
		// disabled panel would not disable the field inside it.
		searchField.setName(MenuId.SEARCH_TOOL_BAR.propertiesKey());
		searchField.setEnabled(false);
		toolBar.add(searchPanel);
		toolBar.getToolbarItems().add(searchField);
		// JButton lockWorkspace = JButtonInfo.builder()
		// .icon(ImageIconFactory.newImageIcon("io/github/astrapi69/silk/icons/lock.png"))
		// .toolTipText("Lock workspace").actionListener(new LockWorkspaceAction("Lock workspace"))
		// .name(MenuId.LOCK_WORKSPACE_TOOL_BAR.propertiesKey()).build().toJButton();
		// lockWorkspace.setEnabled(false);
		// toolBar.add(lockWorkspace);

		return toolBar;
	}

	@Override
	protected BasePanel<ApplicationModelBean> newMainComponent()
	{
		JDesktopPanePanel<ApplicationModelBean> desktopPanePanel = new JDesktopPanePanel<>();
		return desktopPanePanel;
	}

	/**
	 * Puts this frame into the given view.
	 * <p>
	 * Switching to the panel view before signing in is refused rather than attempted: the
	 * application panel only exists once a database is open, and the library refuses a null main
	 * component. The plugins keep calling {@link #switchToDesktopPane()} directly for the window
	 * they are about to open - that is a temporary move and must not become the remembered view.
	 *
	 * @param viewMode
	 *            the view to put the frame into; null is the panel view
	 */
	public void applyViewMode(final FrameMode viewMode)
	{
		if (FrameMode.DESKTOP_PANE.equals(viewMode))
		{
			switchToDesktopPane();
			return;
		}
		if (getApplicationPanel() != null)
		{
			switchToApplicationPanel();
		}
	}

	public void switchToDesktopPane()
	{
		replaceMainComponent(getDesktopPanePanel());
		instance.frameMode = FrameMode.DESKTOP_PANE;
	}

	public void switchToApplicationPanel()
	{
		replaceMainComponent(getApplicationPanel());
		instance.frameMode = FrameMode.APPLICATION_PANEL;
	}

	/**
	 * Checks if all changes have been stored to the application file
	 */
	protected void onWindowClosing()
	{
		MysticCryptApplicationFrame.this.addWindowListener(new CloseWindow()
		{
			@Override
			public void windowClosing(WindowEvent windowEvent)
			{
				ApplicationModelBean modelObject = MysticCryptApplicationFrame.this
					.getModelObject();
				boolean dirty = modelObject.isDirty();
				if (dirty)
				{
					String defaultMessage = "<html><body>"
						+ "<div>The current database file is modified.</div>"
						+ "<div>Store your changes before finish application</div>"
						+ "</body></html>";
					String confirmMessage = Messages
						.getString("dialog.confirm.save.before.close.message", defaultMessage);
					LabelPanel panel = new LabelPanel(BaseModel.of(confirmMessage));
					int option = JOptionPaneExtensions.getSelectedOption(panel,
						JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION,
						MysticCryptApplicationFrame.this,
						Messages.getString("dialog.confirm.save.before.close.title",
							"Save Database Before Close."),
						null);
					if (option == JOptionPane.YES_OPTION)
					{
						ApplicationXmlFileStoreWorker.storeApplicationFile(modelObject);
					}
				}
				stopPluginsQuietly();
				super.windowClosing(windowEvent);
			}
		});
	}

	/**
	 * Stops all plugins, swallowing and logging any exception a plugin's {@code stop()} throws so a
	 * broken plugin cannot prevent the application window from closing
	 */
	private void stopPluginsQuietly()
	{
		if (pluginManager == null)
		{
			return;
		}
		// stop each plugin individually over a snapshot copy: pf4j's own stopPlugins() iterates the
		// started-plugins list while stopPlugin() removes from it, which throws a
		// ConcurrentModificationException once more than one plugin is started
		for (PluginWrapper startedPlugin : new ArrayList<>(pluginManager.getStartedPlugins()))
		{
			try
			{
				pluginManager.stopPlugin(startedPlugin.getPluginId());
			}
			catch (RuntimeException runtimeException)
			{
				log.log(Level.SEVERE, "Error while stopping plugin " + startedPlugin.getPluginId(),
					runtimeException);
			}
		}
	}
}
