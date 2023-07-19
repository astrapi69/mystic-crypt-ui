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