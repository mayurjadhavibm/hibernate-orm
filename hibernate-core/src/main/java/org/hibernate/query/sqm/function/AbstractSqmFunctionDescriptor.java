/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunctionDescriptor implements SqmFunctionDescriptor {
	private final ArgumentsValidator argumentsValidator;
	private final FunctionReturnTypeResolver returnTypeResolver;
	private final FunctionArgumentTypeResolver functionArgumentTypeResolver;
	private final String name;

	public AbstractSqmFunctionDescriptor(String name) {
		this( name, null, null, null );
	}

	public AbstractSqmFunctionDescriptor(
			String name,
			ArgumentsValidator argumentsValidator) {
		this( name, argumentsValidator, null, null );
	}

	public AbstractSqmFunctionDescriptor(
			String name,
			ArgumentsValidator argumentsValidator,
			FunctionArgumentTypeResolver argumentTypeResolver) {
		this( name, argumentsValidator, null, argumentTypeResolver );
	}

	public AbstractSqmFunctionDescriptor(
			String name,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			FunctionArgumentTypeResolver argumentTypeResolver) {
		this.name = name;
		this.argumentsValidator = argumentsValidator == null
				? StandardArgumentsValidators.NONE
				: argumentsValidator;
		this.returnTypeResolver = returnTypeResolver == null
				? StandardFunctionReturnTypeResolvers.useFirstNonNull()
				: returnTypeResolver;
		this.functionArgumentTypeResolver = argumentTypeResolver == null
				? StandardFunctionArgumentTypeResolvers.NULL
				: argumentTypeResolver;
	}

	public String getName() {
		return name;
	}

	public String getSignature(String name) {
		return getReturnSignature() + name + getArgumentListSignature();
	}

	@Override
	public ArgumentsValidator getArgumentsValidator() {
		return argumentsValidator;
	}

	public FunctionReturnTypeResolver getReturnTypeResolver() {
		return returnTypeResolver;
	}

	public FunctionArgumentTypeResolver getArgumentTypeResolver() {
		return functionArgumentTypeResolver;
	}

	public String getReturnSignature() {
		String result = returnTypeResolver.getReturnType();
		return result.isEmpty() ? "" : result + " ";
	}

	public String getArgumentListSignature() {
		String args = argumentsValidator.getSignature();
		return alwaysIncludesParentheses() ? args : "()".equals(args) ? "" : "[" + args + "]";
	}

	@Override
	public final <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		argumentsValidator.validate( arguments, getName(), typeConfiguration );

		return generateSqmFunctionExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	@Override
	public final <T> SelfRenderingSqmFunction<T> generateAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		argumentsValidator.validate( arguments, getName(), typeConfiguration );

		return generateSqmAggregateFunctionExpression(
				arguments,
				filter,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	@Override
	public final <T> SelfRenderingSqmFunction<T> generateOrderedSetAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		argumentsValidator.validate( arguments, getName(), typeConfiguration );

		return generateSqmOrderedSetAggregateFunctionExpression(
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	@Override
	public final <T> SelfRenderingSqmFunction<T> generateWindowSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		argumentsValidator.validate( arguments, getName(), typeConfiguration );

		return generateSqmWindowFunctionExpression(
				arguments,
				filter,
				respectNulls,
				fromFirst,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	/**
	 * Return an SQM node or subtree representing an invocation of this function
	 * with the given arguments. This method may be overridden in the case of
	 * function descriptors that wish to customize creation of the node.
	 *  @param arguments the arguments of the function invocation
	 * @param impliedResultType the function return type as inferred from its usage
	 */
	protected abstract <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration);

	/**
	 * Return an SQM node or subtree representing an invocation of this aggregate function
	 * with the given arguments. This method may be overridden in the case of
	 * function descriptors that wish to customize creation of the node.
	 *
	 * @param arguments the arguments of the function invocation
	 * @param impliedResultType the function return type as inferred from its usage
	 */
	protected <T> SelfRenderingSqmAggregateFunction<T> generateSqmAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return (SelfRenderingSqmAggregateFunction<T>) generateSqmExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	/**
	 * Return an SQM node or subtree representing an invocation of this ordered set-aggregate function
	 * with the given arguments. This method may be overridden in the case of
	 * function descriptors that wish to customize creation of the node.
	 *
	 * @param arguments the arguments of the function invocation
	 * @param impliedResultType the function return type as inferred from its usage
	 */
	protected <T> SelfRenderingSqmAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return (SelfRenderingSqmAggregateFunction<T>) generateSqmExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	/**
	 * Return an SQM node or subtree representing an invocation of this window function
	 * with the given arguments. This method may be overridden in the case of
	 * function descriptors that wish to customize creation of the node.
	 *
	 * @param arguments the arguments of the function invocation
	 * @param respectNulls
	 * @param fromFirst
	 * @param impliedResultType the function return type as inferred from its usage
	 */
	protected <T> SelfRenderingSqmWindowFunction<T> generateSqmWindowFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return (SelfRenderingSqmWindowFunction<T>) generateSqmExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}
}

