/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * A script scope, stores the declaration of parameters and local variables as symbols.
 * @since 3.0
 */
public final class Scope {    
    /**
     * The value of a declared but undefined variable, for instance: var x;.
     */
    private static final Object UNDEFINED = new Object() {
        @Override public String toString() {
            return "?";
        }
    };
    /**
     * The parent scope.
     */
    private final Scope parent;
    /**
     * The number of parameters.
     */
    private int parms;
    /**
     * If params have variable number.
     */
    private boolean varArgs;
    /**
     * The number of local variables.
     */
    private int vars;
    /**
     * The map of named variables aka script parameters and local variables.
     * Each parameter is associated to a symbol and is materialized as an offset in the stacked array used
     * during evaluation.
     */
    private Map<String, Integer> namedVariables = null;
    /**
     * The map of local hoisted variables to parent scope variables, ie closure.
     */
    private Map<Integer, Integer> hoistedVariables = null;
    /**
     * The map of variable types.
     * Each variable may be associated with specific type
     */
    private Map<Integer, Class> variableTypes = null;
    /**
     * The set of final variables.
     */
    private Set<Integer> finalVariables = null;
    /**
     * The set of non-null variables.
     */
    private Set<Integer> requiredVariables = null;
    /**
     * The empty string array.
     */
    private static final String[] EMPTY_STRS = new String[0];

    /**
     * Creates a new scope with a list of parameters.
     * @param scope the parent scope if any
     * @param parameters the list of parameters
     */
    public Scope(Scope scope, String... parameters) {
        if (parameters != null) {
            parms = parameters.length;
            namedVariables = new LinkedHashMap<String, Integer>();
            for (int p = 0; p < parms; ++p) {
                namedVariables.put(parameters[p], p);
            }
        } else {
            parms = 0;
        }
        vars = 0;
        parent = scope;
    }

