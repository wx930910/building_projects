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
package org.apache.commons.scxml2.semantics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.PathResolver;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.SCXMLSemantics;
import org.apache.commons.scxml2.SCXMLSystemContext;
import org.apache.commons.scxml2.Status;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.invoke.Invoker;
import org.apache.commons.scxml2.invoke.InvokerException;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.DocumentOrder;
import org.apache.commons.scxml2.model.EnterableState;
import org.apache.commons.scxml2.model.Executable;
import org.apache.commons.scxml2.model.Final;
import org.apache.commons.scxml2.model.History;
import org.apache.commons.scxml2.model.Invoke;
import org.apache.commons.scxml2.model.OnEntry;
import org.apache.commons.scxml2.model.OnExit;
import org.apache.commons.scxml2.model.Param;
import org.apache.commons.scxml2.model.Script;
import org.apache.commons.scxml2.model.SimpleTransition;
import org.apache.commons.scxml2.model.TransitionalState;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.Parallel;
import org.apache.commons.scxml2.model.SCXML;
import org.apache.commons.scxml2.model.State;
import org.apache.commons.scxml2.model.Transition;
import org.apache.commons.scxml2.model.TransitionTarget;
import org.apache.commons.scxml2.system.EventVariable;

/**
 * This class encapsulate and implements the
 * <a href="http://www.w3.org/TR/2014/CR-scxml-20140313/#AlgorithmforSCXMLInterpretation">
 *     W3C SCXML Algorithm for SCXML Interpretation</a>
 *
 * <p>Custom semantics can be created by sub-classing this implementation.</p>
 * <p>This implementation is full stateless and all methods are public accessible to make
 * it easier to extend, reuse and test its behavior.</p>
 */
public class SCXMLSemanticsImpl implements SCXMLSemantics {

    /**
     * Suffix for error event that are triggered in reaction to invalid data
     * model locations.
     */
    public static final String ERR_ILLEGAL_ALLOC = ".error.illegalalloc";

    /**
     * Optional post processing immediately following SCXMLReader. May be used
     * for removing pseudo-states etc.
     *
     * @param input  SCXML state machine
     * @param errRep ErrorReporter callback
     * @return normalized SCXML state machine, pseudo states are removed, etc.
     */
    public SCXML normalizeStateMachine(final SCXML input, final ErrorReporter errRep) {
        //it is a no-op for now
        return input;
    }

    /**
     * First step in the execution of an SCXML state machine.
     * <p>
     * This will first (re)initialize the state machine instance, destroying any existing state!
     * </p>
     * <p>
     * The first step is corresponding to the Algorithm for SCXML processing from the interpret() procedure to the
     * mainLoop() procedure up to the blocking wait for an external event.
     * </p>
     * <p>
     * This step will thus complete the SCXML initial execution and a subsequent macroStep to stabilize the state
     * machine again before returning.
     * </p>
     * <p>
     * If the state machine no longer is running after all this, first the {@link #finalStep(SCXMLExecutionContext)}
     * will be called for cleanup before returning.
     * </p>
     * @param exctx The execution context for this step
     * @throws ModelException if the state machine instance failed to initialize or a SCXML model error occurred during
     * the execution.
     */
    public void firstStep(final SCXMLExecutionContext exctx) throws ModelException {
        // (re)initialize the execution context and state machine instance
        exctx.initialize();
        // execute global script if defined
        executeGlobalScript(exctx);
        // enter initial states
        HashSet<TransitionalState> statesToInvoke = new HashSet<TransitionalState>();
        Step step = new Step(null);
        step.getTransitList().add(exctx.getStateMachine().getInitialTransition());
        microStep(exctx, step, statesToInvoke);
        // AssignCurrentStatus
        setSystemAllStatesVariable(exctx.getScInstance());
        // Execute Immediate Transitions

        if (exctx.isRunning()) {
            macroStep(exctx, statesToInvoke);
        }

        if (!exctx.isRunning()) {
            finalStep(exctx);
        }
    }

