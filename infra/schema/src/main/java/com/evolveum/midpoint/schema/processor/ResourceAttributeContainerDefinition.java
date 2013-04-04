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

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

/**
 * Resource Object Definition (Object Class).
 * 
 * Object Class refers to a type of object on the Resource. Unix account, Active
 * Directory group, inetOrgPerson LDAP objectclass or a schema of USERS database
 * table are all Object Classes from the midPoint point of view. Object class
 * defines a set of attribute names, types for each attributes and few
 * additional properties.
 * 
 * This class represents schema definition for resource object (object class).
 * See {@link Definition} for more details.
 * 
 * Resource Object Definition is immutable. TODO: This will probably need to be
 * changed to a mutable object.
 * 
 * @author Radovan Semancik
 * 
 */
public class ResourceAttributeContainerDefinition extends PrismContainerDefinition<ShadowAttributesType> {

	private static final long serialVersionUID = 3943909626639924429L;
	
	public ResourceAttributeContainerDefinition(QName name, ObjectClassComplexTypeDefinition complexTypeDefinition,  PrismContext prismContext) {
		super(name, complexTypeDefinition, prismContext);
		super.setCompileTimeClass(ShadowAttributesType.class);
	}
	
	@Override
	public ObjectClassComplexTypeDefinition getComplexTypeDefinition() {
		return (ObjectClassComplexTypeDefinition)super.getComplexTypeDefinition();
	}

	/**
	 * Returns the definition of identifier attributes of a resource object.
	 * 
	 * May return empty set if there are no identifier attributes. Must not
	 * return null.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return definition of identifier attributes
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public Collection<? extends ResourceAttributeDefinition> getIdentifiers() {
		return getComplexTypeDefinition().getIdentifiers();
	}

	/**
	 * Returns the definition of secondary identifier attributes of a resource
	 * object.
	 * 
	 * May return empty set if there are no secondary identifier attributes.
	 * Must not return null.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return definition of secondary identifier attributes
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public Collection<? extends ResourceAttributeDefinition> getSecondaryIdentifiers() {
		return getComplexTypeDefinition().getSecondaryIdentifiers();
	}

	/**
	 * Returns the definition of description attribute of a resource object.
	 * 
	 * Returns null if there is no description attribute.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return definition of secondary identifier attributes
	 * @throws IllegalStateException
	 *             if there is more than one description attribute. But this
	 *             should never happen.
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public ResourceAttributeDefinition getDescriptionAttribute() {
		return getComplexTypeDefinition().getDescriptionAttribute();
	}

	public void setDescriptionAttribute(ResourceAttributeDefinition descriptionAttribute) {
		// We can afford to delegate a set here as we know that there is one-to-one correspondence between
		// object class definition and attribute container
		getComplexTypeDefinition().setDescriptionAttribute(descriptionAttribute);
	}
	
	/**
	 * Specifies which resource attribute should be used as a "technical" name
	 * for the account. This name will appear in log files and other troubleshooting
	 * tools. The name should be a form of unique identifier that can be used to
	 * locate the resource object for diagnostics. It should not contain white chars and
	 * special chars if that can be avoided and it should be reasonable short.
                
	 * It is different from a display name attribute. Display name is intended for a 
	 * common user or non-technical administrator (such as role administrator). The
	 * naming attribute is intended for technical IDM administrators and developers.
	 * 
	 * @return resource attribute definition that should be used as a "technical" name
	 * 					for the account.
	 */
	public ResourceAttributeDefinition getNamingAttribute() {
		return getComplexTypeDefinition().getNamingAttribute();
	}

	public void setNamingAttribute(ResourceAttributeDefinition namingAttribute) {
		// We can afford to delegate a set here as we know that there is one-to-one correspondence between
		// object class definition and attribute container
		getComplexTypeDefinition().setNamingAttribute(namingAttribute);
	}

	public void setNamingAttribute(QName namingAttribute) {
		getComplexTypeDefinition().setNamingAttribute(namingAttribute);
	}

	/**
	 * Returns the native object class string for the resource object.
	 * 
	 * Native object class is the name of the Resource Object Definition (Object
	 * Class) as it is seen by the resource itself. The name of the Resource
	 * Object Definition may be constrained by XSD or other syntax and therefore
	 * may be "mangled" to conform to such syntax. The <i>native object
	 * class</i> value will contain unmangled name (if available).
	 * 
	 * Returns null if there is no native object class.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return native object class
	 * @throws IllegalStateException
	 *             if there is more than one description attribute.
	 */
	public String getNativeObjectClass() {
		return getComplexTypeDefinition().getNativeObjectClass();
	}

	public void setNativeObjectClass(String nativeObjectClass) {
		// We can afford to delegate a set here as we know that there is one-to-one correspondence between
		// object class definition and attribute container
		getComplexTypeDefinition().setNativeObjectClass(nativeObjectClass);
	}

	/**
	 * Indicates whether definition is should be used as default account type.
	 * 
	 * If true value is returned then the definition should be used as a default
	 * account type definition. This is a way how a resource connector may
	 * suggest applicable object classes (resource object definitions) for
	 * accounts.
	 * 
	 * If no information about account type is present, false should be
	 * returned. This method must return true only if isAccountType() returns
	 * true.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of at-most-one value should be done at the time of
	 * schema parsing. The exception may not even be thrown at all if the
	 * implementation is not able to determine duplicity.
	 * 
	 * @return true if the definition should be used as account type.
	 * @throws IllegalStateException
	 *             if more than one default account is suggested in the schema.
	 */
	public boolean isDefaultInAKind() {
		return getComplexTypeDefinition().isDefaultInAKind();
	}

