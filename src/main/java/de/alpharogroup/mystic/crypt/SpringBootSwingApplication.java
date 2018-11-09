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
import java.awt.EventQueue;

import javax.swing.JInternalFrame;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import de.alpharogroup.swing.base.ApplicationFrame;
import de.alpharogroup.swing.base.BaseDesktopMenu;
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
public class SpringBootSwingApplication extends ApplicationFrame<ApplicationModelBean>
{

	/**
	 * The main method that start this {@link SpringBootSwingApplication}
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args)
	{

		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(
			SpringBootSwingApplication.class)
			.headless(false).run(args);
		SpringBootSwingApplication.ctx = ctx;

		EventQueue.invokeLater(() -> {
			SpringBootSwingApplication ex = ctx.getBean(SpringBootSwingApplication.class);
			ex.setVisible(true);
		});
	}

	/** The instance. */
	private static SpringBootSwingApplication instance;
	public static ConfigurableApplicationContext ctx;

	/**
	 * Gets the single instance of SpringBootSwingApplication.
	 *
	 * @return single instance of SpringBootSwingApplication
	 */
	public static SpringBootSwingApplication getInstance()
	{
		return instance;
	}

	/** The internal frame. */
	@Getter
	JInternalFrame internalFrame;

	/**
	 * Instantiates a new main frame.
	 */
	public SpringBootSwingApplication()
	{
		super(Messages.getString("mainframe.title"));
		if (instance == null)
		{
			instance = this;
		}
		initComponents();
	}

	@Override
	protected String newIconPath()
	{
		return Messages.getString("global.icon.app.path");
	}
	
	@Override
	protected BaseDesktopMenu newDesktopMenu(@NonNull Component applicationFrame)
	{
		return new DesktopMenu(applicationFrame);
	}

}