    /**
     * Next step in the execution of an SCXML state machine.
     * <p>
     * The next step is corresponding to the Algorithm for SCXML processing mainEventLoop() procedure after receiving an
     * external event, up to the blocking wait for another external event.
     * </p>
     * <p>
     * If the state machine isn't {@link SCXMLExecutionContext#isRunning()} (any more), invoking this method will simply
     * do nothing.
     * </p>
     * <p>
     * If the provided event is a {@link TriggerEvent#CANCEL_EVENT}, the state machine will stop running.
     * </p>
     * <p>
     * Otherwise, the event is set in the {@link SCXMLSystemContext} and processing of the event then is started, and
     * if the event leads to any transitions a microStep for this event will be performed, followed up by a macroStep
     * to stabilize the state machine again before returning.
     * </p>
     * <p>
     * If the state machine no longer is running after all this, first the {@link #finalStep(SCXMLExecutionContext)}
     * will be called for cleanup before returning.
     * </p>
     * @param exctx The execution context for this step
     * @param event The event to process
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void nextStep(final SCXMLExecutionContext exctx, final TriggerEvent event) throws ModelException {
        if (!exctx.isRunning()) {
            return;
        }
        if (isCancelEvent(event)) {
            exctx.stopRunning();
        }
        else {
            setSystemEventVariable(exctx.getScInstance(), event, false);
            processInvokes(exctx, event);
            Step step = new Step(event);
            selectTransitions(exctx, step);
            if (!step.getTransitList().isEmpty()) {
                HashSet<TransitionalState> statesToInvoke = new HashSet<TransitionalState>();
                microStep(exctx, step, statesToInvoke);
                setSystemAllStatesVariable(exctx.getScInstance());
                if (exctx.isRunning()) {
                    macroStep(exctx, statesToInvoke);
                }
            }
        }
        if (!exctx.isRunning()) {
            finalStep(exctx);
        }
    }

    /**
     * The final step in the execution of an SCXML state machine.
     * <p>
     * This final step is corresponding to the Algorithm for SCXML processing exitInterpreter() procedure, after the
     * state machine stopped running.
     * </p>
     * <p>
     * If the state machine still is {@link SCXMLExecutionContext#isRunning()} invoking this method will simply
     * do nothing.
     * </p>
     * <p>
     * This final step will exit all remaining active states and cancel any active invokers.
     * </p>
     * <p>
     *  <em>TODO: the current implementation does not yet provide final donedata handling.</em>
     * </p>
     * @param exctx The execution context for this step
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void finalStep(SCXMLExecutionContext exctx) throws ModelException {
        if (exctx.isRunning()) {
            return;
        }
        ArrayList<EnterableState> configuration = new ArrayList<EnterableState>(exctx.getScInstance().getCurrentStatus().getAllStates());
        Collections.sort(configuration,DocumentOrder.reverseDocumentOrderComparator);
        for (EnterableState es : configuration) {
            for (OnExit onexit : es.getOnExits()) {
                executeContent(exctx, onexit);
            }
            if (es instanceof TransitionalState) {
                // check if invokers are active in this state
                for (Invoke inv : ((TransitionalState)es).getInvokes()) {
                    exctx.cancelInvoker(inv);
                }
            }
            exctx.getNotificationRegistry().fireOnExit(es, es);
            exctx.getNotificationRegistry().fireOnExit(exctx.getStateMachine(), es);
            if (!(es instanceof Final && es.getParent() == null)) {
                exctx.getScInstance().getCurrentStatus().getStates().remove(es);
            }
            // else: keep final Final
            // TODO: returnDoneEvent(s.donedata)?
        }
    }

    /**
     * Perform a micro step in the execution of a state machine.
     * <p>
     * This micro step is corresponding to the Algorithm for SCXML processing microstep() procedure.
     * <p>
     * @param exctx The execution context for this step
     * @param step The current micro step
     * @param statesToInvoke the set of activated states which invokes need to be invoked at the end of the current
     *                       macro step
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void microStep(final SCXMLExecutionContext exctx, final Step step,
                          final Set<TransitionalState> statesToInvoke)
            throws ModelException {
        buildStep(exctx, step);
        exitStates(exctx, step, statesToInvoke);
        executeTransitionContent(exctx, step);
        enterStates(exctx, step, statesToInvoke);
    }

    /**
     * buildStep builds the exitSet and entrySet for the current configuration given the transitionList on the step.
     *
     * @param exctx The SCXML execution context
     * @param step The step containing the list of transitions to be taken
     * @throws ModelException if the result of taking the transitions would lead to an illegal configuration
     */
    public void buildStep(final SCXMLExecutionContext exctx, final Step step) throws ModelException {
        step.getExitSet().clear();
        step.getEntrySet().clear();
        step.getDefaultEntrySet().clear();
        step.getDefaultHistoryTransitionEntryMap().clear();

        // compute exitSet, if there is something to exit
        if (!exctx.getScInstance().getCurrentStatus().getStates().isEmpty()) {
            computeExitSet(step, exctx.getScInstance().getCurrentStatus().getAllStates());
        }
        // compute entrySet
        computeEntrySet(exctx, step);

        // default result states to entrySet
        Set<EnterableState> states = step.getEntrySet();
        if (!step.getExitSet().isEmpty()) {
            // calculate result states by taking current states, subtracting exitSet and adding entrySet
            states = new HashSet<EnterableState>(exctx.getScInstance().getCurrentStatus().getStates());
            states.removeAll(step.getExitSet());
            states.addAll(step.getEntrySet());
        }
        // validate the result states represent a legal configuration
        if (!isLegalConfig(states, exctx.getErrorReporter())) {
            throw new ModelException("Illegal state machine configuration!");
        }
    }