	public void setDefaultInAKind(boolean defaultAccountType) {
		getComplexTypeDefinition().setDefaultInAKind(defaultAccountType);
	}
	
	public String getIntent() {
		return getComplexTypeDefinition().getIntent();
	}
	
	public void setIntent(String accountTypeName) {
		getComplexTypeDefinition().setIntent(accountTypeName);
	}
	
	public ShadowKindType getKind() {
		return getComplexTypeDefinition().getKind();
	}
	
	public void setKind(ShadowKindType kind) {
		getComplexTypeDefinition().setKind(kind);
	}

	/**
	 * Returns the definition of display name attribute.
	 * 
	 * Display name attribute specifies which resource attribute should be used
	 * as title when displaying objects of a specific resource object class. It
	 * must point to an attribute of String type. If not present, primary
	 * identifier should be used instead (but this method does not handle this
	 * default behavior).
	 * 
	 * Returns null if there is no display name attribute.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return native object class
	 * @throws IllegalStateException
	 *             if there is more than one display name attribute or the
	 *             definition of the referenced attribute does not exist.
	 */
	public ResourceAttributeDefinition getDisplayNameAttribute() {
		return getComplexTypeDefinition().getDisplayNameAttribute();
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition displayName) {
		getComplexTypeDefinition().setDisplayNameAttribute(displayName);
	}

	/**
	 * TODO
	 * 
	 * Convenience method. It will internally look up the correct definition.
	 * 
	 * @param displayName
	 */
	public void setDisplayNameAttribute(QName displayName) {
		getComplexTypeDefinition().setDisplayNameAttribute(displayName);
	}

	public ResourceAttributeContainer instantiate() {
		return instantiate(getNameOrDefaultName());
	}
	
	public ResourceAttributeContainer instantiate(QName name) {
		return new ResourceAttributeContainer(name, this, prismContext);
	}
	
	public ResourceAttributeContainerDefinition clone() {
		ResourceAttributeContainerDefinition clone = new ResourceAttributeContainerDefinition(name, 
				(ObjectClassComplexTypeDefinition)complexTypeDefinition.clone(), prismContext);
		copyDefinitionData(clone);
		return clone;
	}
	
	protected void copyDefinitionData(ResourceAttributeContainerDefinition clone) {
		super.copyDefinitionData(clone);
	}

//	public Set<ResourceAttribute> parseAttributes(List<Object> elements, PropertyPath parentPath) throws SchemaException {
//		return (Set) parseItems(elements, parentPath);
//	}
//	
//	// Resource objects are usualy constructed as top-level objects, so this comes handy
//	public Set<ResourceAttribute> parseAttributes(List<Object> elements) throws SchemaException {
//		return (Set) parseItems(elements, null);
//	}
//
//	public Collection<? extends ResourceAttribute> parseIdentifiers(List<Object> elements, PropertyPath parentPath) throws SchemaException {
//		return (Collection) parseItems(elements, parentPath, getIdentifiers());
//	}
//	
//	// Resource objects are usualy constructed as top-level objects, so this comes handy
//	public Collection<? extends ResourceAttribute> parseIdentifiers(List<Object> elements) throws SchemaException {
//		return (Collection) parseItems(elements, null, getIdentifiers());
//	}

	public ResourceAttributeDefinition findAttributeDefinition(QName elementQName) {
		return findItemDefinition(elementQName, ResourceAttributeDefinition.class);
	}
	
	public ResourceAttributeDefinition findAttributeDefinition(ItemPath elementPath) {
		return findItemDefinition(elementPath, ResourceAttributeDefinition.class);
	}
	
	public ResourceAttributeDefinition findAttributeDefinition(String elementLocalname) {
		QName elementQName = new QName(getNameOrDefaultName().getNamespaceURI(),elementLocalname);
		return findAttributeDefinition(elementQName);
	}
	
	public Collection<? extends ResourceAttributeDefinition> getAttributeDefinitions() {
		Set<ResourceAttributeDefinition> attrs = new HashSet<ResourceAttributeDefinition>();
		for (ItemDefinition def: complexTypeDefinition.getDefinitions()) {
			if (def instanceof ResourceAttributeDefinition) {
				attrs.add((ResourceAttributeDefinition)def);
			}
		}
		return attrs;
	}
	
	public <T extends ShadowType> PrismObjectDefinition<T> toShadowDefinition() {
		PrismObjectDefinition<T> origShadowDef =  (PrismObjectDefinition<T>) prismContext.getSchemaRegistry().
			findObjectDefinitionByCompileTimeClass(ShadowType.class);
		PrismObjectDefinition<T> shadowDefinition = 
			origShadowDef.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES, this);
		return shadowDefinition;
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
		sb.append(toString());
		for (Definition def : getDefinitions()) {
			sb.append("\n");
			if (def instanceof ResourceAttributeDefinition) {
				ResourceAttributeDefinition attrDef = (ResourceAttributeDefinition)def;
				sb.append(attrDef.debugDump(indent+1));
				if (attrDef.isIdentifier(this)) {
					sb.deleteCharAt(sb.length()-1);
					sb.append(" id");
				}
			} else {
				sb.append(def.debugDump(indent+1));
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(":").append(getName()).append(" (").append(getTypeName()).append(")");
		if (isDefaultInAKind()) {
			sb.append(" def");
		}
		if (getKind() != null) {
			sb.append(" ").append(getKind());
		}
		if (getNativeObjectClass()!=null) {
			sb.append(" native=");
			sb.append(getNativeObjectClass());
		}
		return sb.toString();
	}


}
