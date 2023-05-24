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

package org.springframework.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Listener for the {@link SpringApplication} {@code run} method.
 * {@link SpringApplicationRunListener}s are loaded via the {@link SpringFactoriesLoader}
 * and should declare a public constructor that accepts a {@link SpringApplication}
 * instance and a {@code String[]} of arguments. A new
 * {@link SpringApplicationRunListener} instance will be created for each run.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public interface SpringApplicationRunListener {

	/**
	 * Called immediately when the run method has first started. Can be used for very
	 * early initialization.
	 * @param bootstrapContext the bootstrap context
	 */
	default void starting(ConfigurableBootstrapContext bootstrapContext) {
		starting();
	}

	/**
	 * Called immediately when the run method has first started. Can be used for very
	 * early initialization.
	 * @deprecated since 2.4.0 for removal in 2.6.0 in favor of
	 * {@link #starting(ConfigurableBootstrapContext)}
	 */
	//当run方法第一次启动时立即调用，可用于非常早期的初始化
	@Deprecated
	default void starting() {
	}

	/**
	 * Called once the environment has been prepared, but before the
	 * {@link ApplicationContext} has been created.
	 * @param bootstrapContext the bootstrap context
	 * @param environment the environment
	 */
	default void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
			ConfigurableEnvironment environment) {
		environmentPrepared(environment);
	}

	/**
	 * Called once the environment has been prepared, but before the
	 * {@link ApplicationContext} has been created.
	 * @param environment the environment
	 * @deprecated since 2.4.0 for removal in 2.6.0 in favor of
	 * {@link #environmentPrepared(ConfigurableBootstrapContext, ConfigurableEnvironment)}
	 */
	////在ApplicationContext创建之前，一旦环境environment准备好就调用。
	@Deprecated
	default void environmentPrepared(ConfigurableEnvironment environment) {
	}

	/**
	 * Called once the {@link ApplicationContext} has been created and prepared, but
	 * before sources have been loaded.
	 * @param context the application context
	 */
	//在资源（可以理解为配置主类）被加载完成之前，一旦ApplicationContext被创建并准备好就立马调用，
	default void contextPrepared(ConfigurableApplicationContext context) {
	}

	/**
	 * Called once the application context has been loaded but before it has been
	 * refreshed.
	 * @param context the application context
	 */
	//在资源加载完成之后，但在刷新之前调用
	default void contextLoaded(ConfigurableApplicationContext context) {
	}

	/**
	 * The context has been refreshed and the application has started but
	 * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
	 * ApplicationRunners} have not been called.
	 * @param context the application context.
	 * @since 2.0.0
	 */
	//上下文已刷新，应用程序已启动，但是CommandLineRunner和ApplicationRunner尚未调用。
	default void started(ConfigurableApplicationContext context) {
	}

	/**
	 * Called immediately before the run method finishes, when the application context has
	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
	 * {@link ApplicationRunner ApplicationRunners} have been called.
	 * @param context the application context.
	 * @since 2.0.0
	 */
	//当ApplicationContext已经refresh且所有的CommandLineRunner和ApplicationRunner都已被调用时，在run方法完成之前立即调用
	default void running(ConfigurableApplicationContext context) {
	}

	/**
	 * Called when a failure occurs when running the application.
	 * @param context the application context or {@code null} if a failure occurred before
	 * the context was created
	 * @param exception the failure
	 * @since 2.0.0
	 */
	//在运行应用程序时发生故障时调用
	default void failed(ConfigurableApplicationContext context, Throwable exception) {
	}

}
