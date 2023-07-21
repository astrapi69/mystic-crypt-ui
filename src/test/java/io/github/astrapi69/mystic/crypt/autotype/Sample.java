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
package io.github.astrapi69.mystic.crypt.autotype;

import org.kquiet.browser.*;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;

public class Sample
{
	public static void main(String args[]) throws Exception
	{
		try (ActionRunner actionRunner = new BasicActionRunner())
		{
			ActionComposer actionComposer = new ActionComposerBuilder().prepareActionSequence()
				.getUrl("https://github.com/login")
				.waitUntil(elementToBeClickable(By.id("login_field")), 3000)
				.sendKey(By.id("login_field"), "foo")
				.waitUntil(elementToBeClickable(By.id("password")), 3000)
				.sendKey(By.id("password"), "bar")
				.waitUntil(elementToBeClickable(By.cssSelector("input[type='submit']")), 3000)
				.prepareClick(By.cssSelector("input[type='submit']")).done()
				.returnToComposerBuilder().buildBasic().setCloseWindow(false)
				.onFail(ac -> System.out
					.println("called when an exception is thrown or is marked as failed"))
				.onDone(ac -> System.out
					.println("always called after all browser actions and callbacks"));
			actionRunner.executeComposer(actionComposer).get();
			// while (true)
			// {
			// Thread.sleep(1000);
			// }
		}
	}

}