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

import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXLoginPane;
import org.jdesktop.swingx.JXLoginPane.Status;
import org.jdesktop.swingx.auth.LoginService;

import de.alpharogroup.lang.ClassExtensions;
import de.alpharogroup.swing.desktoppane.SingletonDesktopPane;
import de.alpharogroup.swing.laf.LookAndFeels;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class MainFrame.
 */
@SuppressWarnings("serial")
@Slf4j
public class MainFrame extends JXFrame
{

	/** The instance. */
	private static MainFrame instance = new MainFrame();

	/**
	 * Gets the single instance of MainFrame.
	 *
	 * @return single instance of MainFrame
	 */
	public static MainFrame getInstance()
	{
		return instance;
	}

	/** The desktop pane. */
	@Getter
	private final JDesktopPane desktopPane = SingletonDesktopPane.getInstance();

	/** The menubar. */
	@Getter
	private JMenuBar menubar;

	/** The toolbar. */
	@Getter
	private JToolBar toolbar;

	/** The internal frame. */
	@Getter
	private JInternalFrame internalFrame;

	/** The current look and feels. */
	@Getter
	@Setter
	private LookAndFeels currentLookAndFeels = LookAndFeels.SYSTEM;

	/**
	 * Instantiates a new main frame.
	 */
	private MainFrame()
	{
		super(Messages.getString("mainframe.title"));
		initComponents();
	}

	/**
	 * Inits the components.
	 */
	private void initComponents()
	{

		toolbar = new JToolBar(); // create the tool bar
		setJMenuBar(menubar);
		setToolBar(toolbar);

		getContentPane().add(desktopPane);
		// https://stackoverflow.com/questions/26145425/login-dialog-window-wont-dispose-completely
//		LoginService loginService = new LoginService()
//		{
//
//			@Override
//			public boolean authenticate(String name, char[] password, String server)
//				throws Exception
//			{
//				System.out.println(name+":"+new String(password));
//				if(name.equals("foo")) {
//					return true;
//				}
//				return false;
//			}
//		};
//		JXLoginPane loginPane = new JXLoginPane(loginService);
//		this.setDefaultCloseOperation(JXFrame.DISPOSE_ON_CLOSE);
//		JXLoginPane.JXLoginDialog dialog = new JXLoginPane.JXLoginDialog(this, loginPane);
//		dialog.setDefaultCloseOperation(JXFrame.DISPOSE_ON_CLOSE);
//		dialog.setVisible(true);
//		Status status = dialog.getStatus();
//		if (!JXLoginPane.Status.SUCCEEDED.equals(status))
//		{
//			MainFrame.this.dispatchEvent(new WindowEvent(MainFrame.this, WindowEvent.WINDOW_CLOSING));
//		}
//		else
//		{
//			setVisible(true);
//		}
		
		try
		{
			String iconPath = Messages.getString("global.icon.app.path");
			BufferedImage appIcon = ImageIO.read(ClassExtensions.getResourceAsStream(iconPath));
			setIconImage(appIcon);
		}
		catch (IOException e)
		{
			log.error(e.getMessage(), e);
		}

	}

}
