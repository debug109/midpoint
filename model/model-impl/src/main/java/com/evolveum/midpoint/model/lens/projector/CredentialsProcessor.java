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
package com.evolveum.midpoint.model.lens.projector;

import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor that takes password from user and synchronizes it to accounts.
 * <p/>
 * The implementation is very simple now. It only cares about password value, not
 * expiration or other password facets. It completely ignores other credential types.
 *
 * @author Radovan Semancik
 */
@Component
public class CredentialsProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private PasswordPolicyProcessor passwordPolicyProcessor;

    public <F extends ObjectType, P extends ObjectType> void processCredentials(LensContext<F,P> context, LensProjectionContext<P> projectionContext, OperationResult result) 
    		throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {	
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null && focusContext.getObjectTypeClass() == UserType.class) {
    		processCredentialsUser((LensContext<UserType,ShadowType>) context, (LensProjectionContext<ShadowType>)projectionContext, result);
//    		return;
    	}
//    	if (focusContext.getObjectTypeClass() != UserType.class) {
//    		// We can do this only for user.
//    		return;
//    	} 	
    	
    	passwordPolicyProcessor.processPasswordPolicy((LensProjectionContext<ShadowType>)projectionContext, context, result);
    }
    
    
    public void processCredentialsUser(LensContext<UserType,ShadowType> context, final LensProjectionContext<ShadowType> accCtx, OperationResult result) 
		throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userDelta = focusContext.getDelta();
        PropertyDelta<PasswordType> userPasswordValueDelta = null;
        if (userDelta != null) {
        	userPasswordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        	// Modification sanity check
            if (userDelta.getChangeType() == ChangeType.MODIFY && userPasswordValueDelta != null && 
            		(userPasswordValueDelta.isAdd() || userPasswordValueDelta.isDelete())) {
            	throw new SchemaException("User password value cannot be added or deleted, it can only be replaced"); 
            }
        }
//            LOGGER.trace("userDelta is null, skipping credentials processing");
//            return; 

        PrismObject<UserType> userNew = focusContext.getObjectNew();
        if (userNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("userNew is null, skipping credentials processing");
            return;
        }
        
        PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismPropertyDefinition accountPasswordPropertyDefinition = accountDefinition.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

        ResourceShadowDiscriminator rat = accCtx.getResourceShadowDiscriminator();

        ObjectDelta<ShadowType> accountDelta = accCtx.getDelta();
        PropertyDelta<ProtectedStringType> accountPasswordValueDelta = null;
        if (accountDelta != null) {
        	accountPasswordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        }
        if (accountDelta != null && accountDelta.getChangeType() == ChangeType.MODIFY) {
        	if (accountPasswordValueDelta != null && (accountPasswordValueDelta.isAdd() || accountDelta.isDelete())) {
        		throw new SchemaException("Password for account "+rat+" cannot be added or deleted, it can only be replaced");
        	}
        }
        if (accountDelta != null && (accountDelta.getChangeType() == ChangeType.ADD || accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD)) {
            // adding new account, synchronize password regardless whether the password was changed or not.
        } else if (userPasswordValueDelta != null) {
            // user password was changed. synchronize it regardless of the account change.
        } else {
            LOGGER.trace("No change in password and the account is not added, skipping credentials processing for account " + rat);
            return;
        }

        RefinedObjectClassDefinition refinedAccountDef = accCtx.getRefinedAccountDefinition();
        if (refinedAccountDef == null){
        	LOGGER.trace("No RefinedAccountDefinition, therefore also no password outbound definition, skipping credentials processing for account " + rat);
          return;
        }
        
        MappingType outboundMappingType = refinedAccountDef.getCredentialsOutbound();
        
        if (outboundMappingType == null) {
            LOGGER.trace("No outbound definition in password definition in credentials in account type {}, skipping credentials processing", rat);
            return;
        }
        
        Mapping<PrismPropertyValue<ProtectedStringType>> passwordMapping = valueConstructionFactory.createMapping(outboundMappingType, 
        		"outbound password mapping in account type " + rat);
        if (!passwordMapping.isApplicableToChannel(context.getChannel())) {
        	return;
        }
        
        passwordMapping.setDefaultTargetDefinition(accountPasswordPropertyDefinition);
        ItemDeltaItem<PrismPropertyValue<PasswordType>> userPasswordIdi = focusContext.getObjectDeltaObject().findIdi(SchemaConstants.PATH_PASSWORD_VALUE);
        Source<PrismPropertyValue<PasswordType>> source = new Source<PrismPropertyValue<PasswordType>>(userPasswordIdi, ExpressionConstants.VAR_INPUT);
		passwordMapping.setDefaultSource(source);
		passwordMapping.setOriginType(OriginType.OUTBOUND);
		passwordMapping.setOriginObject(accCtx.getResource());
		
		if (passwordMapping.getStrength() != MappingStrengthType.STRONG) {
        	if (accountPasswordValueDelta != null && !accountPasswordValueDelta.isEmpty()) {
        		return;
        	}
        }
		
		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private ItemPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				ValuePolicyType passwordPolicy = accCtx.getEffectivePasswordPolicy();
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};
		passwordMapping.setStringPolicyResolver(stringPolicyResolver);
		
        passwordMapping.evaluate(result);
        
        PrismProperty<ProtectedStringType> accountPasswordNew = (PrismProperty) passwordMapping.getOutput();
        if (accountPasswordNew == null) {
            LOGGER.trace("Credentials 'password' expression resulted in null, skipping credentials processing for {}", rat);
            return;
        }
        PropertyDelta<ProtectedStringType> accountPasswordDeltaNew = new PropertyDelta<ProtectedStringType>(SchemaConstants.PATH_PASSWORD_VALUE, accountPasswordPropertyDefinition);
        accountPasswordDeltaNew.setValuesToReplace(accountPasswordNew.getClonedValues());
        LOGGER.trace("Adding new password delta for account {}", rat);
        accCtx.addToSecondaryDelta(accountPasswordDeltaNew);

    }


}
