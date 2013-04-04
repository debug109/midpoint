/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.model.util.mock.MockClockworkHook;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class AbstractInternalModelIntegrationTest extends AbstractModelIntegrationTest {
	
	protected static final String CONNECTOR_DUMMY_FILENAME = COMMON_DIR_NAME + "/connector-dummy.xml";
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_NAME + "/system-configuration.xml";
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final String USER_ADMINISTRATOR_FILENAME = COMMON_DIR_NAME + "/user-administrator.xml";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	
	protected static final String USER_JACK_FILENAME = COMMON_DIR_NAME + "/user-jack.xml";
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";
	
	protected static final String USER_BARBOSSA_FILENAME = COMMON_DIR_NAME + "/user-barbossa.xml";
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	
	protected static final String USER_GUYBRUSH_FILENAME = COMMON_DIR_NAME + "/user-guybrush.xml";
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	
	static final String USER_ELAINE_FILENAME = COMMON_DIR_NAME + "/user-elaine.xml";
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	// Largo does not have a full name set, employeeType=PIRATE
	protected static final String USER_LARGO_FILENAME = COMMON_DIR_NAME + "/user-largo.xml";
	protected static final String USER_LARGO_OID = "c0c010c0-d34d-b33f-f00d-111111111118";
	
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-hbarbossa-dummy.xml";
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-222211111112";
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_USERNAME = "hbarbossa";
	
	public static final String ACCOUNT_SHADOW_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-jack-dummy.xml";
	public static final String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
	
	public static final String ACCOUNT_HERMAN_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-herman-dummy.xml";
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";
	
//	public static final String ACCOUNT_HERMAN_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-herman-opendj.xml";
//	public static final String ACCOUNT_HERMAN_OPENDJ_OID = "22220000-2200-0000-0000-333300003333";
	
	public static final String ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-guybrush-dummy.xml";
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-guybrush-dummy.xml";
	
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-22220004000e";
	public static final String ACCOUNT_ELAINE_DUMMY_USERNAME = USER_ELAINE_USERNAME;
	
	public static final String RESOURCE_DUMMY_FILENAME = COMMON_DIR_NAME + "/resource-dummy.xml";
	public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	public static final String RESOURCE_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004";
	
	public static final String USER_TEMPLATE_FILENAME = COMMON_DIR_NAME + "/user-template.xml";
	public static final String USER_TEMPLATE_OID = "10000000-0000-0000-0000-000000000002";
	
	protected static final String PASSWORD_POLICY_GLOBAL_FILENAME = COMMON_DIR_NAME + "/password-policy-global.xml";
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";
	
	protected static final String MOCK_CLOCKWORK_HOOK_URL = MidPointConstants.NS_MIDPOINT_TEST_PREFIX + "/mockClockworkHook";
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

	protected PrismObject<UserType> userAdministrator;
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected UserType userTypeElaine;
		
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	
	protected MockClockworkHook mockClockworkHook;
			
	public AbstractInternalModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		mockClockworkHook = new MockClockworkHook();
		hookRegistry.registerChangeHook(MOCK_CLOCKWORK_HOOK_URL, mockClockworkHook);
		
		// System Configuration
		try {
			addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
				
		// Administrator
		userAdministrator = addObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);
		
		// User Templates
		addObjectFromFile(USER_TEMPLATE_FILENAME, UserTemplateType.class, initResult);

		// Connectors
		addObjectFromFile(CONNECTOR_DUMMY_FILENAME, ConnectorType.class, initResult);
		
		// Resources
		
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILENAME, RESOURCE_DUMMY_OID, initTask, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		
		dummyResourceCtl = DummyResourceContoller.create(null, resourceDummy);
		dummyResourceCtl.extendDummySchema();
		dummyResource = dummyResourceCtl.getDummyResource();
		
		dummyResourceCtl.addAccount(ACCOUNT_HBARBOSSA_DUMMY_USERNAME, "Hector Barbossa", "Caribbean");
		dummyResourceCtl.addAccount(ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", "Monkey Island");
		dummyResourceCtl.addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Melee Island");
				
		// Accounts
		addObjectFromFile(ACCOUNT_HBARBOSSA_DUMMY_FILENAME, ShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME, ShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME, ShadowType.class, initResult);
		
		// Users
		userTypeJack = addObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = addObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		userTypeGuybrush = addObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, initResult).asObjectable();
		userTypeElaine = addObjectFromFile(USER_ELAINE_FILENAME, UserType.class, initResult).asObjectable();
				
	}
	
	protected LensContext<UserType, ShadowType> createUserAccountContext() {
		return new LensContext<UserType, ShadowType>(UserType.class, ShadowType.class, prismContext);
	}
	
	protected LensFocusContext<UserType> fillContextWithUser(LensContext<UserType, ShadowType> context, PrismObject<UserType> user) throws SchemaException, ObjectNotFoundException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setObjectOld(user);
		return focusContext;
	}
	
	protected LensFocusContext<UserType> fillContextWithUser(LensContext<UserType, ShadowType> context, String userOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
        return fillContextWithUser(context, user);
    }
	
	protected void fillContextWithUserFromFile(LensContext<UserType, ShadowType> context, String filename) throws SchemaException,
	ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(filename));
		fillContextWithUser(context, user);
	}
	
	protected void fillContextWithEmtptyAddUserDelta(LensContext<UserType, ShadowType> context, OperationResult result) throws SchemaException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyAddDelta(UserType.class, null, prismContext);
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setPrimaryDelta(userDelta);
	}
	
	protected void fillContextWithAddUserDelta(LensContext<UserType, ShadowType> context, PrismObject<UserType> user) throws SchemaException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setPrimaryDelta(userDelta);
	}

	protected LensProjectionContext<ShadowType> fillContextWithAccount(LensContext<UserType, ShadowType> context, String accountOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<ShadowType> account = repositoryService.getObject(ShadowType.class, accountOid, result);
        provisioningService.applyDefinition(account, result);
        return fillContextWithAccount(context, account, result);
	}

	protected LensProjectionContext<ShadowType> fillContextWithAccountFromFile(LensContext<UserType, ShadowType> context, String filename, OperationResult result) throws SchemaException,
	ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(filename));
		provisioningService.applyDefinition(account, result);
		return fillContextWithAccount(context, account, result);
	}

    protected LensProjectionContext<ShadowType> fillContextWithAccount(LensContext<UserType, ShadowType> context, PrismObject<ShadowType> account, OperationResult result) throws SchemaException,
		ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	ShadowType accountType = account.asObjectable();
        String resourceOid = accountType.getResourceRef().getOid();
        ResourceType resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
        applyResourceSchema(accountType, resourceType);
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, ResourceObjectShadowUtil.getIntent(accountType));
        LensProjectionContext<ShadowType> accountSyncContext = context.findOrCreateProjectionContext(rat);
        accountSyncContext.setOid(account.getOid());
		accountSyncContext.setObjectOld(account);
		accountSyncContext.setResource(resourceType);
		context.rememberResource(resourceType);
		return accountSyncContext;
    }

	protected ObjectDelta<UserType> addModificationToContext(
			LensContext<UserType, ShadowType> context, String filename)
			throws JAXBException, SchemaException, FileNotFoundException {
		ObjectModificationType modElement = PrismTestUtil.unmarshalObject(
				new File(filename), ObjectModificationType.class);
		ObjectDelta<UserType> userDelta = DeltaConvertor.createObjectDelta(
				modElement, UserType.class, prismContext);
		LensFocusContext<UserType> focusContext = context
				.getOrCreateFocusContext();
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(
			LensContext<UserType, ShadowType> context, QName propertyName, Object... propertyValues)
			throws SchemaException {
		return addModificationToContextReplaceUserProperty(context, new ItemPath(propertyName), propertyValues);
	}

	protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(
			LensContext<UserType, ShadowType> context, ItemPath propertyPath, Object... propertyValues)
			throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, focusContext
				.getObjectOld().getOid(), propertyPath, prismContext, propertyValues);
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<UserType> addModificationToContextAddAccountFromFile(
			LensContext<UserType, ShadowType> context, String filename) throws JAXBException, SchemaException,
			FileNotFoundException {
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(filename));
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddReference(UserType.class, focusContext
				.getObjectOld().getOid(), UserType.F_ACCOUNT_REF, prismContext, account);
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<ShadowType> addModificationToContextDeleteAccount(
			LensContext<UserType, ShadowType> context, String accountOid) throws SchemaException,
			FileNotFoundException {
		LensProjectionContext<ShadowType> accountCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<ShadowType> deleteAccountDelta = ObjectDelta.createDeleteDelta(ShadowType.class,
				accountOid, prismContext);
		accountCtx.addPrimaryDelta(deleteAccountDelta);
		return deleteAccountDelta;
	}

	protected <T> ObjectDelta<ShadowType> addModificationToContextReplaceAccountAttribute(
			LensContext<UserType, ShadowType> context, String accountOid, String attributeLocalName,
			T... propertyValues) throws SchemaException {
		LensProjectionContext<ShadowType> accCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<ShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName,
				propertyValues);
		accCtx.addPrimaryDelta(accountDelta);
		return accountDelta;
	}

	protected <T> ObjectDelta<ShadowType> addSyncModificationToContextReplaceAccountAttribute(
			LensContext<UserType, ShadowType> context, String accountOid, String attributeLocalName,
			T... propertyValues) throws SchemaException {
		LensProjectionContext<ShadowType> accCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<ShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName,
				propertyValues);
		accCtx.addAccountSyncDelta(accountDelta);
		return accountDelta;
	}
	
	
	protected <T> ObjectDelta<ShadowType> createAccountDelta(LensProjectionContext<ShadowType> accCtx, String accountOid, 
			String attributeLocalName, T... propertyValues) throws SchemaException {
		ResourceType resourceType = accCtx.getResource();
		QName attrQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeLocalName);
		ItemPath attrPath = new ItemPath(ShadowType.F_ATTRIBUTES, attrQName);
		RefinedObjectClassDefinition refinedAccountDefinition = accCtx.getRefinedAccountDefinition();
		RefinedAttributeDefinition attrDef = refinedAccountDefinition.findAttributeDefinition(attrQName);
		assertNotNull("No definition of attribute "+attrQName+" in account def "+refinedAccountDefinition, attrDef);
		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
		PropertyDelta<T> attrDelta = new PropertyDelta<T>(attrPath, attrDef);
		attrDelta.setValuesToReplace(PrismPropertyValue.createCollection(propertyValues));
		accountDelta.addModification(attrDelta);
		return accountDelta;
	}	
	
	protected void assertUserModificationSanity(LensContext<UserType, ShadowType> context) throws JAXBException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
	    PrismObject<UserType> userOld = focusContext.getObjectOld();
	    if (userOld == null) {
	    	return;
	    }
	    ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
	    if (userPrimaryDelta != null) {
		    assertEquals("No OID in userOld", userOld.getOid(), userPrimaryDelta.getOid());
		    assertEquals(ChangeType.MODIFY, userPrimaryDelta.getChangeType());
		    assertNull(userPrimaryDelta.getObjectToAdd());
		    for (ItemDelta itemMod : userPrimaryDelta.getModifications()) {
		        if (itemMod.getValuesToDelete() != null) {
		            Item property = userOld.findItem(itemMod.getPath());
		            assertNotNull("Deleted item " + itemMod.getParentPath() + "/" + itemMod.getName() + " not found in user", property);
		            for (Object valueToDelete : itemMod.getValuesToDelete()) {
		                if (!property.containsRealValue((PrismValue) valueToDelete)) {
		                    display("Deleted value " + valueToDelete + " is not in user item " + itemMod.getParentPath() + "/" + itemMod.getName());
		                    display("Deleted value", valueToDelete);
		                    display("HASHCODE: " + valueToDelete.hashCode());
		                    for (Object value : property.getValues()) {
		                        display("Existing value", value);
		                        display("EQUALS: " + valueToDelete.equals(value));
		                        display("HASHCODE: " + value.hashCode());
		                    }
		                    AssertJUnit.fail("Deleted value " + valueToDelete + " is not in user item " + itemMod.getParentPath() + "/" + itemMod.getName());
		                }
		            }
		        }
		
		    }
	    }
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected void breakAssignmentDelta(LensContext<UserType, ShadowType> context) throws SchemaException {
        LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        breakAssignmentDelta(userPrimaryDelta);		
	}
	
	protected void makeImportSyncDelta(LensProjectionContext<ShadowType> accContext) {
    	PrismObject<ShadowType> syncAccountToAdd = accContext.getObjectOld().clone();
    	ObjectDelta<ShadowType> syncDelta = ObjectDelta.createAddDelta(syncAccountToAdd);
    	accContext.setSyncDelta(syncDelta);
    }
	
	protected void assertNoUserPrimaryDelta(LensContext<UserType, ShadowType> context) {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		if (userPrimaryDelta == null) {
			return;
		}
		assertTrue("User primary delta is not empty", userPrimaryDelta.isEmpty());
	}

	protected void assertUserPrimaryDelta(LensContext<UserType, ShadowType> context) {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		assertNotNull("User primary delta is null", userPrimaryDelta);
		assertFalse("User primary delta is empty", userPrimaryDelta.isEmpty());
	}
	
	protected void assertNoUserSecondaryDelta(LensContext<UserType, ShadowType> context) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		if (userSecondaryDelta == null) {
			return;
		}
		assertTrue("User secondary delta is not empty", userSecondaryDelta.isEmpty());
	}

	protected void assertUserSecondaryDelta(LensContext<UserType, ShadowType> context) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertNotNull("User secondary delta is null", userSecondaryDelta);
		assertFalse("User secondary delta is empty", userSecondaryDelta.isEmpty());
	}
	     	
}
