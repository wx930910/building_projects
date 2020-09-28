/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.scxml2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.SimpleErrorReporter;
import org.apache.commons.scxml2.invoke.Invoker;
import org.apache.commons.scxml2.invoke.InvokerException;
import org.apache.commons.scxml2.model.Invoke;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;

/**
 * SCXMLExecutionContext provides all the services and internal data used during the interpretation of an SCXML
 * statemachine across micro and macro steps
 */
public class SCXMLExecutionContext implements SCXMLIOProcessor {

    /**
     * SCXML Execution Logger for the application.
     */
    private Log appLog = LogFactory.getLog(SCXMLExecutionContext.class);

    /**
     * The action execution context instance, providing restricted access to this execution context
     */
    private final ActionExecutionContext actionExecutionContext;

    /**
     * The SCInstance.
     */
    private SCInstance scInstance;

    /**
     * The evaluator for expressions.
     */
    private Evaluator evaluator;

    /**
     * The external IOProcessor for Invokers to communicate back on
     */
    private SCXMLIOProcessor externalIOProcessor;

    /**
     * The event dispatcher to interface with external documents etc.
     */
    private EventDispatcher eventdispatcher;

    /**
     * The environment specific error reporter.
     */
    private ErrorReporter errorReporter = null;

    /**
     * The notification registry.
     */
    private NotificationRegistry notificationRegistry;

    /**
     * The internal event queue
     */
    private final Queue<TriggerEvent> internalEventQueue = new LinkedList<TriggerEvent>();

    /**
     * The Invoker classes map, keyed by invoke target types (specified using "type" attribute).
     */
    private final Map<String, Class<? extends Invoker>> invokerClasses = new HashMap<String, Class<? extends Invoker>>();

    /**
     * The map storing the unique invokeId for an Invoke with an active Invoker
     */
    private final Map<Invoke, String> invokeIds = new HashMap<Invoke, String>();

    /**
     * The Map of active Invoker, keyed by their unique invokeId.
     */
    private final Map<String, Invoker> invokers = new HashMap<String, Invoker>();

    /**
     * Running status for this state machine
     */
    private boolean running;

    /**
     * Constructor
     *
     * @param externalIOProcessor The external IO Processor
     * @param evaluator The evaluator
     * @param eventDispatcher The event dispatcher, if null a SimpleDispatcher instance will be used
     * @param errorReporter The error reporter, if null a SimpleErrorReporter instance will be used
     */
    protected SCXMLExecutionContext(SCXMLIOProcessor externalIOProcessor, Evaluator evaluator,
                                    EventDispatcher eventDispatcher, ErrorReporter errorReporter) {
        this.externalIOProcessor = externalIOProcessor;
        this.evaluator = evaluator;
        this.eventdispatcher = eventDispatcher != null ? eventDispatcher : new SimpleDispatcher();
        this.errorReporter = errorReporter != null ? errorReporter : new SimpleErrorReporter();
        this.notificationRegistry = new NotificationRegistry();

        this.scInstance = new SCInstance(this, this.evaluator, this.errorReporter);
        this.actionExecutionContext = new ActionExecutionContext(this);
    }

    public SCXMLIOProcessor getExternalIOProcessor() {
        return externalIOProcessor;
    }

    public SCXMLIOProcessor getInternalIOProcessor() {
        return this;
    }

    /**
     * @return Returns the restricted execution context for actions
     */
    public ActionExecutionContext getActionExecutionContext() {
        return actionExecutionContext;
    }

    /**
     * @return Returns true if this state machine is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Stop a running state machine
     */
    public void stopRunning() {
        this.running = false;
    }

    /**
     * Initialize method which will cancel all current active Invokers, clear the internal event queue and mark the
     * state machine process as running (again).
     *
     * @throws ModelException if the state machine instance failed to initialize.
     */
    public void initialize() throws ModelException {
        running = false;
        if (!invokeIds.isEmpty()) {
            for (Invoke invoke : new ArrayList<Invoke>(invokeIds.keySet())) {
                cancelInvoker(invoke);
            }
        }
        internalEventQueue.clear();
        scInstance.initialize();
        running = true;
    }

    /**
     * @return Returns the SCXML Execution Logger for the application
     */
    public Log getAppLog() {
        return appLog;
    }

    /**
     * @return Returns the state machine
     */
    public SCXML getStateMachine() {
        return scInstance.getStateMachine();
    }

    /**
     * Set or replace the state machine to be executed
     * <p>
     * If the state machine instance has been initialized before, it will be initialized again, destroying all existing
     * state!
     * </p>
     * @param stateMachine The state machine to set
     * @throws ModelException if attempting to set a null value or the state machine instance failed to re-initialize
     */
    protected void setStateMachine(SCXML stateMachine) throws ModelException {
        scInstance.setStateMachine(stateMachine);
    }

    /**
     * @return Returns the SCInstance
     */
    public SCInstance getScInstance() {
        return scInstance;
    }

    /**
     * @return Returns The evaluator.
     */
    public Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Set or replace the evaluator
     * <p>
     * If the state machine instance has been initialized before, it will be initialized again, destroying all existing
     * state!
     * </p>
     * @param evaluator The evaluator to set
     * @throws ModelException if attempting to set a null value or the state machine instance failed to re-initialize
     */
    protected void setEvaluator(Evaluator evaluator) throws ModelException {
        scInstance.setEvaluator(evaluator);
        this.evaluator = evaluator;
    }

