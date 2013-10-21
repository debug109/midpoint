/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class H3Header extends SimplePanel<ObjectWrapper> {

    private static final String ID_TITLE = "title";
    private static final String ID_MENU = "menu";

    public H3Header(String id, IModel<ObjectWrapper> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        Label title = new Label(ID_TITLE, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getDisplayName();
            }
        });
        add(title);

        final IModel<List<InlineMenuItem>> items = new Model((Serializable) createMenuItems());
        InlineMenu menu = new InlineMenu(ID_MENU, items);
        menu.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                List<InlineMenuItem> list = items.getObject();
                return list != null && !list.isEmpty();
            }
        });
        add(menu);
    }

    private String getDisplayName() {
        ObjectWrapper wrapper = getModel().getObject();
        String key = wrapper.getDisplayName();
        if (key == null) {
            key = "";
        }

        return new StringResourceModel(key, getPage(), null, key).getString();
    }

    protected List<InlineMenuItem> createMenuItems() {
        return new ArrayList<InlineMenuItem>();
    }
}
