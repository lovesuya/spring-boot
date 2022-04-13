/*
 * Copyright 2012-2019 the original author or authors.
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
 * 监听SpringApplication的run方法。SpringApplicationRunListener 是 SpringFactoriesLoader加载的，并且应该声明
 * 为构造器是接收 SpringApplication实例 和 String[]的参数。一个新的SpringApplicationRunListener实例将会每次运行创建
 */
public interface SpringApplicationRunListener {

	/**
	 * Called immediately when the run method has first started. Can be used for very
	 * early initialization.
	 * 在运行方法第一次启动时立即调用。可以用于非常早期的初始化。
	 */
	default void starting() {
	}

	/**
	 * Called once the environment has been prepared, but before the
	 * {@link ApplicationContext} has been created.
	 * @param environment the environment
	 * 在环境准备好，但在ApplicaitonContext创建之前
	 */
	default void environmentPrepared(ConfigurableEnvironment environment) {
	}

	/**
	 * Called once the {@link ApplicationContext} has been created and prepared, but
	 * before sources have been loaded.
	 * @param context the application context
	 * 在ApplicationContext创建和准备好，但在资源加载前
	 */
	default void contextPrepared(ConfigurableApplicationContext context) {
	}

	/**
	 * Called once the application context has been loaded but before it has been
	 * refreshed.
	 * @param context the application context
	 * application context 加载完，但在刷新之前
	 */
	default void contextLoaded(ConfigurableApplicationContext context) {
	}

	/**
	 * The context has been refreshed and the application has started but
	 * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
	 * ApplicationRunners} have not been called.
	 * @param context the application context.
	 * @since 2.0.0
	 * context 刷新完并且 application 已完成，但是在 CommandLineRunner 和 ApplicationRunner 被调用前
	 */
	default void started(ConfigurableApplicationContext context) {
	}

	/**
	 * Called immediately before the run method finishes, when the application context has
	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
	 * {@link ApplicationRunner ApplicationRunners} have been called.
	 * @param context the application context.
	 * @since 2.0.0
	 * 在run方法运行结束前调用，当application context 已经刷新 并且 CommandLineRunner 和 ApplicationRunner也已经
	 * 被调用
	 */
	default void running(ConfigurableApplicationContext context) {
	}

	/**
	 * Called when a failure occurs when running the application.
	 * @param context the application context or {@code null} if a failure occurred before
	 * the context was created
	 * @param exception the failure
	 * @since 2.0.0
	 * 在运行程序的时候发生错误的时候调用
	 */
	default void failed(ConfigurableApplicationContext context, Throwable exception) {
	}

}
