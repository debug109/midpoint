/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wf.processes.itemApproval;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.wf.DecisionsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ItemApprovalHistoryPanel extends BasePanel<ApprovalContextType> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ItemApprovalHistoryPanel.class);

    private static final String ID_DECISIONS_DONE = "decisionsDone";

    public ItemApprovalHistoryPanel(String id, IModel<ApprovalContextType> model, UserProfileStorage.TableId tableId, int pageSize) {
        super(id, model);
        initLayout(tableId, pageSize);
    }

    private void initLayout(UserProfileStorage.TableId tableId, int pageSize) {

        add(new DecisionsPanel(ID_DECISIONS_DONE, new ReadOnlyModel<>(() -> {
	        List<DecisionDto> rv = new ArrayList<>();
	        ApprovalContextType approvalContext = getModelObject();
	        if (approvalContext == null) {
		        return rv;
	        }
	        // TODO
//	        if (!wfContextType.getEvent().isEmpty()) {
//		        wfContextType.getEvent().forEach(e -> addIgnoreNull(rv, DecisionDto.create(e, getPageBase())));
//	        } else {
//		        ItemApprovalProcessStateType instanceState = WfContextUtil.getItemApprovalProcessInfo(wfContextType);
//		        if (instanceState != null) {
//			        instanceState.getDecisions().forEach(d -> addIgnoreNull(rv, DecisionDto.create(d)));
//		        }
//	        }
	        return rv;
        }), tableId, pageSize));
    }
}
