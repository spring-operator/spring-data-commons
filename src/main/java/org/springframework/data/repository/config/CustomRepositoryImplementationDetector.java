/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.repository.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Detects the custom implementation for a {@link org.springframework.data.repository.Repository}
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Jens Schauder
 */
@RequiredArgsConstructor
public class CustomRepositoryImplementationDetector {

	private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";

	private final @NonNull MetadataReaderFactory metadataReaderFactory;
	private final @NonNull Environment environment;
	private final @NonNull ResourceLoader resourceLoader;

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 *
	 * @param configuration the {@link RepositoryConfiguration} to consider.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	public Optional<AbstractBeanDefinition> detectCustomImplementation(RepositoryConfiguration<?> configuration) {

		// TODO 2.0: Extract into dedicated interface for custom implementation lookup configuration.

		return detectCustomImplementation( //
				configuration.getImplementationClassName(), //
				configuration.getImplementationBeanName(), //
				configuration.getBasePackages(), //
				configuration.getExcludeFilters(), //
				bd -> configuration.getConfigurationSource().generateBeanName(bd));
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 *
	 * @param className must not be {@literal null}.
	 * @param beanName may be {@literal null}
	 * @param basePackages must not be {@literal null}.
	 * @param excludeFilters must not be {@literal null}.
	 * @param beanNameGenerator must not be {@literal null}.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	public Optional<AbstractBeanDefinition> detectCustomImplementation(String className, String beanName,
			Iterable<String> basePackages, Iterable<TypeFilter> excludeFilters,
			Function<BeanDefinition, String> beanNameGenerator) {

		Assert.notNull(className, "ClassName must not be null!");
		Assert.notNull(basePackages, "BasePackages must not be null!");

		Set<BeanDefinition> definitions = findCandidateBeanDefinitions(className, basePackages, excludeFilters);

		SelectionSet<BeanDefinition> selection = new SelectionSet<>( //
				definitions, //
				c -> c.isEmpty() ? null : throwAmbiguousCustomImplementationException(c) //
		).filterIfNecessary(bd -> beanName != null && beanName.equals(beanNameGenerator.apply(bd)));

		return Optional.ofNullable((AbstractBeanDefinition) selection.uniqueResult());
	}

	Set<BeanDefinition> findCandidateBeanDefinitions(String className, Iterable<String> basePackages,
			Iterable<TypeFilter> excludeFilters) {

		// Build pattern to lookup implementation class
		Pattern pattern = Pattern.compile(".*\\." + className);

		// Build classpath scanner and lookup bean definition
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setEnvironment(environment);
		provider.setResourceLoader(resourceLoader);
		provider.setResourcePattern(String.format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, className));
		provider.setMetadataReaderFactory(metadataReaderFactory);
		provider.addIncludeFilter(new RegexPatternTypeFilter(pattern));

		for (TypeFilter excludeFilter : excludeFilters) {
			provider.addExcludeFilter(excludeFilter);
		}

		Set<BeanDefinition> definitions = new HashSet<>();

		for (String basePackage : basePackages) {
			definitions.addAll(provider.findCandidateComponents(basePackage));
		}

		return definitions;
	}

	private static AbstractBeanDefinition throwAmbiguousCustomImplementationException(
			Collection<BeanDefinition> definitions) {

		List<String> implementationClassNames = new ArrayList<String>();
		for (BeanDefinition bean : definitions) {
			implementationClassNames.add(bean.getBeanClassName());
		}

		throw new IllegalStateException(
				String.format("Ambiguous custom implementations detected! Found %s but expected a single implementation!", //
						definitions.stream()//
								.map(BeanDefinition::getBeanClassName)//
								.collect(Collectors.joining(", "))));
	}
}
