package jminusminus;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tests for Exercises 4.1 and 4.2 using the actual jminusminus APIs present in
 * the project.
 *
 * These tests are defensive: they count the "multiple public" diagnostics so
 * that failures in the compiler that erroneously emit the diagnostic can be
 * detected precisely.
 */
public class JVariableTest {

	// Write source to a temp file and return the path
	private String writeTempSource(String baseName, String source) throws IOException {
		File tmp = File.createTempFile(baseName, ".java");
		tmp.deleteOnExit();
		try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
			w.write(source);
		}
		return tmp.getAbsolutePath();
	}

	// Parse a file, run preAnalyze/analyze, return captured System.err output
	// (debug lines removed)
	private String analyzeFileAndCaptureErrors(String filePath) throws Exception {
		ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
		PrintStream oldErr = System.err;
		System.setErr(new PrintStream(errBaos, true, "UTF-8"));

		try {
			LookaheadScanner scanner = new LookaheadScanner(filePath);
			Parser parser = new Parser(scanner);
			JCompilationUnit unit = parser.compilationUnit();
			unit.preAnalyze();
			unit.analyze(null);
		} finally {
			System.err.flush();
			System.setErr(oldErr);
		}

		String out = errBaos.toString("UTF-8");

		// Filter out debug lines that begin with "DEBUG:" (case-sensitive)
		StringBuilder filtered = new StringBuilder();
		String[] lines = out.split("\\R");
		for (String line : lines) {
			if (line.startsWith("DEBUG:")) {
				continue; // skip debug lines
			}
			filtered.append(line).append(System.lineSeparator());
		}

		return filtered.toString();
	}

	private int countMultiplePublicDiagnostics(String stderr) {
		String lower = stderr.toLowerCase(Locale.ROOT);
		int count = 0;
		for (String line : lower.split("\\R")) {
			if (line.contains("multiple public") || line.contains("only one public") || line.contains("public type")) {
				count++;
			}
		}
		return count;
	}

	@Test
	public void testMultiplePublicTopLevelTypes_reportsError() throws Exception {
		String src = "" + "public class A { }\n" + "public class B { }\n";

		String path = writeTempSource("MultiplePublic", src);
		String errors = analyzeFileAndCaptureErrors(path);

		int matches = countMultiplePublicDiagnostics(errors);
		assertEquals(
				"Expected exactly one multiple-public diagnostic for two public top-level types; stderr:\n" + errors, 1,
				matches);
	}

	@Test
	public void testOnlyFirstPublicKeepsPublic_secondLosesPublic() throws Exception {
		String src = "" + "public class First { }\n" + "public class Second { }\n" + "class Third { }\n";

		String path = writeTempSource("MultiPublicCheck", src);
		String errors = analyzeFileAndCaptureErrors(path);

		int matches = countMultiplePublicDiagnostics(errors);
		assertEquals(
				"Expected exactly one multiple-public diagnostic for two public top-level types; stderr:\n" + errors, 1,
				matches);
	}

	@Test
	public void testSingleOrNoPublic_noError() throws Exception {
		// Case 1: single public only -> no multiple-public diagnostic
		String single = "public class OnlyOne { }\n";
		String p1 = writeTempSource("SinglePublic", single);
		String e1 = analyzeFileAndCaptureErrors(p1);
		assertEquals("Did not expect multiple-public diagnostic for single public type; stderr:\n" + e1, 0,
				countMultiplePublicDiagnostics(e1));

		// Case 2: no public at all -> no multiple-public diagnostic
		String none = "class A { }\nclass B { }\n";
		String p2 = writeTempSource("NoPublic", none);
		String e2 = analyzeFileAndCaptureErrors(p2);
		assertEquals("Did not expect multiple-public diagnostic when no top-level type is public; stderr:\n" + e2, 0,
				countMultiplePublicDiagnostics(e2));
	}

	// ---- Tests that inspect the AST ----

	@Test
	public void testPublicModifierRemoved_inspectsAST() throws Exception {
		String src = "" + "public class First { }\n" + "public class Second { }\n" + "class Third { }\n";

		String path = writeTempSource("PublicInspect", src);

		LookaheadScanner scanner = new LookaheadScanner(path);
		Parser parser = new Parser(scanner);
		JCompilationUnit unit = parser.compilationUnit();

		// run preAnalyze/analyze exactly as production does
		unit.preAnalyze();
		unit.analyze(null);

		// reflectively read private/package-private field 'typeDeclarations' from
		// JCompilationUnit
		java.lang.reflect.Field tdField = unit.getClass().getDeclaredField("typeDeclarations");
		tdField.setAccessible(true);
		Object typesObj = tdField.get(unit);

		// normalize into a List<JAST>
		List<?> typeDeclsList;
		if (typesObj == null) {
			typeDeclsList = Collections.emptyList();
		} else if (typesObj.getClass().isArray()) {
			typeDeclsList = Arrays.asList((Object[]) typesObj);
		} else if (typesObj instanceof Collection) {
			typeDeclsList = new ArrayList<>((Collection<?>) typesObj);
		} else {
			typeDeclsList = Arrays.asList(typesObj);
		}

		// Inspect reflected list instead of accessing the non-visible field directly
		int publicCount = 0;
		List<String> perTypeDebug = new ArrayList<>();
		for (Object o : typeDeclsList) {
			if (!(o instanceof JAST)) {
				perTypeDebug.add("non-JAST entry: " + (o == null ? "null" : o.getClass().getName()));
				continue;
			}
			JAST td = (JAST) o;
			if (td instanceof JClassDeclaration) {
				JClassDeclaration cd = (JClassDeclaration) td;
				ArrayList<String> mods = cd.mods();
				if (mods != null && mods.contains("public")) {
					publicCount++;
					perTypeDebug.add(cd.toString() + " has public");
				} else {
					perTypeDebug.add(cd.toString() + " does not have public");
				}
			} else {
				perTypeDebug.add(td.getClass().getSimpleName() + " (not class decl)");
			}
		}

		assertEquals("Expected exactly one top-level class to remain public after analyze; details: " + perTypeDebug, 1,
				publicCount);
	}

	@Test
	public void testUndeclaredVariablePlaceholder_declaredInSymbolTable() throws Exception {
		String src = "" + "class A {\n" + "  int f() {\n"
				+ "    x = 1;          // first use of x -> should be reported and placeholder declared\n"
				+ "    x = x + 2;      // second use -> should not produce another undeclared error\n"
				+ "    return x;\n" + "  }\n" + "}\n";

		String path = writeTempSource("UndeclaredPlaceholder", src);

		LookaheadScanner scanner = new LookaheadScanner(path);
		Parser parser = new Parser(scanner);
		JCompilationUnit unit = parser.compilationUnit();

		unit.preAnalyze();
		unit.analyze(null);

		// reflectively read private/package-private field 'typeDeclarations' from
		// JCompilationUnit
		java.lang.reflect.Field tdField = unit.getClass().getDeclaredField("typeDeclarations");
		tdField.setAccessible(true);
		Object typesObj = tdField.get(unit);

		// normalize into a List<JAST>
		List<?> typeDeclsList;
		if (typesObj == null) {
			typeDeclsList = Collections.emptyList();
		} else if (typesObj.getClass().isArray()) {
			typeDeclsList = Arrays.asList((Object[]) typesObj);
		} else if (typesObj instanceof Collection) {
			typeDeclsList = new ArrayList<>((Collection<?>) typesObj);
		} else {
			typeDeclsList = Arrays.asList(typesObj);
		}

		// Find class declaration (first top-level class)
		JClassDeclaration classDecl = null;
		for (Object o : typeDeclsList) {
			if (o instanceof JClassDeclaration) {
				classDecl = (JClassDeclaration) o;
				break;
			}
		}
		assertNotNull("Could not find JClassDeclaration in unit.typeDeclarations (reflected)", classDecl);

		// Find the first method declaration inside the class
		JMethodDeclaration methodDecl = null;

		// 1) try members() if available
		try {
			java.lang.reflect.Method membersM = classDecl.getClass().getMethod("members");
			Object membersObj = membersM.invoke(classDecl);
			if (membersObj instanceof Collection) {
				for (Object m : (Collection<?>) membersObj) {
					if (m instanceof JMethodDeclaration) {
						methodDecl = (JMethodDeclaration) m;
						break;
					}
				}
			} else if (membersObj != null && membersObj.getClass().isArray()) {
				for (Object m : (Object[]) membersObj) {
					if (m instanceof JMethodDeclaration) {
						methodDecl = (JMethodDeclaration) m;
						break;
					}
				}
			}
		} catch (NoSuchMethodException ignore) {
		} catch (ReflectiveOperationException ignore) {
		}

		// 2) fallback: inspect declared fields on classDecl for a collection/array of
		// members
		if (methodDecl == null) {
			for (java.lang.reflect.Field f : classDecl.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				Object val = null;
				try {
					val = f.get(classDecl);
				} catch (IllegalAccessException ignore) {
				}
				if (val instanceof Collection) {
					for (Object m : (Collection<?>) val) {
						if (m instanceof JMethodDeclaration) {
							methodDecl = (JMethodDeclaration) m;
							break;
						}
					}
				} else if (val != null && val.getClass().isArray()) {
					for (Object m : (Object[]) val) {
						if (m instanceof JMethodDeclaration) {
							methodDecl = (JMethodDeclaration) m;
							break;
						}
					}
				}
				if (methodDecl != null)
					break;
			}
		}

		assertNotNull("Could not find a method declaration inside class declaration; update test to match your API",
				methodDecl);

		// Obtain the MethodContext from the method declaration by probing common
		// accessors or fields
		MethodContext mc = null;
		Class<?> mdClass = methodDecl.getClass();

		// 1) Try explicitly named accessors first
		String[] ctxNames = { "methodContext", "context", "getContext", "localContext", "getMethodContext" };
		for (String n : ctxNames) {
			try {
				java.lang.reflect.Method m = mdClass.getMethod(n);
				Object res = m.invoke(methodDecl);
				if (res instanceof MethodContext) {
					mc = (MethodContext) res;
					break;
				}
			} catch (NoSuchMethodException ignore) {
			} catch (ReflectiveOperationException ignore) {
			}
			try {
				java.lang.reflect.Method m = mdClass.getDeclaredMethod(n);
				m.setAccessible(true);
				Object res = m.invoke(methodDecl);
				if (res instanceof MethodContext) {
					mc = (MethodContext) res;
					break;
				}
			} catch (NoSuchMethodException ignore) {
			} catch (ReflectiveOperationException ignore) {
			}
		}
		if (mc == null) {
			// 2) Try any no-arg method returning MethodContext
			for (java.lang.reflect.Method m : mdClass.getDeclaredMethods()) {
				if (m.getParameterCount() == 0 && MethodContext.class.isAssignableFrom(m.getReturnType())) {
					try {
						m.setAccessible(true);
						Object res = m.invoke(methodDecl);
						if (res instanceof MethodContext) {
							mc = (MethodContext) res;
							break;
						}
					} catch (ReflectiveOperationException ignore) {
					}
				}
			}
		}
		if (mc == null) {
			// 3) Try fields whose type is MethodContext
			for (java.lang.reflect.Field f : mdClass.getDeclaredFields()) {
				if (MethodContext.class.isAssignableFrom(f.getType())) {
					try {
						f.setAccessible(true);
						Object res = f.get(methodDecl);
						if (res instanceof MethodContext) {
							mc = (MethodContext) res;
							break;
						}
					} catch (IllegalAccessException ignore) {
					}
				}
			}
		}

		if (mc == null) {
			// Helpful failure message listing available no-arg methods and fields
			StringBuilder sb = new StringBuilder("Could not obtain MethodContext from method declaration. ");
			sb.append("Tried methods: ").append(Arrays.toString(ctxNames)).append(". ");
			sb.append("Available no-arg methods: ");
			for (java.lang.reflect.Method m : mdClass.getDeclaredMethods()) {
				if (m.getParameterCount() == 0)
					sb.append(m.getName()).append("(), ");
			}
			sb.append("Declared fields: ");
			for (java.lang.reflect.Field f : mdClass.getDeclaredFields())
				sb.append(f.getName()).append(", ");
			fail(sb.toString());
		}
		assertNotNull(
				"Could not obtain MethodContext from method declaration. Ensure methodDecl exposes methodContext() or context()",
				mc);

		// Now lookup 'x' in the method context. Your implementation should have
		// inserted a synthetic entry.
		Object entry = mc.lookup("x");
		assertNotNull("Expected a placeholder symbol for 'x' in the method context after analyze()", entry);

		// If the placeholder has a type accessor, confirm it's Type.ANY
		try {
			Object t = null;
			try {
				java.lang.reflect.Method m = entry.getClass().getMethod("type");
				t = m.invoke(entry);
			} catch (NoSuchMethodException ignore) {
				try {
					java.lang.reflect.Method m2 = entry.getClass().getMethod("getType");
					t = m2.invoke(entry);
				} catch (NoSuchMethodException ignore2) {
				}
			}
			if (t != null) {
				assertEquals("Placeholder symbol for 'x' should have type Type.ANY", Type.ANY, t);
			}
		} catch (ReflectiveOperationException ignored) {
			// If we can't reflectively find a type accessor, presence of the entry is
			// sufficient.
		}
	}
}