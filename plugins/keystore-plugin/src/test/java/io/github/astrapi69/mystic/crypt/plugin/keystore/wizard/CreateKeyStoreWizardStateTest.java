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
package io.github.astrapi69.mystic.crypt.plugin.keystore.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;

/**
 * Tests of the creation wizard's state machine: which step follows which, and the two places a step
 * refuses to advance until its own step is actually complete - Store without a matching repeated
 * password, Entry without an alias and subject when a key pair was asked for. Built directly on
 * {@link BaseWizardStateMachineModel} without any Swing component, the same way
 * {@code ConversionWizardState}'s own step-gating is provable without a display.
 */
class CreateKeyStoreWizardStateTest
{

	private static final String PATH = "/tmp/new.p12";

	private static BaseWizardStateMachineModel<CreateKeyStoreWizardModel> machineAt(
		CreateKeyStoreWizardState state, CreateKeyStoreWizardModel model)
	{
		return BaseWizardStateMachineModel.<CreateKeyStoreWizardModel> builder().currentState(state)
			.modelObject(model).build();
	}

	@Test
	void everyStateNameMatchesItsCardLayoutName()
	{
		assertEquals("STORE", CreateKeyStoreWizardState.STORE.getName());
		assertEquals("ENTRY", CreateKeyStoreWizardState.ENTRY.getName());
		assertEquals("REVIEW", CreateKeyStoreWizardState.REVIEW.getName());
	}

	@Test
	@DisplayName("Store is first, has no previous step, and does not advance without a file path")
	void storeStaysOnItselfWithoutAFilePath()
	{
		assertTrue(CreateKeyStoreWizardState.STORE.isFirst());
		assertFalse(CreateKeyStoreWizardState.STORE.hasPrevious());
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setStorePassword("secret".toCharArray());
		model.setStorePasswordRepeated("secret".toCharArray());
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.STORE, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.STORE, stateMachine.getCurrentState(),
			"clicking Next with no file path must not advance the wizard");
	}

	@Test
	@DisplayName("Store does not advance without a password")
	void storeStaysOnItselfWithoutAPassword()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setKeyStoreFilePath(PATH);
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.STORE, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.STORE, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Store does not advance when the repeated password does not match")
	void storeStaysOnItselfWithAMismatchedRepeatedPassword()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setKeyStoreFilePath(PATH);
		model.setStorePassword("secret".toCharArray());
		model.setStorePasswordRepeated("typo".toCharArray());
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.STORE, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.STORE, stateMachine.getCurrentState(),
			"a typo in the repeated password must not go unnoticed");
	}

	@Test
	@DisplayName("Store advances to Entry once the path and the matching passwords are set")
	void storeAdvancesToEntryOnceCompletedProperly()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setKeyStoreFilePath(PATH);
		model.setStorePassword("secret".toCharArray());
		model.setStorePasswordRepeated("secret".toCharArray());
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.STORE, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.ENTRY, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Entry advances to Review without any of its own fields when no key pair was asked for")
	void entryAdvancesToReviewWithoutFieldsWhenNoKeyPairIsWanted()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.ENTRY, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.REVIEW, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Entry does not advance when a key pair was asked for but the alias is missing")
	void entryStaysOnItselfWithoutAnAliasWhenAKeyPairIsWanted()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setAddKeyPairNow(true);
		model.setDistinguishedName("CN=example.com");
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.ENTRY, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.ENTRY, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Entry does not advance when a key pair was asked for but the subject is missing")
	void entryStaysOnItselfWithoutADistinguishedNameWhenAKeyPairIsWanted()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setAddKeyPairNow(true);
		model.setAlias("server");
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.ENTRY, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.ENTRY, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Entry advances to Review once alias and subject are set for the requested key pair")
	void entryAdvancesToReviewOnceAliasAndDistinguishedNameAreSet()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setAddKeyPairNow(true);
		model.setAlias("server");
		model.setDistinguishedName("CN=example.com");
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.ENTRY, model);

		stateMachine.next();

		assertEquals(CreateKeyStoreWizardState.REVIEW, stateMachine.getCurrentState());
	}

	@Test
	void entryGoesBackToStore()
	{
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.ENTRY, new CreateKeyStoreWizardModel());

		stateMachine.previous();

		assertEquals(CreateKeyStoreWizardState.STORE, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Review is last, has no next step, and going back returns to Entry")
	void reviewIsLastAndGoesBackToEntry()
	{
		assertTrue(CreateKeyStoreWizardState.REVIEW.isLast());
		assertFalse(CreateKeyStoreWizardState.REVIEW.hasNext());
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = machineAt(
			CreateKeyStoreWizardState.REVIEW, new CreateKeyStoreWizardModel());

		stateMachine.previous();

		assertEquals(CreateKeyStoreWizardState.ENTRY, stateMachine.getCurrentState());
	}

	@Test
	void entryIsNeitherFirstNorLastAndHasBothNeighbours()
	{
		assertFalse(CreateKeyStoreWizardState.ENTRY.isFirst());
		assertFalse(CreateKeyStoreWizardState.ENTRY.isLast());
		assertTrue(CreateKeyStoreWizardState.ENTRY.hasPrevious());
		assertTrue(CreateKeyStoreWizardState.ENTRY.hasNext());
	}
}
