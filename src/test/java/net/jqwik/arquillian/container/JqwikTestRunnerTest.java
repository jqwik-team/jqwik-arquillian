/*
 * Copyright (c) 2026 jqwik team
 * Copyright (c) 2026 Adeptum AB and Adam Waldenberg
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package net.jqwik.arquillian.container;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class JqwikTestRunnerTest {
	@Example
	void findsAPropertyDeclaredInASuperclass() throws NoSuchMethodException {
		assertThat(JqwikTestRunner.propertyMethod(Subclass.class, "inherited"))
			.isEqualTo(Superclass.class.getDeclaredMethod("inherited"));
	}

	@Example
	void findsAPropertyDeclaredAsInterfaceDefaultMethod() throws NoSuchMethodException {
		assertThat(JqwikTestRunner.propertyMethod(Subclass.class, "fromInterface"))
			.isEqualTo(WithDefaultMethod.class.getDeclaredMethod("fromInterface"));
	}

	@Example
	void findsTheOverridingMethodOnly() throws NoSuchMethodException {
		assertThat(JqwikTestRunner.propertyMethod(Subclass.class, "overridden"))
			.isEqualTo(Subclass.class.getDeclaredMethod("overridden"));
	}

	@Example
	void rejectsANameThatIsNotUnique() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> JqwikTestRunner.propertyMethod(Subclass.class, "overloaded"))
			.withMessageEndingWith("but found 2");
	}

	interface WithDefaultMethod {
		default void fromInterface() {
		}
	}

	static class Superclass {
		void inherited() {
		}

		void overridden() {
		}
	}

	static class Subclass extends Superclass implements WithDefaultMethod {
		@Override
		void overridden() {
		}

		void overloaded() {
		}

		void overloaded(int tries) {
		}
	}
}
