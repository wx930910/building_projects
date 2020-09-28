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
package org.apache.commons.scxml2.env;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.w3c.dom.Node;

/**
 * <p>EventDispatcher implementation that can schedule <code>delay</code>ed
 * &lt;send&gt; events for the &quot;scxml&quot; <code>type</code>
 * attribute value (which is also the default). This implementation uses
 * J2SE <code>Timer</code>s.</p>
 *
 * <p>No other <code>type</code>s are processed. Subclasses may support
 * additional <code>type</code>s by overriding the
 * <code>send(...)</code> and <code>cancel(...)</code> methods and
 * delegating to their <code>super</code> counterparts for the
 * &quot;scxml&quot; <code>type</code>.</p>
 *
 */
public class SimpleScheduler implements EventDispatcher, Serializable {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Log instance. */
    private Log log = LogFactory.getLog(SimpleScheduler.class);

    /**
     * The <code>Map</code> of active <code>Timer</code>s, keyed by
     * &lt;send&gt; element <code>id</code>s.
     */
    private Map<String, Timer> timers;

    /**
     * The state chart execution instance we schedule events for.
     */
    private SCXMLExecutor executor;

    /**
     * Constructor.
     *
     * @param executor The owning {@link SCXMLExecutor} instance.
     */
    public SimpleScheduler(final SCXMLExecutor executor) {
        super();
        this.executor = executor;
        this.timers = Collections.synchronizedMap(new HashMap<String, Timer>());
    }

    /**
     * @see EventDispatcher#cancel(String)
     */
    public void cancel(final String sendId) {
        // Log callback
        if (log.isInfoEnabled()) {
            log.info("cancel( sendId: " + sendId + ")");
        }
        if (!timers.containsKey(sendId)) {
            return; // done, we don't track this one or its already expired
        }
        Timer timer = timers.get(sendId);
        if (timer != null) {
            timer.cancel();
            if (log.isDebugEnabled()) {
                log.debug("Cancelled event scheduled by <send> with id '"
                    + sendId + "'");
            }
        }
        timers.remove(sendId);
    }

    /**
    @see EventDispatcher#send(String,String,String,String,Map,Object,long,List)
     */
    public void send(final String sendId, final String target,
            final String type, final String event,
            final Map<String, Object> params, final Object hints, final long delay,
            final List<Node> externalNodes) {
        // Log callback
        if (log.isInfoEnabled()) {
            StringBuffer buf = new StringBuffer();
            buf.append("send ( sendId: ").append(sendId);
            buf.append(", target: ").append(target);
            buf.append(", type: ").append(type);
            buf.append(", event: ").append(event);
            buf.append(", params: ").append(String.valueOf(params));
            buf.append(", hints: ").append(String.valueOf(hints));
            buf.append(", delay: ").append(delay);
            buf.append(')');
            log.info(buf.toString());
        }

        // We only handle the "scxml" type (which is the default too)
        if (type == null || type.equalsIgnoreCase(TYPE_SCXML)) {

            if (target != null) {
                // We know of no other target
                if (log.isWarnEnabled()) {
                    log.warn("<send>: Unavailable target - " + target);
                }
                try {
                    this.executor.triggerEvent(new TriggerEvent(
                        EVENT_ERR_SEND_TARGETUNAVAILABLE,
                        TriggerEvent.ERROR_EVENT));
                } catch (ModelException me) {
                    log.error(me.getMessage(), me);
                }
                return; // done
            }

            if (delay > 0L) {
                // Need to schedule this one
                Timer timer = new Timer(true);
                timer.schedule(new DelayedEventTask(sendId, event, params), delay);
                timers.put(sendId, timer);
                if (log.isDebugEnabled()) {
                    log.debug("Scheduled event '" + event + "' with delay "
                        + delay + "ms, as specified by <send> with id '"
                        + sendId + "'");
                }
            }
            // else short-circuited by Send#execute()
            // TODO: Pass through in v1.0

        }

    }

    /**
     * Get the log instance.
     *
     * @return The current log instance
     */
    protected Log getLog() {
        return log;
    }

    /**
     * Get the current timers.
     *
     * @return The currently scheduled timers
     */
    protected Map<String, Timer> getTimers() {
        return timers;
    }

    /**
     * Get the executor we're attached to.
     *
     * @return The owning executor instance
     */
    protected SCXMLExecutor getExecutor() {
        return executor;
    }

    /**
     * TimerTask implementation.
     */
    class DelayedEventTask extends TimerTask {

        /**
         * The ID of the &lt;send&gt; element.
         */
        private String sendId;

        /**
         * The event name.
         */
        private String event;

        /**
         * The event payload, if any.
         */
        private Map<String, Object> payload;

        /**
         * Constructor.
         *
         * @param sendId The ID of the send element.
         * @param event The name of the event to be triggered.
         */
        DelayedEventTask(final String sendId, final String event) {
            this(sendId, event, null);
        }

        /**
         * Constructor for events with payload.
         *
         * @param sendId The ID of the send element.
         * @param event The name of the event to be triggered.
         * @param payload The event payload, if any.
         */
        DelayedEventTask(final String sendId, final String event,
                final Map<String, Object> payload) {
            super();
            this.sendId = sendId;
            this.event = event;
            this.payload = payload;
        }

        /**
         * What to do when timer expires.
         */
        @Override
        public void run() {
            timers.remove(sendId);
            try {
                executor.triggerEvent(new TriggerEvent(event,
                    TriggerEvent.SIGNAL_EVENT, payload));
            } catch (ModelException me) {
                log.error(me.getMessage(), me);
            }
            if (log.isDebugEnabled()) {
                log.debug("Fired event '" + event + "' as scheduled by "
                    + "<send> with id '" + sendId + "'");
            }
        }

    }

    /**
     * The default target type.
     */
    private static final String TYPE_SCXML = "scxml";

    /**
     * The spec mandated derived event when target cannot be reached.
     */
    private static final String EVENT_ERR_SEND_TARGETUNAVAILABLE =
        "error.send.targetunavailable";

}

