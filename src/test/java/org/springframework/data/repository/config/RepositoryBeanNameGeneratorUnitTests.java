/*
 * Copyright 2012-2013 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * Unit tests for {@link RepositoryBeanNameGenerator}.
 * 
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class RepositoryBeanNameGeneratorUnitTests {

	RepositoryBeanNameGenerator generator;
	BeanDefinitionRegistry registry;

	@Before
	public void setUp() {

		this.generator = new RepositoryBeanNameGenerator(Thread.currentThread().getContextClassLoader());
	}

	@Test
	public void usesPlainClassNameIfNoAnnotationPresent() {
		assertThat(generator.generateBeanName(getBeanDefinitionFor(MyRepository.class))).isEqualTo("myRepository");
	}

	@Test
	public void usesAnnotationValueIfAnnotationPresent() {
		assertThat(generator.generateBeanName(getBeanDefinitionFor(AnnotatedInterface.class))).isEqualTo("specialName");
	}

	private BeanDefinition getBeanDefinitionFor(Class<?> repositoryInterface) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RepositoryFactoryBeanSupport.class);
		builder.addConstructorArgValue(repositoryInterface.getName());
		return builder.getBeanDefinition();
	}

	interface PlainInterface {

	}

	@Named("specialName")
	interface AnnotatedInterface {

	}
}