    /**
     * Perform a macro step in the execution of a state machine.
     * <p>
     * This macro step is corresponding to the Algorithm for SCXML processing mainEventLoop() procedure macro step
     * sub-flow, which are the first <em>3</em> steps of the described <em>4</em>, so everything up to the blocking
     * wait for an external event.
     * <p>
     * @param exctx The execution context for this step
     * @param statesToInvoke the set of activated states which invokes need to be invoked at the end of the current
     *                       macro step
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void macroStep(final SCXMLExecutionContext exctx, final Set<TransitionalState> statesToInvoke)
            throws ModelException {
        do {
            boolean macroStepDone = false;
            do {
                Step step = new Step(null);
                selectTransitions(exctx, step);
                if (step.getTransitList().isEmpty()) {
                    TriggerEvent event = exctx.nextInternalEvent();
                    if (event != null) {
                        if (isCancelEvent(event)) {
                            exctx.stopRunning();
                        }
                        else {
                            setSystemEventVariable(exctx.getScInstance(), event, true);
                            step = new Step(event);
                            selectTransitions(exctx, step);
                        }
                    }
                }
                if (step.getTransitList().isEmpty()) {
                    macroStepDone = true;
                }
                else {
                    microStep(exctx, step, statesToInvoke);
                    setSystemAllStatesVariable(exctx.getScInstance());
                }

            } while (exctx.isRunning() && !macroStepDone);

            if (exctx.isRunning() && !statesToInvoke.isEmpty()) {
                initiateInvokes(exctx, statesToInvoke);
                statesToInvoke.clear();
            }
        } while (exctx.isRunning() && exctx.hasPendingInternalEvent());
    }

    /**
     * Compute and store the set of states to exit for the current list of transitions in the provided step.
     * <p>
     * This method corresponds to the Algorithm for SCXML processing computeExitSet() procedure.
     * <p>
     * @param step The step containing the list of transitions to be taken
     * @param configuration The current configuration of the state machine ({@link Status#getAllStates()}).
     */
    public void computeExitSet(final Step step, final Set<EnterableState> configuration) {
        for (SimpleTransition st : step.getTransitList()) {
            computeExitSet(st, step.getExitSet(), configuration);
        }
    }

    /**
     * Compute and store the set of states to exit for one specific transition in the provided step.
     * <p>
     * This method corresponds to the Algorithm for SCXML processing computeExitSet() procedure.
     * <p>
     * @param transition The transition to compute the states to exit from
     * @param exitSet The set for adding the states to exit to
     * @param configuration The current configuration of the state machine ({@link Status#getAllStates()}).
     */
    public void computeExitSet(SimpleTransition transition, Set<EnterableState> exitSet, Set<EnterableState> configuration) {
        if (!transition.getTargets().isEmpty()) {
            TransitionalState transitionDomain = transition.getTransitionDomain();
            if (transitionDomain == null) {
                // root transition: every active state will be exited
                exitSet.addAll(configuration);
            }
            else {
                for (EnterableState state : configuration) {
                    if (state.isDescendantOf(transitionDomain)) {
                        exitSet.add(state);
                    }
                }
            }
        }
    }

