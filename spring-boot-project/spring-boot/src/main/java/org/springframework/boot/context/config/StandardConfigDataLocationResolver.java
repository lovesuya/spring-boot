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

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;

import org.springframework.boot.context.config.LocationResourceLoader.ResourceType;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigDataLocationResolver} for standard locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.4.0
 */
public class StandardConfigDataLocationResolver
		implements ConfigDataLocationResolver<StandardConfigDataResource>, Ordered {

	private static final String PREFIX = "resource:";

	static final String CONFIG_NAME_PROPERTY = "spring.config.name";

	private static final String[] DEFAULT_CONFIG_NAMES = { "application" };

	private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9*]*?:)(.*$)");

	private static final Pattern EXTENSION_HINT_PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)\\](?!\\[)$");

	private static final String NO_PROFILE = null;

	private final Log logger;

	private final List<PropertySourceLoader> propertySourceLoaders;

	private final String[] configNames;

	private final LocationResourceLoader resourceLoader;

	/**
	 * Create a new {@link StandardConfigDataLocationResolver} instance.
	 * @param logger the logger to use
	 * @param binder a binder backed by the initial {@link Environment}
	 * @param resourceLoader a {@link ResourceLoader} used to load resources
	 */
	public StandardConfigDataLocationResolver(Log logger, Binder binder, ResourceLoader resourceLoader) {
		this.logger = logger;
		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				getClass().getClassLoader());
		this.configNames = getConfigNames(binder);
		this.resourceLoader = new LocationResourceLoader(resourceLoader);
	}

	private String[] getConfigNames(Binder binder) {
		String[] configNames = binder.bind(CONFIG_NAME_PROPERTY, String[].class).orElse(DEFAULT_CONFIG_NAMES);
		for (String configName : configNames) {
			validateConfigName(configName);
		}
		return configNames;
	}

	private void validateConfigName(String name) {
		Assert.state(!name.contains("*"), () -> "Config name '" + name + "' cannot contain '*'");
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return true;
	}

	@Override
	public List<StandardConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataNotFoundException {
		return resolve(getReferences(context, location.split()));
	}

	private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation[] configDataLocations) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		for (ConfigDataLocation configDataLocation : configDataLocations) {
			references.addAll(getReferences(context, configDataLocation));
		}
		return references;
	}

	private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation configDataLocation) {
		String resourceLocation = getResourceLocation(context, configDataLocation);
		try {
			if (isDirectory(resourceLocation)) {
				return getReferencesForDirectory(configDataLocation, resourceLocation, NO_PROFILE);
			}
			return getReferencesForFile(configDataLocation, resourceLocation, NO_PROFILE);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to load config data from '" + configDataLocation + "'", ex);
		}
	}
	/**
	 * 扫描代码片段6
	 */
	@Override
	public List<StandardConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
			ConfigDataLocation location, Profiles profiles) {
		// 上面几段代码的参数无非是 localtion、profile 等基本信息
		// 而这里的 getReferences 获取的是一个依赖集
		// 在进入下一个 resolve 方法前，我们先看看这个依赖集是什么作用
		// 这里传入了两个参数，context 就不用多说了，location.split() 需要提一句
		// SpringBoot 内置的扫描目录是以分号隔开的字符串，这里的 split 方法就是以分号来分割字符串，变成一个目录地址的数组
		// 而依赖集就是根据 SpringBoot 内置的规则获取的依赖地址
		return resolve(getProfileSpecificReferences(context, location.split(), profiles));
	}

	private Set<StandardConfigDataReference> getProfileSpecificReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation[] configDataLocations, Profiles profiles) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		for (String profile : profiles) {
			for (ConfigDataLocation configDataLocation : configDataLocations) {
				String resourceLocation = getResourceLocation(context, configDataLocation);
				// 是不是很熟悉，像极了 resolve 方法的层层嵌套，那么继续往里面看
				references.addAll(getReferences(configDataLocation, resourceLocation, profile));
			}
		}
		return references;
	}

	private String getResourceLocation(ConfigDataLocationResolverContext context,
			ConfigDataLocation configDataLocation) {
		String resourceLocation = configDataLocation.getNonPrefixedValue(PREFIX);
		boolean isAbsolute = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
		if (isAbsolute) {
			return resourceLocation;
		}
		ConfigDataResource parent = context.getParent();
		if (parent instanceof StandardConfigDataResource) {
			String parentResourceLocation = ((StandardConfigDataResource) parent).getReference().getResourceLocation();
			String parentDirectory = parentResourceLocation.substring(0, parentResourceLocation.lastIndexOf("/") + 1);
			return parentDirectory + resourceLocation;
		}
		return resourceLocation;
	}

	private Set<StandardConfigDataReference> getReferences(ConfigDataLocation configDataLocation,
			String resourceLocation, String profile) {
		// 我们只看配置文件的扫描，配置文件扫描的是目录，所以这里只解析这段 if 里的代码
		if (isDirectory(resourceLocation)) {
			return getReferencesForDirectory(configDataLocation, resourceLocation, profile);
		}
		return getReferencesForFile(configDataLocation, resourceLocation, profile);
	}

	private Set<StandardConfigDataReference> getReferencesForDirectory(ConfigDataLocation configDataLocation,
			String directory, String profile) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		// 配置文件的 name 为 application，这是 SpringBoot 约定好的，不是我们能更改的
		for (String name : this.configNames) {
			// 进入下一个方法 依赖代码片段4
			Deque<StandardConfigDataReference> referencesForName = getReferencesForConfigName(name, configDataLocation,
					directory, profile);
			references.addAll(referencesForName);
		}
		return references;
	}

	/**
	 * 依赖代码片段4
	 */
	private Deque<StandardConfigDataReference> getReferencesForConfigName(String name,
			ConfigDataLocation configDataLocation, String directory, String profile) {
		Deque<StandardConfigDataReference> references = new ArrayDeque<>();
		// PropertySourceLoader 在这个类的构造函数中初始化完毕
		// 在 spring.factories 文件中可以看到一共有两个 loader
		// PropertiesPropertySourceLoader 和 YamlPropertySourceLoader
		// 分别是 （properties 与 xml）加载器、（yaml 与 yml）加载器
		for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
			// 不同加载器有对应的文件扩展名
			for (String extension : propertySourceLoader.getFileExtensions()) {
				// 根据目录 + 文件名 + 激活名 + 扩展名创建一个资源依赖，并传入了对应的资源加载器
				StandardConfigDataReference reference = new StandardConfigDataReference(configDataLocation, directory,
						directory + name, profile, extension, propertySourceLoader);
				if (!references.contains(reference)) {
					references.addFirst(reference);
				}
			}
		}
		return references;
	}

	private Set<StandardConfigDataReference> getReferencesForFile(ConfigDataLocation configDataLocation, String file,
			String profile) {
		Matcher extensionHintMatcher = EXTENSION_HINT_PATTERN.matcher(file);
		boolean extensionHintLocation = extensionHintMatcher.matches();
		if (extensionHintLocation) {
			file = extensionHintMatcher.group(1) + extensionHintMatcher.group(2);
		}
		for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
			String extension = getLoadableFileExtension(propertySourceLoader, file);
			if (extension != null) {
				String root = file.substring(0, file.length() - extension.length() - 1);
				StandardConfigDataReference reference = new StandardConfigDataReference(configDataLocation, null, root,
						profile, (!extensionHintLocation) ? extension : null, propertySourceLoader);
				return Collections.singleton(reference);
			}
		}
		throw new IllegalStateException("File extension is not known to any PropertySourceLoader. "
				+ "If the location is meant to reference a directory, it must end in '/' or File.separator");
	}

	private String getLoadableFileExtension(PropertySourceLoader loader, String file) {
		for (String fileExtension : loader.getFileExtensions()) {
			if (StringUtils.endsWithIgnoreCase(file, fileExtension)) {
				return fileExtension;
			}
		}
		return null;
	}

	private boolean isDirectory(String resourceLocation) {
		return resourceLocation.endsWith("/") || resourceLocation.endsWith(File.separator);
	}

	/**
	 * 扫描代码片段7
	 * 从这个 resolve 方法开始，限定符已经变成了 private
	 * 开始涉及到真正的扫描逻辑了
	 */
	private List<StandardConfigDataResource> resolve(Set<StandardConfigDataReference> references) {
		List<StandardConfigDataResource> resolved = new ArrayList<>();
		for (StandardConfigDataReference reference : references) {
			// 继续进入下一个 resolve 方法
			resolved.addAll(resolve(reference));
		}
		if (resolved.isEmpty()) {
			resolved.addAll(resolveEmptyDirectories(references));
		}
		return resolved;
	}

	private Collection<StandardConfigDataResource> resolveEmptyDirectories(
			Set<StandardConfigDataReference> references) {
		Set<StandardConfigDataResource> empty = new LinkedHashSet<>();
		for (StandardConfigDataReference reference : references) {
			if (reference.getDirectory() != null) {
				empty.addAll(resolveEmptyDirectories(reference));
			}
		}
		return empty;
	}

	private Set<StandardConfigDataResource> resolveEmptyDirectories(StandardConfigDataReference reference) {
		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
			return resolveNonPatternEmptyDirectories(reference);
		}
		return resolvePatternEmptyDirectories(reference);
	}

	private Set<StandardConfigDataResource> resolveNonPatternEmptyDirectories(StandardConfigDataReference reference) {
		Resource resource = this.resourceLoader.getResource(reference.getDirectory());
		return (resource instanceof ClassPathResource || !resource.exists()) ? Collections.emptySet()
				: Collections.singleton(new StandardConfigDataResource(reference, resource, true));
	}

	private Set<StandardConfigDataResource> resolvePatternEmptyDirectories(StandardConfigDataReference reference) {
		Resource[] subdirectories = this.resourceLoader.getResources(reference.getDirectory(), ResourceType.DIRECTORY);
		ConfigDataLocation location = reference.getConfigDataLocation();
		if (!location.isOptional() && ObjectUtils.isEmpty(subdirectories)) {
			String message = String.format("Config data location '%s' contains no subdirectories", location);
			throw new ConfigDataLocationNotFoundException(location, message, null);
		}
		return Arrays.stream(subdirectories).filter(Resource::exists)
				.map((resource) -> new StandardConfigDataResource(reference, resource, true))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 扫描代码片段8
	 */
	private List<StandardConfigDataResource> resolve(StandardConfigDataReference reference) {
		// 这个 isPattern 是判断依赖的路径字符串是否是模式串
		// 比如 classpath:*
		// 这类带 * 号的代表目录下的所有文件，那么就需要特殊的处理
		// 我们加载配置文件的部分是确定文件地址的，所以走这个 if
		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
			return resolveNonPattern(reference);
		}
		return resolvePattern(reference);
	}

	/**
	 * 扫描代码片段9
	 */
	private List<StandardConfigDataResource> resolveNonPattern(StandardConfigDataReference reference) {
		// 根据依赖地址加载资源
		// 这里的加载和后面的 load 不是一个概念
		// 这里只是将资源加载到内存，还没有开始解析
		// 而 load 的加载是加载到 SpringBoot
		Resource resource = this.resourceLoader.getResource(reference.getResourceLocation());
		// 如果资源不存在并且是可跳过的，那么返回空集合
		if (!resource.exists() && reference.isSkippable()) {
			logSkippingResource(reference);
			return Collections.emptyList();
		}
		// 资源存在的话就返回
		return Collections.singletonList(createConfigResourceLocation(reference, resource));
	}

	private List<StandardConfigDataResource> resolvePattern(StandardConfigDataReference reference) {
		List<StandardConfigDataResource> resolved = new ArrayList<>();
		for (Resource resource : this.resourceLoader.getResources(reference.getResourceLocation(), ResourceType.FILE)) {
			if (!resource.exists() && reference.isSkippable()) {
				logSkippingResource(reference);
			}
			else {
				resolved.add(createConfigResourceLocation(reference, resource));
			}
		}
		return resolved;
	}

	private void logSkippingResource(StandardConfigDataReference reference) {
		this.logger.trace(LogMessage.format("Skipping missing resource %s", reference));
	}

	private StandardConfigDataResource createConfigResourceLocation(StandardConfigDataReference reference,
			Resource resource) {
		return new StandardConfigDataResource(reference, resource);
	}

}