    /**
     * @return Returns the error reporter
     */
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Set or replace the error reporter
     *
     * @param errorReporter The error reporter to set, if null a SimpleErrorReporter instance will be used instead
     */
    protected void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter != null ? errorReporter : new SimpleErrorReporter();
        try {
            scInstance.setErrorReporter(errorReporter);
        }
        catch (ModelException me) {
            // won't happen
            return;
        }
    }

    /**
     * @return Returns the event dispatcher
     */
    public EventDispatcher getEventDispatcher() {
        return eventdispatcher;
    }

    /**
     * Set or replace the event dispatch
     *
     * @param eventdispatcher The event dispatcher to set, if null a SimpleDispatcher instance will be used instead
     */
    protected void setEventdispatcher(EventDispatcher eventdispatcher) {
        this.eventdispatcher = eventdispatcher != null ? eventdispatcher : new SimpleDispatcher();
    }

    /**
     * @return Returns the notification registry
     */
    public NotificationRegistry getNotificationRegistry() {
        return notificationRegistry;
    }

    /**
     * Detach the current SCInstance to allow external serialization.
     * <p>
     * {@link #attachInstance(SCInstance)} can be used to re-attach a previously detached instance
     * </p>
     * @return the detached instance
     */
    protected SCInstance detachInstance() {
        SCInstance instance = scInstance;
        scInstance.detach();
        scInstance = null;
        return instance;
    }

    /**
     * Re-attach a previously detached SCInstance.
     * <p>
     * Note: an already attached instance will get overwritten (and thus lost).
     * </p>
     * @param instance An previously detached SCInstance
     */
    protected void attachInstance(SCInstance instance) {
        if (scInstance != null ) {
            scInstance.detach();
        }
        scInstance = instance;
        if (scInstance != null) {
            scInstance.detach();
            try {
                scInstance.setEvaluator(evaluator);
                scInstance.setErrorReporter(errorReporter);
            }
            catch (ModelException me) {
                // should not happen
                return;
            }
        }
    }

    /**
     * Register an Invoker for this target type.
     *
     * @param type The target type (specified by "type" attribute of the invoke element).
     * @param invokerClass The Invoker class.
     */
    protected void registerInvokerClass(final String type, final Class<? extends Invoker> invokerClass) {
        invokerClasses.put(type, invokerClass);
    }

    /**
     * Remove the Invoker registered for this target type (if there is one registered).
     *
     * @param type The target type (specified by "type" attribute of the invoke element).
     */
    protected void unregisterInvokerClass(final String type) {
        invokerClasses.remove(type);
    }

    /**
     * Create a new {@link Invoker}
     *
     * @param type The type of the target being invoked.
     * @return An {@link Invoker} for the specified type, if an
     *         invoker class is registered against that type,
     *         <code>null</code> otherwise.
     * @throws InvokerException When a suitable {@link Invoker} cannot be instantiated.
     */
    public Invoker newInvoker(final String type) throws InvokerException {
        Class<? extends Invoker> invokerClass = invokerClasses.get(type);
        if (invokerClass == null) {
            throw new InvokerException("No Invoker registered for type \"" + type + "\"");
        }
        try {
            return invokerClass.newInstance();
        } catch (InstantiationException ie) {
            throw new InvokerException(ie.getMessage(), ie.getCause());
        } catch (IllegalAccessException iae) {
            throw new InvokerException(iae.getMessage(), iae.getCause());
        }
    }

    /**
     * Get the {@link Invoker} for this {@link Invoke}.
     * May return <code>null</code>. A non-null {@link Invoker} will be
     * returned if and only if the {@link Invoke} parent TransitionalState is
     * currently active and contains the &lt;invoke&gt; child.
     *
     * @param invoke The <code>Invoke</code>.
     * @return The Invoker.
     */
    public Invoker getInvoker(final Invoke invoke) {
        return invokers.get(invokeIds.get(invoke));
    }

    /**
     * Set the {@link Invoker} for a {@link Invoke} and returns the unique invokerId for the Invoker
     *
     * @param invoke The Invoke.
     * @param invoker The Invoker.
     * @return The invokeId
     */
    public String setInvoker(final Invoke invoke, final Invoker invoker) {
        String invokeId = invoke.getId();
        if (invokeId == null) {
            invokeId = UUID.randomUUID().toString();
        }
        invokeIds.put(invoke, invokeId);
        invokers.put(invokeId, invoker);
        return invokeId;
    }

    /**
     * Remove a previously active Invoker, which must already have been canceled
     * @param invoke The Invoke for the Invoker to remove
     */
    public void removeInvoker(final Invoke invoke) {
        invokers.remove(invokeIds.remove(invoke));
    }

    /**
     * @return Returns the map of current active Invokes and their invokeId
     */
    public Map<Invoke, String> getInvokeIds() {
        return invokeIds;
    }


    /**
     * Cancel and remove an active Invoker
     *
     * @param invoke The Invoke for the Invoker to cancel
     */
    public void cancelInvoker(Invoke invoke) {
        String invokeId = invokeIds.get(invoke);
        if (invokeId != null) {
            try {
                invokers.get(invokeId).cancel();
            } catch (InvokerException ie) {
                TriggerEvent te = new TriggerEvent("failed.invoke.cancel."+invokeId, TriggerEvent.ERROR_EVENT);
                addEvent(te);
            }
            removeInvoker(invoke);
        }
    }

    /**
     * Add an event to the internal event queue
     * @param event The event
     */
    @Override
    public void addEvent(TriggerEvent event) {
        internalEventQueue.add(event);
    }

    /**
     * @return Returns the next event from the internal event queue, if available
     */
    public TriggerEvent nextInternalEvent() {
        return internalEventQueue.poll();
    }

    /**
     * @return Returns true if the internal event queue isn't empty
     */
    public boolean hasPendingInternalEvent() {
        return !internalEventQueue.isEmpty();
    }
}
