/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.querydsl.binding;

import java.util.Collection;

import org.springframework.util.Assert;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.CollectionPathBase;

/**
 * Default implementation of {@link MultiValueBinding} creating {@link Predicate} based on the {@link Path}s type.
 * Binds:
 * <ul>
 * <li><i>{@link java.lang.Object}</i> as {@link SimpleExpression#eq()} on simple properties.</li>
 * <li><i>{@link java.lang.Object}</i> as {@link SimpleExpression#contains()} on collection properties.</li>
 * <li><i>{@link java.util.Collection}</i> as {@link SimpleExpression#in()} on simple properties.</li>
 * </ul>
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
class QuerydslDefaultBinding implements MultiValueBinding<Path<? extends Object>, Object> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.querydsl.QueryDslPredicateBuilder#buildPredicate(org.springframework.data.mapping.PropertyPath, java.lang.Object)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate bind(Path<?> path, Collection<? extends Object> value) {

		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(value, "Value must not be null!");

		if (value.isEmpty()) {
			return null;
		}

		if (path instanceof CollectionPathBase) {

			BooleanBuilder builder = new BooleanBuilder();

			for (Object element : value) {
				builder.and(((CollectionPathBase) path).contains(element));
			}

			return builder.getValue();
		}

		if (path instanceof SimpleExpression) {

			if (value.size() > 1) {
				return ((SimpleExpression) path).in(value);
			}

			return ((SimpleExpression) path).eq(value.iterator().next());
		}

		throw new IllegalArgumentException(
				String.format("Cannot create predicate for path '%s' with type '%s'.", path, path.getMetadata().getPathType()));
	}
}
