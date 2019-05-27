/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.story.perf;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.testing.story.TestTrafo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.mysql.cj.jdbc.Driver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests the performance of bulk import of data. See MID-5368.
 *
 * This test is not meant to be run automatically.
 * It requires externally-configured MySQL database with the data to be imported.
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImport extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "perf/import");

	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

	public static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "template-import.xml");
	public static final String OBJECT_TEMPLATE_OID = "e84d7b5a-4634-4b75-a17c-df0b8b49b593";

	protected static final File RESOURCE_SOURCE_FILE = new File(TEST_DIR, "resource-source.xml");
	protected static final String RESOURCE_SOURCE_OID = "f2dd9222-6aff-4099-b5a2-04ae6b3a00b7";

	protected static final File ORG_BASIC_FILE = new File(TEST_DIR, "org-basic.xml");

	protected static final File TASK_IMPORT_FILE = new File(TEST_DIR, "task-import.xml");
	protected static final String TASK_IMPORT_OID = "50142510-8003-4a47-993a-2434119f5028";

	private static final int IMPORT_TIMEOUT = 3600_000 * 3;     // 3 hours

	private static final int USERS = 50000;

	private int usersBefore;

	private PrismObject<ResourceType> sourceResource;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		InternalsConfig.turnOffAllChecks();

		Class.forName(Driver.class.getName());

		// Resources
		sourceResource = importAndGetObjectFromFile(ResourceType.class, RESOURCE_SOURCE_FILE, RESOURCE_SOURCE_OID, initTask, initResult);

		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_OID);

		// Org
		repoAddObjectFromFile(ORG_BASIC_FILE, OrgType.class, initResult);

		usersBefore = repositoryService.countObjects(UserType.class, null, null, initResult);
		display("users before", usersBefore);

		//InternalMonitor.setTrace(InternalOperationClasses.PRISM_OBJECT_CLONES, true);
	}

	@Override
	protected boolean isAvoidLoggingChange() {
		return false;           // we want logging from our system config
	}

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Override
	protected void importSystemTasks(OperationResult initResult) {
		// nothing here
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

        OperationResult testResultHr = modelService.testResource(RESOURCE_SOURCE_OID, task);
        TestUtil.assertSuccess(testResultHr);

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);
	}

	@Test
    public void test100RunImport() throws Exception {
		final String TEST_NAME = "test100RunImport";
        displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		importObjectFromFile(TASK_IMPORT_FILE, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        waitForTaskFinish(TASK_IMPORT_OID, true, IMPORT_TIMEOUT);

        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<TaskType> taskAfter = repositoryService.getObject(TaskType.class, TASK_IMPORT_OID, null, result);
		String taskXml = prismContext.xmlSerializer().serialize(taskAfter);
		display("Task after", taskXml);

		int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
		display("users after", usersAfter);
		assertEquals("Wrong # of users", usersBefore + USERS, usersAfter);

	}

}
