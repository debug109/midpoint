/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.test.util.ModelServiceUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-provisioning.xml" })
public class AddUserActionTest {

	private static final Trace LOGGER = TraceManager.getTrace(AddUserActionTest.class);
	@Autowired(required = true)
	// private ResourceObjectChangeListener resourceObjectChangeService;
	private ResourceObjectChangeListenerPortType resourceObjectChangeService;
	@Autowired(required = true)
	private RepositoryService repositoryService;

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescriptionType createChangeDescription(String file)
			throws JAXBException {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(file))).getValue();
		return change;
	}

	private ResourceObject createSampleResourceObject(ResourceType resourceType, AccountShadowType accountType)
			throws SchemaProcessorException {
		Schema schema = Schema.parse(resourceType.getSchema().getAny().get(0));
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(accountType.getObjectClass());
		ResourceObject resourceObject = rod.instantiate();

		Set<ResourceObjectAttribute> properties = rod.parseAttributes(accountType.getAttributes().getAny());

		resourceObject.getProperties().addAll(properties);
		return resourceObject;
	}

	@Ignore
	// FIXME: fix test
	@Test
	public void testAddUserAction() throws Exception {

		final String resourceOid = "c0c010c0-d34d-b33f-f00d-333222111111";
		final String userTemplateOid = "c0c010c0-d34d-b55f-f22d-777666111111";
		final String accountOid = "c0c010c0-d34d-b44f-f11d-333222111111";

		UserType addedUser = null;

		try {
			// create additional change
			ResourceObjectShadowChangeDescriptionType change = createChangeDescription("src/test/resources/account-change-add-user.xml");
			// adding objects to repo
			ModelServiceUtil.addObjectToRepo(repositoryService,
					"src/test/resources/user-template-create-account.xml");
			ResourceType resourceType = (ResourceType) ModelServiceUtil.addObjectToRepo(repositoryService,
					change.getResource());
			AccountShadowType accountType = (AccountShadowType) ModelServiceUtil.addObjectToRepo(
					repositoryService, change.getShadow());

			// setting resource for ResourceObjectShadowType
			((ResourceObjectShadowType) ((ObjectChangeAdditionType) change.getObjectChange()).getObject())
					.setResource(resourceType);
			((ResourceObjectShadowType) ((ObjectChangeAdditionType) change.getObjectChange()).getObject())
					.setResourceRef(null);

			assertNotNull(resourceType);
			OperationResult result = new OperationResult("Test Operation");
			resourceObjectChangeService.notifyChange(change);
			// resourceObjectChangeService.notifyChange(change, result);
			LOGGER.info(result.debugDump());
			// creating filter to search user according to the user name
			Document doc = DOMUtil.getDocument();

			XPathType xpath = new XPathType();

			List<Element> values = new ArrayList<Element>();
			Element element = doc.createElementNS(SchemaConstants.NS_C, "name");
			element.setTextContent("will");
			values.add(element);

			Element filter = QueryUtil.createAndFilter(doc,
					QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE)),
					QueryUtil.createEqualFilter(doc, xpath, values));

			QueryType query = new QueryType();
			query.setFilter(filter);

			ObjectListType list = repositoryService.searchObjects(query, new PagingType(),
					new OperationResult("Search Objects"));
			assertNotNull(list);
			assertEquals(1, list.getObject().size());

			addedUser = (UserType) list.getObject().get(0);
			assertNotNull(addedUser);

			List<ObjectReferenceType> accountRefs = addedUser.getAccountRef();
			assertEquals(accountOid, accountRefs.get(0).getOid());

			AccountShadowType addedAccount = (AccountShadowType) repositoryService.getObject(accountOid,
					new PropertyReferenceListType(), result);

			assertNotNull(addedAccount);
			assertEquals(addedUser.getName(), addedAccount.getName());
		} catch (Exception ex) {
			LOGGER.info("a", ex);
			throw ex;
		} finally {
			// cleanup repo
			ModelServiceUtil.deleteObject(repositoryService, accountOid);
			ModelServiceUtil.deleteObject(repositoryService, resourceOid);
			ModelServiceUtil.deleteObject(repositoryService, userTemplateOid);
			if (addedUser != null) {
				ModelServiceUtil.deleteObject(repositoryService, addedUser.getOid());
			}
		}
	}
}