    /**
     * Compute and store the set of states to enter for the current list of transitions in the provided step.
     * <p>
     * This method corresponds to the Algorithm for SCXML processing computeEntrySet() procedure.
     * <p>
     * @param exctx The execution context for this step
     * @param step The step containing the list of transitions to be taken
     */
    public void computeEntrySet(final SCXMLExecutionContext exctx, final Step step) {
        Set<History> historyTargets = new HashSet<History>();
        Set<EnterableState> entrySet = new HashSet<EnterableState>();
        for (SimpleTransition st : step.getTransitList()) {
            for (TransitionTarget tt : st.getTargets()) {
                if (tt instanceof EnterableState) {
                    entrySet.add((EnterableState) tt);
                }
                else {
                    // History
                    historyTargets.add((History)tt);
                }
            }
        }
        for (EnterableState es : entrySet) {
            addDescendantStatesToEnter(exctx, step, es);
        }
        for (History h : historyTargets) {
            addDescendantStatesToEnter(exctx, step, h);
        }
        for (SimpleTransition st : step.getTransitList()) {
            TransitionalState ancestor = st.getTransitionDomain();
            for (TransitionTarget tt : st.getTargets()) {
                addAncestorStatesToEnter(exctx, step, tt, ancestor);
            }
        }
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing addDescendantStatesToEnter() procedure.
     *
     * @param exctx The execution context for this step
     * @param step The step
     * @param tt The TransitionTarget
     */
    public void addDescendantStatesToEnter(final SCXMLExecutionContext exctx, final Step step,
                                              final TransitionTarget tt) {
        if (tt instanceof History) {
            History h = (History) tt;
            if (exctx.getScInstance().isEmpty(h)) {
                step.getDefaultHistoryTransitionEntryMap().put(h.getParent(), h.getTransition());
                for (TransitionTarget dtt : h.getTransition().getTargets()) {
                    addDescendantStatesToEnter(exctx, step, dtt);
                    addAncestorStatesToEnter(exctx, step, dtt, tt.getParent());
                }
            } else {
                for (TransitionTarget dtt : exctx.getScInstance().getLastConfiguration(h)) {
                    addDescendantStatesToEnter(exctx, step, dtt);
                    addAncestorStatesToEnter(exctx, step, dtt, tt.getParent());
                }
            }
        }
        else { // tt instanceof EnterableState
            EnterableState es = (EnterableState)tt;
            step.getEntrySet().add(es);
            if (es instanceof Parallel) {
                for (EnterableState child : ((Parallel)es).getChildren()) {
                    if (!containsDescendant(step.getEntrySet(), child)) {
                        addDescendantStatesToEnter(exctx, step, child);
                    }
                }
            }
            else if (es instanceof State && ((State) es).isComposite()) {
                step.getDefaultEntrySet().add(es);
                for (TransitionTarget dtt : ((State)es).getInitial().getTransition().getTargets()) {
                    addDescendantStatesToEnter(exctx, step, dtt);
                    addAncestorStatesToEnter(exctx, step, dtt, tt);
                }
            }
        }
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing addAncestorStatesToEnter() procedure.
     *
     * @param exctx The execution context for this step
     * @param step The step
     * @param tt The TransitionTarget
     * @param ancestor The ancestor TransitionTarget
     */
    public void addAncestorStatesToEnter(final SCXMLExecutionContext exctx, final Step step,
                                            final TransitionTarget tt, TransitionTarget ancestor) {
        // for for anc in getProperAncestors(tt,ancestor)
        for (int i = tt.getNumberOfAncestors()-1; i > -1; i--) {
            EnterableState anc = tt.getAncestor(i);
            if (anc == ancestor) {
                break;
            }
            step.getEntrySet().add(anc);
            if (anc instanceof Parallel) {
                for (EnterableState child : ((Parallel)anc).getChildren()) {
                    if (!containsDescendant(step.getEntrySet(), child)) {
                        addDescendantStatesToEnter(exctx, step, child);
                    }
                }

            }
        }
    }

    /**
     * @return Returns true if a member of the provided states set is a descendant of the provided state.
     * @param states the set of states to check for descendants
     * @param state the state to check with
     */
    public boolean containsDescendant(Set<EnterableState> states, EnterableState state) {
        for (EnterableState es : states) {
            if (es.isDescendantOf(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing selectTransitions() as well as the
     * selectEventlessTransitions() procedure, depending on the event (or null) in the provided step
     * <p>
     * @param exctx The execution context for this step
     * @param step The step
     */
    public void selectTransitions(final SCXMLExecutionContext exctx, final Step step) throws ModelException {
        step.getTransitList().clear();
        ArrayList<Transition> enabledTransitions = new ArrayList<Transition>();

        ArrayList<EnterableState> configuration = new ArrayList<EnterableState>(exctx.getScInstance().getCurrentStatus().getAllStates());
        Collections.sort(configuration,DocumentOrder.documentOrderComparator);

        HashSet<EnterableState> visited = new HashSet<EnterableState>();

        String eventName = step.getEvent() != null ? step.getEvent().getName() : null;
        for (EnterableState es : configuration) {
            if (es.isAtomicState()) {
                if (es instanceof Final) {
                    // Final states don't have transitions, skip to parent
                    if (es.getParent() == null) {
                        // should not happen: a top level active Final state should have stopped the state machine
                        throw new ModelException("Illegal state machine configuration: encountered top level <final> "
                                + "state while processing an event");
                    }
                    else {
                        es = es.getParent();
                    }
                }
                TransitionalState state = (TransitionalState)es;
                TransitionalState current = state;
                int ancestorIndex = state.getNumberOfAncestors()-1;
                boolean transitionMatched = false;
                do {
                    for (Transition transition : current.getTransitionsList()) {
                        if (transitionMatched = matchTransition(exctx, transition, eventName)) {
                            enabledTransitions.add(transition);
                            break;
                        }
                    }
                    current = (!transitionMatched && ancestorIndex > -1) ? state.getAncestor(ancestorIndex--) : null;
                } while (!transitionMatched && current != null && visited.add(current));
            }
        }
        removeConflictingTransitions(exctx, step, enabledTransitions);
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing removeConflictingTransitions() procedure.
     *
     * @param exctx The execution context for this step
     * @param step The step
     * @param enabledTransitions The list of enabled transitions
     */
    public void removeConflictingTransitions(final SCXMLExecutionContext exctx, final Step step,
                                             final List<Transition> enabledTransitions) {
        LinkedHashSet<Transition> filteredTransitions = new LinkedHashSet<Transition>();
        LinkedHashSet<Transition> preemptedTransitions = new LinkedHashSet<Transition>();
        Map<Transition, Set<EnterableState>> exitSets = new HashMap<Transition, Set<EnterableState>>();

        Set<EnterableState> configuration = exctx.getScInstance().getCurrentStatus().getAllStates();
        Collections.sort(enabledTransitions, DocumentOrder.documentOrderComparator);

        for (Transition t1 : enabledTransitions) {
            boolean t1Preempted = false;
            Set<EnterableState> t1ExitSet = exitSets.get(t1);
            for (Transition t2 : filteredTransitions) {
                if (t1ExitSet == null) {
                    t1ExitSet = new HashSet<EnterableState>();
                    computeExitSet(t1, t1ExitSet, configuration);
                    exitSets.put(t1, t1ExitSet);
                }
                Set<EnterableState> t2ExitSet = exitSets.get(t2);
                if (t2ExitSet == null) {
                    t2ExitSet = new HashSet<EnterableState>();
                    computeExitSet(t2, t2ExitSet, configuration);
                    exitSets.put(t2, t2ExitSet);
                }
                Set<EnterableState> smaller = t1ExitSet.size() < t2ExitSet.size() ? t1ExitSet : t2ExitSet;
                Set<EnterableState> larger = smaller == t1ExitSet ? t2ExitSet : t1ExitSet;
                boolean hasIntersection = false;
                for (EnterableState s1 : smaller) {
                    hasIntersection = larger.contains(s1);
                    if (hasIntersection) {
                        break;
                    }
                }
                if (hasIntersection) {
                    if (t1.getParent().isDescendantOf(t2.getParent())) {
                        preemptedTransitions.add(t2);
                    }
                    else {
                        t1Preempted = true;
                        break;
                    }
                }
            }
            if (t1Preempted) {
                exitSets.remove(t1);
            }
            else {
                for (Transition preempted : preemptedTransitions) {
                    filteredTransitions.remove(preempted);
                    exitSets.remove(preempted);
                }
                filteredTransitions.add(t1);
            }
        }
        step.getTransitList().addAll(filteredTransitions);
    }

    /**
     * @param exctx The execution context for this step
     * @param transition The transition
     * @param eventName The (optional) event name to match against
     * @return Returns true if the transition matches against the provided eventName, or is event-less when no eventName
     *         is provided, <em>AND</em> its (optional) condition guard evaluates to true.
     */
    public boolean matchTransition(final SCXMLExecutionContext exctx, final Transition transition, final String eventName) {
        if (eventName != null) {
            if (!(transition.isNoEventsTransition() || transition.isAllEventsTransition())) {
                boolean eventMatch = false;
                for (String event : transition.getEvents()) {
                    if (eventName.startsWith(event)) {
                        if (eventName.length() == event.length() || eventName.charAt(event.length())=='.')
                            eventMatch = true;
                        break;
                    }
                }
                if (!eventMatch) {
                    return false;
                }
            }
        }
        else if (!transition.isNoEventsTransition()) {
            return false;
        }
        if (transition.getCond() != null) {
            Boolean result = Boolean.FALSE;
            Context context = exctx.getScInstance().getContext(transition.getParent());
            context.setLocal(Context.NAMESPACES_KEY, transition.getNamespaces());
            try {
                if ((result = exctx.getEvaluator().evalCond(context, transition.getCond())) == null) {
                    result = Boolean.FALSE;
                    if (exctx.getAppLog().isDebugEnabled()) {
                        exctx.getAppLog().debug("Treating as false because the cond expression was evaluated as null: '"
                                + transition.getCond() + "'");
                    }
                }
            }
            catch (SCXMLExpressionException e) {
                exctx.getInternalIOProcessor().addEvent(new TriggerEvent(TriggerEvent.ERROR_EXECUTION, TriggerEvent.ERROR_EVENT));
                exctx.getErrorReporter().onError(ErrorConstants.EXPRESSION_ERROR, "Treating as false due to error: "
                        + e.getMessage(), transition);
            }
            finally {
                context.setLocal(Context.NAMESPACES_KEY, null);
            }
            return result;
        }
        return true;
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing isFinalState() function.
     *
     * @param es the enterable state to check
     * @param configuration the current state machine configuration
     * @return Return true if s is a compound state and one of its children is an active final state (i.e. is a member
     *         of the current configuration), or if s is a parallel state and isInFinalState is true of all its children.
     */
    public boolean isInFinalState(final EnterableState es, final Set<EnterableState> configuration) {
        if (es instanceof State) {
            for (EnterableState child : ((State)es).getChildren()) {
                if (child instanceof Final && configuration.contains(child)) {
                    return true;
                }
            }
        }
        else if (es instanceof Parallel) {
            for (EnterableState child : ((Parallel)es).getChildren()) {
                if (!isInFinalState(child, configuration)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if an external event was send (back) by an specific Invoker
     *
     * @param invokerId the invokerId
     *
     * @param event received external event
     * @return true if this event was send by the specific Invoker
     */
    public boolean isInvokerEvent(final String invokerId, final TriggerEvent event) {
        return event.getName().equals("done.invoke."+invokerId) ||
                event.getName().startsWith("done.invoke."+invokerId+".");
    }

    /**
     * Check if an external event indicates the state machine execution must be cancelled.
     *
     * @param event received external event
     * @return true if this event is of type {@link TriggerEvent#CANCEL_EVENT}.
     */
    public boolean isCancelEvent(TriggerEvent event) {
        return (event.getType() == TriggerEvent.CANCEL_EVENT);
    }

    /**
     * Checks whether a given set of states is a legal Harel State Table
     * configuration (with the respect to the definition of the OR and AND
     * states).
     *
     * @param states a set of states
     * @param errRep ErrorReporter to report detailed error info if needed
     * @return true if a given state configuration is legal, false otherwise
     */
    public boolean isLegalConfig(final Set<EnterableState> states, final ErrorReporter errRep) {
        /*
         * For every active state we add 1 to the count of its parent. Each
         * Parallel should reach count equal to the number of its children and
         * contribute by 1 to its parent. Each State should reach count exactly
         * 1. SCXML elemnt (top) should reach count exactly 1. We essentially
         * summarize up the hierarchy tree starting with a given set of
         * states = active configuration.
         */
        boolean legalConfig = true; // let's be optimists
        Map<EnterableState, Set<EnterableState>> counts = new HashMap<EnterableState, Set<EnterableState>>();
        Set<EnterableState> scxmlCount = new HashSet<EnterableState>();
        for (EnterableState es : states) {
            EnterableState parent;
            while ((parent = es.getParent()) != null) {
                Set<EnterableState> cnt = counts.get(parent);
                if (cnt == null) {
                    cnt = new HashSet<EnterableState>();
                    counts.put(parent, cnt);
                }
                cnt.add(es);
                es = parent;
            }
            //top-level contribution
            scxmlCount.add(es);
        }
        if (scxmlCount.size() > 1) {
            errRep.onError(ErrorConstants.ILLEGAL_CONFIG, "Multiple top-level OR states active!", scxmlCount);
        }
        //Validate child counts:
        for (Map.Entry<EnterableState, Set<EnterableState>> entry : counts.entrySet()) {
            EnterableState es = entry.getKey();
            Set<EnterableState> count = entry.getValue();
            if (es instanceof Parallel) {
                Parallel p = (Parallel) es;
                if (count.size() < p.getChildren().size()) {
                    errRep.onError(ErrorConstants.ILLEGAL_CONFIG, "Not all AND states active for parallel " + p.getId(), entry);
                    legalConfig = false;
                }
            } else {
                if (count.size() > 1) {
                    errRep.onError(ErrorConstants.ILLEGAL_CONFIG, "Multiple OR states active for state " + es.getId(), entry);
                    legalConfig = false;
                }
            }
            count.clear(); //cleanup
        }
        //cleanup
        scxmlCount.clear();
        counts.clear();
        return legalConfig;
    }

    /**
     * Stores the current state machine configuration ({@link Status#getAllStates()}) in the system context.
     * @param scInstance the state machine instance holding the current configuration
     */
    public void setSystemAllStatesVariable(final SCInstance scInstance) {
        Status currentStatus = scInstance.getCurrentStatus();
        scInstance.getSystemContext().setLocal(SCXMLSystemContext.ALL_STATES_KEY, currentStatus.getAllStates());
    }

    /**
     * Stores the provided event in the system context
     * <p>
     * For the event a EventVariable is instantiated and the provided event its type is mapped to the one of the
     * SCXML specification predefined types.
     * </p>
     * @param scInstance the state machine instance holding the system context
     * @param event The event being stored
     * @param internal Flag indicating the event was received internally or externally
     */
    public void setSystemEventVariable(final SCInstance scInstance, final TriggerEvent event, boolean internal) {
        Context systemContext = scInstance.getSystemContext();
        EventVariable eventVar = null;
        if (event != null) {
            String eventType = internal ? EventVariable.TYPE_INTERNAL : EventVariable.TYPE_EXTERNAL;

            final int triggerEventType = event.getType();
            if (triggerEventType == TriggerEvent.ERROR_EVENT || triggerEventType == TriggerEvent.CHANGE_EVENT) {
                eventType = EventVariable.TYPE_PLATFORM;
            }

            // TODO: determine sendid, origin, originType and invokeid based on context later.
            eventVar = new EventVariable(event.getName(), eventType, null, null, null, null, event.getPayload());
        }
        systemContext.setLocal(SCXMLSystemContext.EVENT_KEY, eventVar);
    }

    /**
     * Executes the global SCXML script element
     *
     * @param exctx The execution context
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void executeGlobalScript(final SCXMLExecutionContext exctx) throws ModelException {
        Script globalScript = exctx.getStateMachine().getGlobalScript();
        if ( globalScript != null) {
            try {
                globalScript.execute(exctx.getActionExecutionContext());
            } catch (SCXMLExpressionException e) {
                exctx.getInternalIOProcessor().addEvent(new TriggerEvent(TriggerEvent.ERROR_EXECUTION, TriggerEvent.ERROR_EVENT));
                exctx.getErrorReporter().onError(ErrorConstants.EXPRESSION_ERROR, e.getMessage(), exctx.getStateMachine());
            }
        }
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing exitStates() procedure, where the states to exit
     * already have been pre-computed in {@link #microStep(SCXMLExecutionContext, Step, java.util.Set)}.
     *
     * @param exctx The execution context for this micro step
     * @param step the step
     * @param statesToInvoke the set of activated states which invokes need to be invoked at the end of the current
     *                       macro step
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void exitStates(final SCXMLExecutionContext exctx, final Step step,
                           final Set<TransitionalState> statesToInvoke)
            throws ModelException {
        if (step.getExitSet().isEmpty()) {
            return;
        }
        Set<EnterableState> configuration = null;
        ArrayList<EnterableState> exitList = new ArrayList<EnterableState>(step.getExitSet());
        Collections.sort(exitList, DocumentOrder.reverseDocumentOrderComparator);

        for (EnterableState es : exitList) {

            if (es instanceof TransitionalState && ((TransitionalState)es).hasHistory()) {
                TransitionalState ts = (TransitionalState)es;
                Set<EnterableState> shallow = null;
                Set<EnterableState> deep = null;
                for (History h : ts.getHistory()) {
                    if (h.isDeep()) {
                        if (deep == null) {
                            //calculate deep history for a given state once
                            deep = new HashSet<EnterableState>();
                            for (EnterableState ott : exctx.getScInstance().getCurrentStatus().getStates()) {
                                if (ott.isDescendantOf(es)) {
                                    deep.add(ott);
                                }
                            }
                        }
                        exctx.getScInstance().setLastConfiguration(h, deep);
                    } else {
                        if (shallow == null) {
                            //calculate shallow history for a given state once
                            if (configuration == null) {
                                configuration = exctx.getScInstance().getCurrentStatus().getAllStates();
                            }
                            shallow = new HashSet<EnterableState>(ts.getChildren());
                            shallow.retainAll(configuration);
                        }
                        exctx.getScInstance().setLastConfiguration(h, shallow);
                    }
                }
            }

            boolean onexitEventRaised = false;
            for (OnExit onexit : es.getOnExits()) {
                executeContent(exctx, onexit);
                if (!onexitEventRaised && onexit.isRaiseEvent()) {
                    onexitEventRaised = true;
                    exctx.getInternalIOProcessor().addEvent(new TriggerEvent("exit.state."+es.getId(), TriggerEvent.CHANGE_EVENT));
                }
            }
            exctx.getNotificationRegistry().fireOnExit(es, es);
            exctx.getNotificationRegistry().fireOnExit(exctx.getStateMachine(), es);

            if (es instanceof TransitionalState && !statesToInvoke.remove(es)) {
                // check if invokers are active in this state
                for (Invoke inv : ((TransitionalState)es).getInvokes()) {
                    exctx.cancelInvoker(inv);
                }
            }

        }
        exctx.getScInstance().getCurrentStatus().getStates().removeAll(exitList);
    }

    /**
     * Executes the executable content for all transitions in the micro step
     *
     * @param exctx The execution context for this micro step
     * @param step the step
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void executeTransitionContent(final SCXMLExecutionContext exctx, final Step step) throws ModelException {
        for (SimpleTransition transition : step.getTransitList()) {
            executeContent(exctx, transition);
        }
    }

    /**
     * Executes the executable content for a specific executable in the micro step
     *
     * @param exctx The execution context for this micro step
     * @param exec the executable providing the execution content
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void executeContent(SCXMLExecutionContext exctx, Executable exec) throws ModelException {
        try {
            for (Action action : exec.getActions()) {
                action.execute(exctx.getActionExecutionContext());
            }
        } catch (SCXMLExpressionException e) {
            exctx.getInternalIOProcessor().addEvent(new TriggerEvent(TriggerEvent.ERROR_EXECUTION, TriggerEvent.ERROR_EVENT));
            exctx.getErrorReporter().onError(ErrorConstants.EXPRESSION_ERROR, e.getMessage(), exec);
        }
        if (exec instanceof Transition) {
            Transition t = (Transition)exec;
            if (t.getTargets().isEmpty()) {
                notifyOnTransition(exctx, t, t.getParent());
            }
            else {
                for (TransitionTarget tt : t.getTargets()) {
                    notifyOnTransition(exctx, t, tt);
                }
            }
        }
    }

    /**
     * Notifies SCXMLListeners on the transition taken
     *
     * @param exctx The execution context for this micro step
     * @param t The Transition taken
     * @param target The target of the Transition
     */
    public void notifyOnTransition(final SCXMLExecutionContext exctx, final Transition t,
                                      final TransitionTarget target) {
        EventVariable event = (EventVariable)exctx.getScInstance().getSystemContext().getVars().get(SCXMLSystemContext.EVENT_KEY);
        String eventName = event != null ? event.getName() : null;
        exctx.getNotificationRegistry().fireOnTransition(t, t.getParent(), target, t, eventName);
        exctx.getNotificationRegistry().fireOnTransition(exctx.getStateMachine(), t.getParent(), target, t, eventName);
    }

    /**
     * This method corresponds to the Algorithm for SCXML processing enterStates() procedure, where the states to enter
     * already have been pre-computed in {@link #microStep(SCXMLExecutionContext, Step, java.util.Set)}.
     *
     * @param exctx The execution context for this micro step
     * @param step the step
     * @param statesToInvoke the set of activated states which invokes need to be invoked at the end of the current
     *                       macro step
     * @throws ModelException if a SCXML model error occurred during the execution.
     */
    public void enterStates(final SCXMLExecutionContext exctx, final Step step,
                            final Set<TransitionalState> statesToInvoke)
            throws ModelException {
        if (step.getEntrySet().isEmpty()) {
            return;
        }
        Set<EnterableState> configuration = null;
        ArrayList<EnterableState> entryList = new ArrayList<EnterableState>(step.getEntrySet());
        Collections.sort(entryList, DocumentOrder.documentOrderComparator);
        for (EnterableState es : entryList) {
            if (es.isAtomicState()) {
                // only track actomic active states in Status
                exctx.getScInstance().getCurrentStatus().getStates().add(es);
            }
            if (es instanceof TransitionalState && !((TransitionalState)es).getInvokes().isEmpty()) {
                statesToInvoke.add((TransitionalState) es);
            }

            boolean onentryEventRaised = false;
            for (OnEntry onentry : es.getOnEntries()) {
                executeContent(exctx, onentry);
                if (!onentryEventRaised && onentry.isRaiseEvent()) {
                    onentryEventRaised = true;
                    exctx.getInternalIOProcessor().addEvent(new TriggerEvent("entry.state."+es.getId(), TriggerEvent.CHANGE_EVENT));
                }
            }
            exctx.getNotificationRegistry().fireOnEntry(es, es);
            exctx.getNotificationRegistry().fireOnEntry(exctx.getStateMachine(), es);

            if (es instanceof State && step.getDefaultEntrySet().contains(es) && ((State)es).getInitial() != null) {
                executeContent(exctx, ((State)es).getInitial().getTransition());
            }
            if (es instanceof TransitionalState) {
                SimpleTransition hTransition = step.getDefaultHistoryTransitionEntryMap().get(es);
                if (hTransition != null) {
                    executeContent(exctx, hTransition);
                }
            }

            if (es instanceof Final) {
                State parent = (State)es.getParent();
                if (parent == null) {
                    exctx.stopRunning();
                }
                else {
                    exctx.getInternalIOProcessor().addEvent(new TriggerEvent("done.state."+parent.getId(),TriggerEvent.CHANGE_EVENT));
                    if (parent.isRegion()) {
                        if (configuration == null) {
                            // Note: configuration may 'grow' during enterStates, but activation works downwards
                            //       so it doesn't matter for determining if the parallel children are already
                            //       all in final state, or may become so further down (which at that time will
                            //       trigger the parallel parent done event)
                            configuration = exctx.getScInstance().getCurrentStatus().getAllStates();
                        }
                        if (isInFinalState(parent.getParent(), configuration)) {
                            exctx.getInternalIOProcessor().addEvent(new TriggerEvent("done.state."+parent.getParent().getId()
                                    , TriggerEvent.CHANGE_EVENT));
                        }
                    }
                }
            }
        }
    }

    /**
     * Initiate any new invoked activities.
     *
     * @param exctx provides the execution context
     * @param statesToInvoke the set of activated states which invokes need to be invoked
     */
    public void initiateInvokes(final SCXMLExecutionContext exctx,
                                final Set<TransitionalState> statesToInvoke) {
        SCInstance scInstance = exctx.getScInstance();
        Evaluator eval = exctx.getEvaluator();
        for (TransitionalState ts : statesToInvoke) {
            if (ts.getInvokes().isEmpty()) {
                continue;
            }
            Context context = scInstance.getContext(ts);
            for (Invoke i : ts.getInvokes()) {
                String src = i.getSrc();
                if (src == null) {
                    String srcexpr = i.getSrcexpr();
                    Object srcObj;
                    try {
                        context.setLocal(Context.NAMESPACES_KEY, i.getNamespaces());
                        srcObj = eval.eval(context, srcexpr);
                        context.setLocal(Context.NAMESPACES_KEY, null);
                        src = String.valueOf(srcObj);
                    } catch (SCXMLExpressionException see) {
                        exctx.getInternalIOProcessor().addEvent(new TriggerEvent(TriggerEvent.ERROR_EXECUTION, TriggerEvent.ERROR_EVENT));
                        exctx.getErrorReporter().onError(ErrorConstants.EXPRESSION_ERROR, see.getMessage(), i);
                    }
                }
                String source = src;
                PathResolver pr = i.getPathResolver();
                if (pr != null) {
                    source = i.getPathResolver().resolvePath(src);
                }
                Invoker inv;
                try {
                    inv = exctx.newInvoker(i.getType());
                } catch (InvokerException ie) {
                    exctx.getInternalIOProcessor().addEvent(new TriggerEvent("failed.invoke."+ts.getId(), TriggerEvent.ERROR_EVENT));
                    continue;
                }
                List<Param> params = i.params();
                Map<String, Object> args = new HashMap<String, Object>();
                for (Param p : params) {
                    String argExpr = p.getExpr();
                    Object argValue = null;
                    context.setLocal(Context.NAMESPACES_KEY, p.getNamespaces());
                    // Do we have an "expr" attribute?
                    if (argExpr != null && argExpr.trim().length() > 0) {
                        try {
                            argValue = eval.eval(context, argExpr);
                        } catch (SCXMLExpressionException see) {
                            exctx.getInternalIOProcessor().addEvent(new TriggerEvent(TriggerEvent.ERROR_EXECUTION, TriggerEvent.ERROR_EVENT));
                            exctx.getErrorReporter().onError(ErrorConstants.EXPRESSION_ERROR, see.getMessage(), i);
                        }
                    } else {
                        // No. Does value of "name" attribute refer to a valid
                        // location in the data model?
                        try {
                            argValue = eval.evalLocation(context, p.getName());
                            if (argValue == null) {
                                // Generate error, 4.3.1 in WD-scxml-20080516
                                exctx.getInternalIOProcessor().addEvent(new TriggerEvent(ts.getId() + ERR_ILLEGAL_ALLOC, TriggerEvent.ERROR_EVENT));
                            }
                        } catch (SCXMLExpressionException see) {
                            exctx.getInternalIOProcessor().addEvent(new TriggerEvent(TriggerEvent.ERROR_EXECUTION, TriggerEvent.ERROR_EVENT));
                            exctx.getErrorReporter().onError(ErrorConstants.EXPRESSION_ERROR, see.getMessage(), i);
                        }
                    }
                    context.setLocal(Context.NAMESPACES_KEY, null);
                    args.put(p.getName(), argValue);
                }
                String invokeId = exctx.setInvoker(i, inv);
                inv.setInvokeId(invokeId);
                inv.setParentIOProcessor(exctx.getExternalIOProcessor());
                inv.setEvaluator(exctx.getEvaluator());
                try {
                    inv.invoke(source, args);
                } catch (InvokerException ie) {
                    exctx.getInternalIOProcessor().addEvent(new TriggerEvent("failed.invoke."+ts.getId(), TriggerEvent.ERROR_EVENT));
                    exctx.removeInvoker(i);
                }
            }
        }
    }

    /**
     * Forward events to invoked activities, execute finalize handlers.
     *
     * @param exctx provides the execution context
     * @param event The events to be forwarded
     *
     * @throws ModelException in case there is a fatal SCXML object model problem.
     */
    public void processInvokes(final SCXMLExecutionContext exctx, final TriggerEvent event) throws ModelException {
        for (Map.Entry<Invoke, String> entry : exctx.getInvokeIds().entrySet()) {
            if (!isInvokerEvent(entry.getValue(), event)) {
                if (entry.getKey().isAutoForward()) {
                    Invoker inv = exctx.getInvoker(entry.getKey());
                    try {
                        inv.parentEvent(event);
                    } catch (InvokerException ie) {
                        exctx.getAppLog().error(ie.getMessage(), ie);
                        throw new ModelException(ie.getMessage(), ie.getCause());
                    }
                }
            }
            /*
            else {
                // TODO: applyFinalize
            }
            */
        }
    }
}

