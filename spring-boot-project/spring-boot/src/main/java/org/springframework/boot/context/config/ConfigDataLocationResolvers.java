/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.util.Instantiator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * A collection of {@link ConfigDataLocationResolver} instances loaded via
 * {@code spring.factories}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLocationResolvers {

	private final List<ConfigDataLocationResolver<?>> resolvers;

	/**
	 * Create a new {@link ConfigDataLocationResolvers} instance.
	 * @param logFactory a {@link DeferredLogFactory} used to inject {@link Log} instances
	 * @param bootstrapContext the bootstrap context
	 * @param binder a binder providing values from the initial {@link Environment}
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 */
	ConfigDataLocationResolvers(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			Binder binder, ResourceLoader resourceLoader) {
		this(logFactory, bootstrapContext, binder, resourceLoader, SpringFactoriesLoader
				.loadFactoryNames(ConfigDataLocationResolver.class, resourceLoader.getClassLoader()));
	}

	/**
	 * Create a new {@link ConfigDataLocationResolvers} instance.
	 * @param logFactory a {@link DeferredLogFactory} used to inject {@link Log} instances
	 * @param bootstrapContext the bootstrap context
	 * @param binder {@link Binder} providing values from the initial {@link Environment}
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 * @param names the {@link ConfigDataLocationResolver} class names
	 */
	ConfigDataLocationResolvers(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			Binder binder, ResourceLoader resourceLoader, List<String> names) {
		Instantiator<ConfigDataLocationResolver<?>> instantiator = new Instantiator<>(ConfigDataLocationResolver.class,
				(availableParameters) -> {
					availableParameters.add(Log.class, logFactory::getLog);
					availableParameters.add(DeferredLogFactory.class, logFactory);
					availableParameters.add(Binder.class, binder);
					availableParameters.add(ResourceLoader.class, resourceLoader);
					availableParameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
					availableParameters.add(BootstrapContext.class, bootstrapContext);
					availableParameters.add(BootstrapRegistry.class, bootstrapContext);
				});
		this.resolvers = reorder(instantiator.instantiate(resourceLoader.getClassLoader(), names));
	}

	private List<ConfigDataLocationResolver<?>> reorder(List<ConfigDataLocationResolver<?>> resolvers) {
		List<ConfigDataLocationResolver<?>> reordered = new ArrayList<>(resolvers.size());
		StandardConfigDataLocationResolver resourceResolver = null;
		for (ConfigDataLocationResolver<?> resolver : resolvers) {
			if (resolver instanceof StandardConfigDataLocationResolver) {
				resourceResolver = (StandardConfigDataLocationResolver) resolver;
			}
			else {
				reordered.add(resolver);
			}
		}
		if (resourceResolver != null) {
			reordered.add(resourceResolver);
		}
		return Collections.unmodifiableList(reordered);
	}

	/**
	 * 扫描代码片段3
	 */
	List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location,
			Profiles profiles) {
		if (location == null) {
			return Collections.emptyList();
		}
		for (ConfigDataLocationResolver<?> resolver : getResolvers()) {
			if (resolver.isResolvable(context, location)) {
				// 进入扫描代码片段4
				return resolve(resolver, context, location, profiles);
			}
		}
		throw new UnsupportedConfigDataLocationException(location);
	}

	/**
	 * 扫描代码片段4
	 * resolver 是ConfigDataLocationResolver的factories中的
	 * StandardConfigDataLocationResolver和ConfigTreeConfigDataLocationResolver
	 * 构造函数时加载的
	 */
	private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolver<?> resolver,
			ConfigDataLocationResolverContext context, ConfigDataLocation location, Profiles profiles) {
		// 这里的 debug 会复杂一点，因为传入了一个 Supplier 函数
		// 如下面的扫描代码片段5，内部执行了 Supplier 函数的 get 方法，也就是这里的 lambda 表达式中的 resolve 方法
		List<ConfigDataResolutionResult> resolved = resolve(location, false, () -> resolver.resolve(context, location));
		if (profiles == null) {
			return resolved;
		}
		List<ConfigDataResolutionResult> profileSpecific = resolve(location, true,
				() -> resolver.resolveProfileSpecific(context, location, profiles));
		return merge(resolved, profileSpecific);
	}

	/**
	 * 扫描代码片段5
	 */
	private List<ConfigDataResolutionResult> resolve(ConfigDataLocation location, boolean profileSpecific,
			Supplier<List<? extends ConfigDataResource>> resolveAction) {
		// 前面4个代码片段，不管是中间变量还是 return 的结果，都是 List<ConfigDataResolutionResult>
		// 而这里出现了 List<ConfigDataResource>，显而易见，这一句代码是真正执行扫描的入口
		// 上面的几段代码只是对结果集的封装与处理
		// 代码片段4中提到，这个 get 就是 resolve，所以我们进入扫描代码片段6
		List<ConfigDataResource> resources = nonNullList(resolveAction.get());
		List<ConfigDataResolutionResult> resolved = new ArrayList<>(resources.size());
		for (ConfigDataResource resource : resources) {
			resolved.add(new ConfigDataResolutionResult(location, resource, profileSpecific));
		}
		return resolved;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> nonNullList(List<? extends T> list) {
		return (list != null) ? (List<T>) list : Collections.emptyList();
	}

	private <T> List<T> merge(List<T> list1, List<T> list2) {
		List<T> merged = new ArrayList<>(list1.size() + list2.size());
		merged.addAll(list1);
		merged.addAll(list2);
		return merged;
	}

	/**
	 * Return the resolvers managed by this object.
	 * @return the resolvers
	 */
	List<ConfigDataLocationResolver<?>> getResolvers() {
		return this.resolvers;
	}

}
