/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 *
 */
public interface ModelElementContext<O extends ObjectType> extends Serializable, DebugDumpable {

    /**
     * @return Type of object represented by this context. It is declared when context is created,
     * so the actual type of the object can be its subtype. (Although this is quite unusual.)
     */
    Class<O> getObjectTypeClass();

    /**
     * @return True if the declared or actual object type is a subtype of aClass.
     */
    boolean isOfType(Class<?> aClass);

    /**
     * "Old" state of the object i.e. the one that was present when the clockwork started.
     * It can be present on the beginning or filled-in during projector execution by the context loaded.
     *
     * This value is used as an "old state" for resource object mappings (in constructions or resources),
     * persona mappings, notifications, policy rules, and so on.
     */
    PrismObject<O> getObjectOld();

    /**
     * "Current" state of the object i.e. the one that was present when the current clockwork click
     * started. It is typically filled-in by the context loader. For projections, it is usually the same
     * as the "old" state, as they are not updated iteratively but only once per clockwork run.
     *
     * This value used as an "old state" for focus mappings (in object template or assigned ones).
     */
    PrismObject<O> getObjectCurrent();

    /**
     * Expected state of the object after application of currentDelta i.e. item deltas computed
     * during current projection: objectCurrent + currentDelta = objectNew.
     */
    PrismObject<O> getObjectNew();

    /**
     * @return "Any" value of the object in this order: new, current, old; taking the first non-null one.
     * It is used when we are not interested in the details but we want just "any" value, e.g. for reporting
     * purposes.
     */
    PrismObject<O> getObjectAny();

    /**
     * @return OID of the object. If not determined yet, it is obtained from available sources, like
     * object old, current, new, or primary delta.
     */
    String getOid();

    /**
     * Returns all policy rules that apply to this object - even those that were not triggered.
     * The policy rules are compiled from all the applicable sources (target, meta-roles, etc.)
     */
    @NotNull
    Collection<? extends EvaluatedPolicyRule> getPolicyRules();

    /**
     * Initial intent regarding the account. It indicated what the initiator of the operation WANTS TO DO with the
     * context.
     * If set to null then the decision is left to "the engine". Null is also a typical value
     * when the context is created. It may be pre-set under some circumstances, e.g. if an account is being unlinked.
     */
    SynchronizationIntent getSynchronizationIntent();

    /**
     * @return true if the object (focus or projection) is to be added
     */
    boolean isAdd();

    /**
     * @return true if the object (focus or projection) is to be deleted
     */
    boolean isDelete();

    /**
     * @return Primary delta i.e. one that the caller specified that has to be executed.
     */
    ObjectDelta<O> getPrimaryDelta();

    /**
     * Sets the primary delta. Not to be publicly used. TODO reconsider this method here.
     */
    void setPrimaryDelta(ObjectDelta<O> primaryDelta);

    /**
     * Add a delta to the primary delta. Not to be publicly used. TODO reconsider this method here.
     */
    void addPrimaryDelta(ObjectDelta<O> value) throws SchemaException;

    /**
     * Returns secondary delta for the current clockwork click.
     *
     * The caller MUST NOT modify returned object in any way.
     */
    ObjectDelta<O> getSecondaryDelta();

    /**
     * Returns object delta valid for the current clockwork click. It is either primary delta merged with the current
     * secondary delta (if primary delta was not applied yet), or simply current secondary delta.
     *
     * The returned object is (kind of) immutable. Changing it may do strange things, but most likely the changes will be lost.
     */
    ObjectDelta<?> getCurrentDelta();

    /**
     * Returns object delta comprising both primary delta and (all) secondary deltas, merged together.
     * The returned object is (kind of) immutable. Changing it may do strange things, but most likely the changes will be lost.
     */
    ObjectDelta<O> getSummaryDelta();

    /**
     * @return List of all executed deltas (in fact, {@link ObjectDeltaOperation} objects).
     */
    List<? extends ObjectDeltaOperation> getExecutedDeltas();

    /**
     * @return Determined archetype of the object. Currently not supported for projections.
     */
    ArchetypeType getArchetype();
}
