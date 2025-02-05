// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.catalog;

import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.types.DataType;
import org.apache.doris.nereids.types.coercion.AbstractDataType;
import org.apache.doris.nereids.types.coercion.FollowToArgumentType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public class FunctionSignature {
    public final AbstractDataType returnType;
    public final boolean hasVarArgs;
    public final List<AbstractDataType> argumentsTypes;
    public final int arity;

    private FunctionSignature(AbstractDataType returnType, boolean hasVarArgs,
            List<? extends AbstractDataType> argumentsTypes) {
        this.returnType = Objects.requireNonNull(returnType, "returnType is not null");
        this.argumentsTypes = ImmutableList.copyOf(
                Objects.requireNonNull(argumentsTypes, "argumentsTypes is not null"));
        this.hasVarArgs = hasVarArgs;
        this.arity = argumentsTypes.size();
    }

    public Optional<AbstractDataType> getVarArgType() {
        return hasVarArgs ? Optional.of(argumentsTypes.get(arity - 1)) : Optional.empty();
    }

    public AbstractDataType getArgType(int index) {
        if (hasVarArgs && index >= arity) {
            return argumentsTypes.get(arity - 1);
        }
        return argumentsTypes.get(index);
    }

    public FunctionSignature withReturnType(DataType returnType) {
        return new FunctionSignature(returnType, hasVarArgs, argumentsTypes);
    }

    public FunctionSignature withArgumentTypes(boolean hasVarArgs, List<? extends AbstractDataType> argumentsTypes) {
        return new FunctionSignature(returnType, hasVarArgs, argumentsTypes);
    }

    /**
     * change argument type by the signature's type and the corresponding argument's type
     * @param arguments arguments
     * @param transform param1: signature's type, param2: argument's type, return new type you want to change
     * @return
     */
    public FunctionSignature withArgumentTypes(List<Expression> arguments,
            BiFunction<AbstractDataType, Expression, AbstractDataType> transform) {
        List<AbstractDataType> newTypes = Lists.newArrayList();
        for (int i = 0; i < arguments.size(); i++) {
            newTypes.add(transform.apply(getArgType(i), arguments.get(i)));
        }
        return withArgumentTypes(hasVarArgs, newTypes);
    }

    /**
     * change argument type by the signature's type and the corresponding argument's type
     * @param arguments arguments
     * @param transform param1: argument's index, param2: signature's type, param3: argument's type,
     *                  return new type you want to change
     * @return
     */
    public FunctionSignature withArgumentTypes(List<Expression> arguments,
            TripleFunction<Integer, AbstractDataType, Expression, AbstractDataType> transform) {
        List<AbstractDataType> newTypes = Lists.newArrayList();
        for (int i = 0; i < argumentsTypes.size(); i++) {
            newTypes.add(transform.apply(i, argumentsTypes.get(i), arguments.get(i)));
        }
        return withArgumentTypes(hasVarArgs, newTypes);
    }

    public static FunctionSignature of(AbstractDataType returnType, List<? extends AbstractDataType> argumentsTypes) {
        return of(returnType, false, argumentsTypes);
    }

    public static FunctionSignature of(AbstractDataType returnType, boolean hasVarArgs,
            List<? extends AbstractDataType> argumentsTypes) {
        return new FunctionSignature(returnType, hasVarArgs, argumentsTypes);
    }

    public static FunctionSignature of(AbstractDataType returnType, AbstractDataType... argumentsTypes) {
        return of(returnType, false, argumentsTypes);
    }

    public static FunctionSignature of(AbstractDataType returnType,
            boolean hasVarArgs, AbstractDataType... argumentsTypes) {
        return new FunctionSignature(returnType, hasVarArgs, Arrays.asList(argumentsTypes));
    }

    public static FuncSigBuilder ret(DataType returnType) {
        return new FuncSigBuilder(returnType);
    }

    public static FuncSigBuilder retArgType(int argIndex) {
        return new FuncSigBuilder(new FollowToArgumentType(argIndex));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("returnType", returnType)
                .add("hasVarArgs", hasVarArgs)
                .add("argumentsTypes", argumentsTypes)
                .add("arity", arity)
                .toString();
    }

    public static class FuncSigBuilder {
        public final AbstractDataType returnType;

        public FuncSigBuilder(AbstractDataType returnType) {
            this.returnType = returnType;
        }

        public FunctionSignature args(AbstractDataType...argTypes) {
            return FunctionSignature.of(returnType, false, argTypes);
        }

        public FunctionSignature varArgs(AbstractDataType...argTypes) {
            return FunctionSignature.of(returnType, true, argTypes);
        }
    }

    public interface TripleFunction<P1, P2, P3, R> {
        R apply(P1 p1, P2 p2, P3 p3);
    }
}
