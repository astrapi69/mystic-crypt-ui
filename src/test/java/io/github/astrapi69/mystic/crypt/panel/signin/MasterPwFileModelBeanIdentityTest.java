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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The sign-in bean is reachable through the hash code of the dialog that holds it, and whatever
 * hashes a window - the swing hierarchy, a test harness - may do so at any moment, from a thread
 * that is not the one filling the combo boxes.
 * <p>
 * These tests pin that the identity of the bean does not depend on the two lists behind those combo
 * boxes: they are contents that grow while the user works, not what makes this bean this bean.
 */
class MasterPwFileModelBeanIdentityTest
{

	@Test
	@DisplayName("hashCode stays the same when the combo box contents change")
	void hashCodeDoesNotChangeWithTheComboBoxContents()
	{
		MasterPwFileModelBean modelBean = MasterPwFileModelBean.builder().build();
		int before = modelBean.hashCode();

		modelBean.getApplicationFilePaths().add("/somewhere/first.mcrdb");
		modelBean.getKeyFilePaths().add("/somewhere/first.der");

		assertEquals(before, modelBean.hashCode(),
			"the identity of the bean moved when a combo box was filled");
	}

	/**
	 * Hashing the bean while the combo box list grows must not fail. Before the fix this threw a
	 * {@link java.util.ConcurrentModificationException} out of the generated hash code.
	 */
	@Test
	@DisplayName("hashCode survives a list that is being filled at the same time")
	void hashCodeSurvivesAConcurrentlyGrowingList() throws Exception
	{
		MasterPwFileModelBean modelBean = MasterPwFileModelBean.builder()
			.applicationFilePaths(new ArrayList<>(List.of(""))).build();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		CountDownLatch done = new CountDownLatch(1);

		Thread filling = new Thread(() -> {
			for (int index = 0; index < 20_000; index++)
			{
				modelBean.getApplicationFilePaths().add("/somewhere/" + index + ".mcrdb");
			}
			done.countDown();
		});
		filling.start();
		while (done.getCount() > 0)
		{
			try
			{
				modelBean.hashCode();
			}
			catch (Throwable thrown)
			{
				failure.compareAndSet(null, thrown);
				break;
			}
		}
		done.await(30, TimeUnit.SECONDS);
		filling.join();

		assertEquals(null, failure.get(),
			"hashing the bean failed while its combo box list was being filled");
	}

}
