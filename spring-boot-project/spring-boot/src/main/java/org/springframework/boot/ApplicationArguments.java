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

import java.util.List;
import java.util.Set;

/**
 * Provides access to the arguments that were used to run a {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * ApplicationArguments接口提供对用于运行org.springframework.boot.SpringApplication的参数访问，
 * 此接口只有一个实现类DefaultApplicationArguments
 */
public interface ApplicationArguments {

	/**
	 * Return the raw unprocessed arguments that were passed to the application.
	 * @return the arguments
	 * 返回传递给应用程序的原始未处理参数返回传递给应用程序的原始未处理参数
	 */
	String[] getSourceArgs();

	/**
	 * Return the names of all option arguments. For example, if the arguments were
	 * "--foo=bar --debug" would return the values {@code ["foo", "debug"]}.
	 * @return the option names or an empty set
	 * 返回所有选项参数的名称，例如，如果参数是"--foo=bar --debug"将会返回["foo", "debug"]
	 */
	Set<String> getOptionNames();

	/**
	 * Return whether the set of option arguments parsed from the arguments contains an
	 * option with the given name.
	 * @param name the name to check
	 * @return {@code true} if the arguments contain an option with the given name
	 * 返回解析后的选项参数集合中是否包含给定名称的选项
	 */
	boolean containsOption(String name);

	/**
	 * Return the collection of values associated with the arguments option having the
	 * given name.
	 * <ul>
	 * <li>if the option is present and has no argument (e.g.: "--foo"), return an empty
	 * collection ({@code []})</li>
	 * <li>if the option is present and has a single value (e.g. "--foo=bar"), return a
	 * collection having one element ({@code ["bar"]})</li>
	 * <li>if the option is present and has multiple values (e.g. "--foo=bar --foo=baz"),
	 * return a collection having elements for each value ({@code ["bar", "baz"]})</li>
	 * <li>if the option is not present, return {@code null}</li>
	 * </ul>
	 * @param name the name of the option
	 * @return a list of option values for the given name
	 *  返回与具有给定名称的arguments选项关联的值的集合
	 *  如果该选项存在并且没有参数值（例如：”--foo“）,则返回一个空集合
	 *  如果该选项存在并且只有一个值 (例如: "--foo=bar"), 则返回一个包含一个元素的集合["bar"]
	 *  如果该选项存在并且有多个值（例如："--foo=bar --foo=baz"）,则返回一个包含每个值元素的集合["bar", "baz"]
	 *  如果选项不存在，则返回null
	 *  @param name 选项名称
	 *  @return 返回选项值的集合（不存在返回null）
	 */
	List<String> getOptionValues(String name);

	/**
	 * Return the collection of non-option arguments parsed.
	 * @return the non-option arguments or an empty list
	 * 返回已解析的非选项参数，不存在返回空集合
	 */
	List<String> getNonOptionArgs();

}