    @Override
    public int hashCode() {
        return namedVariables == null ? 0 : parms ^ namedVariables.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Scope)) {
            return false;
        }
        Scope scope = (Scope) o;
        if (parms != scope.parms) {
            return false;
        }
        if (varArgs != scope.varArgs) {
            return false;
        }
        if (namedVariables == null && scope.namedVariables != null) {
            return false;
        }
        if (namedVariables != null && !namedVariables.equals(scope.namedVariables)) {
            return false;
        }
        if (variableTypes == null && scope.variableTypes != null) {
            return false;
        }
        if (variableTypes != null && !variableTypes.equals(scope.variableTypes)) {
            return false;
        }
        if (finalVariables == null && scope.finalVariables != null) {
            return false;
        }
        if (finalVariables != null && !finalVariables.equals(scope.finalVariables)) {
            return false;
        }
        if (requiredVariables == null && scope.requiredVariables != null) {
            return false;
        }
        if (requiredVariables != null && !requiredVariables.equals(scope.requiredVariables)) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * If this fails, attempt to solve by hoisting parent stacked.
     * @param name the symbol name
     * @return the symbol index
     */
    public Integer getSymbol(String name) {
        return getSymbol(name, true);
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * @param name the symbol name
     * @param hoist whether solving by hoisting parent stacked is allowed
     * @return the symbol index
     */
    public Integer getSymbol(String name, boolean hoist) {
        Integer register = namedVariables != null ? namedVariables.get(name) : null;
        if (register == null && hoist && parent != null) {
            Integer pr = parent.getSymbol(name, true);
            if (pr != null) {
                if (hoistedVariables == null) {
                    hoistedVariables = new LinkedHashMap<Integer, Integer>();
                }
                if (namedVariables == null) {
                    namedVariables = new LinkedHashMap<String, Integer>();
                }
                register = namedVariables.size();
                namedVariables.put(name, register);
                hoistedVariables.put(register, pr);
                Class type = parent.getVariableType(pr);
                if (type != null) {
                    if (variableTypes == null) {
                        variableTypes = new HashMap<Integer, Class>();
                    }
                    variableTypes.put(register, type);
                }
                // Make hoisted variables final by default
                if (finalVariables == null) {
                    finalVariables = new HashSet<Integer>();
                }
                finalVariables.add(register);
            }
        }
        return register;
    }

    /**
     * Checks whether a given symbol is hoisted.
     * @param symbol the symbol number
     * @return true if hoisted, false otherwise
     */
    public boolean isHoistedSymbol(int symbol) {
        return hoistedVariables != null && hoistedVariables.containsKey(symbol);
    }

    /**
     * Returns the local variable type if any.
     * @param symbol the symbol index
     * @return the variable class 
     */
    public Class getVariableType(int symbol) {
        return variableTypes == null ? null : variableTypes.get(symbol);
    }

    /**
     * Returns if the local variable is declared final.
     * @param symbol the symbol index
     * @return true if final, false otherwise
     */
    public boolean isVariableFinal(int symbol) {
        return finalVariables == null ? false : finalVariables.contains(symbol);
    }

    /**
     * Returns if the local variable is declared non-null.
     * @param symbol the symbol index
     * @return true if non-null, false otherwise
     */
    public boolean isVariableRequired(int symbol) {
        return requiredVariables == null ? false : requiredVariables.contains(symbol);
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the parameter name
     */
    public void declareParameter(String name) {
        declareParameter(name, null, false, false);
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the parameter name
     * @param type the parameter class
     * @param isFinal if the declared parameter is final
     * @param isRequired if the declared parameter is non-null
     */
    public void declareParameter(String name, Class type, boolean isFinal, boolean isRequired) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        } else if (vars > 0) {
            throw new IllegalStateException("cant declare parameters after variables");
        }
        Integer register = namedVariables.get(name);
        if (register == null) {
            register = namedVariables.size();
            namedVariables.put(name, register);
            parms += 1;
            if (type != null) {
                if (variableTypes == null) {
                    variableTypes = new HashMap<Integer, Class>();
                }
                variableTypes.put(register, type);
            }
            if (isFinal) {
                if (finalVariables == null) {
                    finalVariables = new HashSet<Integer>();
                }
                finalVariables.add(register);
            }
            if (isRequired) {
                if (requiredVariables == null) {
                    requiredVariables = new HashSet<Integer>();
                }
                requiredVariables.add(register);
            }
        }
    }

    /**
     * Declares a scope to support variable parameters.
     */
    public void declareVarArgs() {
        varArgs = true;
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the variable name
     * @return the register index storing this variable
     */
    public Integer declareVariable(String name) {
        return declareVariable(name, null, false, false);
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the variable name
     * @param type the variable class
     * @param isFinal if the declared variable is final
     * @param isRequired if the declared variable is non-null
     * @return the register index storing this variable
     */
    public Integer declareVariable(String name, Class type, boolean isFinal, boolean isRequired) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        }
        Integer register = namedVariables.get(name);
        if (register == null) {
            register = namedVariables.size();
            namedVariables.put(name, register);
            vars += 1;
            if (type != null) {
                if (variableTypes == null) {
                    variableTypes = new HashMap<Integer, Class>();
                }
                variableTypes.put(register, type);
            }
            if (isFinal) {
                if (finalVariables == null) {
                    finalVariables = new HashSet<Integer>();
                }
                finalVariables.add(register);
            }
            if (isRequired) {
                if (requiredVariables == null) {
                    requiredVariables = new HashSet<Integer>();
                }
                requiredVariables.add(register);
            }
        } else {
            if (isVariableFinal(register)) {
                throw new IllegalStateException("final variable can not be redeclared");
            }
            if (type != null) {
                if (variableTypes == null) {
                    variableTypes = new HashMap<Integer, Class>();
                }
                variableTypes.put(register, type);
            }
            if (isFinal) {
                if (finalVariables == null) {
                    finalVariables = new HashSet<Integer>();
                }
                finalVariables.add(register);
            }
            if (isRequired) {
                if (requiredVariables == null) {
                    requiredVariables = new HashSet<Integer>();
                }
                requiredVariables.add(register);
            }
        }
        return register;
    }

    /**
     * Creates a frame by copying values up to the number of parameters.
     * <p>This captures the hoisted variables values.</p>
     * @param frame the caller frame
     * @return the arguments array
     */
    public Frame createFrame(Frame frame) {
        if (namedVariables != null) {
            Object[] arguments = new Object[namedVariables.size()];
            Arrays.fill(arguments, UNDEFINED);
            if (frame != null && hoistedVariables != null && parent != null) {
                for (Map.Entry<Integer, Integer> hoist : hoistedVariables.entrySet()) {
                    Integer target = hoist.getKey();
                    Integer source = hoist.getValue();
                    Object arg = frame.get(source);
                    arguments[target] = arg;
                }
            }
            return new Frame(this, arguments, 0);
        } else {
            return null;
        }
    }

    /**
     * Gets the hoisted index of a given symbol, ie the target index of a symbol in a child frame.
     * @param symbol the symbol index
     * @return the target symbol index or null if the symbol is not hoisted
     */
    public Integer getHoisted(int symbol) {
        if (hoistedVariables != null) {
            for (Map.Entry<Integer, Integer> hoist : hoistedVariables.entrySet()) {
                Integer source = hoist.getValue();
                if (source == symbol) {
                    return hoist.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Gets the (maximum) number of arguments this script expects.
     * @return the number of parameters
     */
    public int getArgCount() {
        return parms;
    }

    /**
     * If this script expects a variable number of arguments.
     * @return true or false
     */
    public boolean isVarArgs() {
        return varArgs;
    }

    /**
     * Gets this script symbols names, i.e. parameters and local variables.
     * @return the symbol names
     */
    public String[] getSymbols() {
        return namedVariables != null ? namedVariables.keySet().toArray(new String[0]) : EMPTY_STRS;
    }

    /**
     * Gets this script parameters, i.e. symbols assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        return getParameters(0);
    }
        
    /**
     * Gets this script parameters.
     * @param bound number of known bound parameters (curry)
     * @return the parameter names
     */
    protected String[] getParameters(int bound) {
        int unbound = parms - bound;
        if (namedVariables != null && unbound > 0) {
            String[] pa = new String[unbound];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
                int argn = entry.getValue();
                if (argn >= bound && argn < parms) {
                    pa[p++] = entry.getKey();
                }
            }
            return pa;
        } else {
            return EMPTY_STRS;
        }
    }

    /**
     * Gets this script local variable, i.e. symbols assigned to local variables excluding hoisted variables.
     * @return the local variable names
     */
    public String[] getLocalVariables() {
        if (namedVariables != null && vars > 0) {
            String[] pa = new String[parms - (hoistedVariables == null? 0 : hoistedVariables.size())];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
                int symnum = entry.getValue();
                if (symnum >= parms && (hoistedVariables == null || !hoistedVariables.containsKey(symnum))) {
                    pa[p++] = entry.getKey();
                }
            }
            return pa;
        } else {
            return EMPTY_STRS;
        }
    }

    /**
     * A variable modifier, stores additional variable attributes.
     * @since 3.2
     */
    protected static final class VariableModifier {
        /** The var 'final' modifier. */
        private final boolean isFinal;
        /** The var 'required' modifier. */
        private final boolean isRequired;
        /** The var 'type' modifier. */
        private final Class type;

        /**
         * Creates a new variable modifier.
         * @param c the variable type
         * @param fin whether the variable is final
         * @param req whether the variable is required
         */
        protected VariableModifier(Class c, boolean fin, boolean req) {
            type = c;
            isFinal = fin;
            isRequired = req;
        }

        public Class getType() {
            return type;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public boolean isRequired() {
            return isRequired;
        }

        /**
         * Creates a clone of this modifier.
         * @return new modifier
         */
        public VariableModifier clone() {
            return new VariableModifier(this.type, this.isFinal, this.isRequired);
        }
    }

    /**
     * A call frame, created from a scope, stores the arguments and local variables in a "stack frame" (sic).
     * @since 3.0
     */
    public static final class Frame {
        /** The scope. */
        private final Scope scope;
        /** The actual var modifiers. */
        private final VariableModifier[] modifiers;
        /** The actual stack frame. */
        private final Object[] stack;
        /** Number of curried parameters. */
        private int curried = 0;

        /**
         * Creates a new frame.
         * @param s the scope
         * @param r the stack frame
         * @param c the number of curried parameters
         */
        protected Frame(Scope s, Object[] r, int c) {
            scope = s;
            stack = r;
            curried = c;

            modifiers = stack != null ? new VariableModifier[stack.length] : null;
        }

        /**
         * Creates a new frame.
         * @param f the parent frame
         */
        protected Frame(Frame f, Object... values) {
            scope = f.scope;
            stack = f.stack != null ? f.stack.clone() : null;
            curried = f.curried;

            modifiers = f.modifiers != null ? f.modifiers.clone() : null;

            if (stack != null) {
                int nparm = scope.getArgCount();
                int ncopy = 0;
                if (values != null && values.length > 0) {
                    ncopy = Math.min(nparm - curried, Math.min(nparm, values.length));
                    System.arraycopy(values, 0, stack, curried, ncopy);
                }
                // unbound parameters are defined as null
                Arrays.fill(stack, curried + ncopy, nparm, null);
                curried += ncopy;
            }
        }

        /**
         * Gets this script unbound parameters, i.e. parameters not bound through curry().
         * @return the parameter names
         */
        public String[] getUnboundParameters() {
            return scope.getParameters(curried);
        }

        /**
         * Gets the scope.
         * @return this frame scope
         */
        public Scope getScope() {
            return scope;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(this.stack);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Frame other = (Frame) obj;
            return Arrays.deepEquals(this.stack, other.stack);
        }

        /**
         * Gets a value.
         * @param s the offset in this frame
         * @return the stacked value
         */
        public Object get(int s) {
            return stack[s];
        }
        
        /**
         * Whether this frame defines a symbol, ie declared it and assigned it a value.
         * @param s the offset in this frame
         * @return true if this symbol has been assigned a value, false otherwise
         */
        public boolean has(int s) {
            return s >= 0 && s < stack.length && stack[s] != UNDEFINED;
        }
            
        /**
         * Sets a value.
         * @param r the offset in this frame
         * @param value the value to set in this frame
         */
        public void set(int r, Object value) {
            stack[r] = value;
        }

        /**
         * Sets a variable modifiers.
         * @param r the offset in this frame
         * @param c the variable type
         * @param fin whether the variable is final
         * @param req whether the variable is required
         */
        public void setModifiers(int r, Class c, boolean fin, boolean req) {
            modifiers[r] = new VariableModifier(c, fin, req);
        }

        /**
         * Gets a symbol type.
         * @param s the offset in this frame
         * @return the type if any
         */
        public Class typeof(int s) {
            return modifiers != null && modifiers[s] != null ? modifiers[s].getType() : scope.getVariableType(s);
        }

        /**
         * Returns if the local variable is declared final.
         * @param s the symbol index
         * @return true if final, false otherwise
         */
        public boolean isVariableFinal(int s) {
            return modifiers != null && modifiers[s] != null ? modifiers[s].isFinal() : scope.isVariableFinal(s);
        }

        /**
         * Returns if the local variable is declared non-null.
         * @param s the symbol index
         * @return true if non-null, false otherwise
         */
        public boolean isVariableRequired(int s) {
            return modifiers != null && modifiers[s] != null ? modifiers[s].isRequired() : scope.isVariableRequired(s);
        }

        /**
         * Assign values to this frame.
         * @param values the values
         * @return this frame
         */
        public Frame assign(Object... values) {
            if (stack != null) {
                return new Frame(this, values);
            }
            return this;
        }

        /**
         * Creates a clone of this frame.
         * @return new frame
         */
        public Frame clone() {
            return new Frame(this);
        }

    }
}
