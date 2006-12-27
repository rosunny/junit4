package org.junit.internal.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MethodValidator {
	private final TestIntrospector fIntrospector;

	private final List<Throwable> fErrors= new ArrayList<Throwable>();

	private final Class<?> fTestClass;

	public MethodValidator(Class<?> testClass) {
		fTestClass= testClass;
		fIntrospector= new TestIntrospector(testClass);
	}

	public void validateInstanceMethods() {
		validateTestMethods(After.class, false);
		validateTestMethods(Before.class, false);
		validateTestMethods(Test.class, false);
	}

	public void validateStaticMethods() {
		validateTestMethods(BeforeClass.class, true);
		validateTestMethods(AfterClass.class, true);
	}
	
	// TODO Ugly API--one method should do both
	public List<Throwable> validateAllMethods() {
		validateNoArgConstructor();
		validateStaticMethods();
		validateInstanceMethods();
		return fErrors;
	}
	
	public void assertValid() throws InitializationError {
		if (!fErrors.isEmpty())
			throw new InitializationError(fErrors);
	}

	public void validateNoArgConstructor() {
		try {
			fTestClass.getConstructor();
		} catch (Exception e) {
			fErrors.add(new Exception("Test class should have public zero-argument constructor", e));
		}
	}

	private void validateTestMethods(Class<? extends Annotation> annotation,
			boolean shouldBeStatic) {
		List<Method> methods= fIntrospector.getTestMethods(annotation);
		for (Method each : methods) {
			if (shouldBeStatic && !isStatic(each))
				fErrors.add(new Exception("Method " + each.getName() + "() "
						+ "should be static"));
			if (!shouldBeStatic && runsAsStatic(each))
				fErrors.add(new Exception("Method " + each.getName() + "() "
						+ "should not be static"));
			if (!Modifier.isPublic(each.getDeclaringClass().getModifiers()))
				fErrors.add(new Exception("Class " + each.getDeclaringClass().getName()
						+ " should be public"));
			if (!Modifier.isPublic(each.getModifiers()))
				fErrors.add(new Exception("Method " + each.getName()
						+ " should be public"));
			if (each.getReturnType() != Void.TYPE)
				fErrors.add(new Exception("Method " + each.getName()
						+ " should be void"));
			if (effectiveParameterCount(each, shouldBeStatic) != 0)
				fErrors.add(new Exception("Method " + each.getName()
						+ " should have no parameters"));
		}
	}

	private int effectiveParameterCount(Method method, boolean shouldBeStatic) {
		int rawLength= method.getParameterTypes().length;
		if (!shouldBeStatic && isStatic(method) && rawLength > 0)
			return rawLength - 1;
		return rawLength;
	}

	private boolean runsAsStatic(Method method) {
		return isStatic(method) && !(method.getParameterTypes().length == 1);
	}

	private boolean isStatic(Method method) {
		return Modifier.isStatic(method.getModifiers());
	}
}
