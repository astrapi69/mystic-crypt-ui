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
package io.github.astrapi69.mystic.crypt.panel.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How Enter walks through the matches: to the next one, and to the first again after the last
 */
class SearchToolbarModelBeanTest
{

	@Test
	@DisplayName("enter walks forward and wraps around after the last match")
	void enterWalksForwardAndWrapsAround()
	{
		SearchToolbarModelBean modelBean = new SearchToolbarModelBean();

		assertEquals(1, modelBean.nextMatchIndex(3));
		modelBean.setMatchIndex(1);
		assertEquals(2, modelBean.nextMatchIndex(3));
		modelBean.setMatchIndex(2);
		assertEquals(0, modelBean.nextMatchIndex(3), "after the last match comes the first again");
	}

	@Test
	@DisplayName("without matches there is nothing to walk to")
	void withoutMatchesThereIsNothingToWalkTo()
	{
		SearchToolbarModelBean modelBean = new SearchToolbarModelBean();
		modelBean.setMatchIndex(5);

		assertEquals(0, modelBean.nextMatchIndex(0));
		assertEquals(0, modelBean.nextMatchIndex(-1));
	}

	@Test
	@DisplayName("a match index beyond the matches wraps instead of overshooting")
	void aMatchIndexBeyondTheMatchesWraps()
	{
		SearchToolbarModelBean modelBean = new SearchToolbarModelBean();
		modelBean.setMatchIndex(7);

		assertEquals(0, modelBean.nextMatchIndex(4),
			"the walk went past the matches instead of wrapping");
	}

}
