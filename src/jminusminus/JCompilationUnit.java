// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * The abstract syntax tree (AST) node representing a compilation unit, and so
 * the root of the AST.
 * <p>
 * The AST is produced by the Parser. Once the AST has been built, three
 * successive methods are invoked:
 * <ol>
 * <li>Method preAnalyze() is invoked for making a first pass at type analysis,
 * recursively reaching down to the member headers for declaring types and
 * member interfaces in the environment (contexts). preAnalyze() also creates a
 * partial class file (in memory) for recording member header information.</li>
 *
 * <li>Method analyze() is invoked for type-checking field initializations and
 * method bodies, and determining the types of all expressions. A certain amount
 * of tree surgery is also done here. And stack frame offsets are computed for
 * method parameters and local variables.</li>
 *
 * <li>Method codegen() is invoked for generating code for the compilation unit
 * to a class file. For each type declaration, it instantiates a CLEmitter
 * object (an abstraction of the class file) and then invokes methods on that
 * CLEmitter for generating instructions. At the end of each type declaration, a
 * method is invoked on the CLEmitter which writes the class out to the file
 * system either as .class file or as a .s (SPIM) file. Of course, codegen()
 * makes recursive calls down the tree, to the {@code codegen} methods at each
 * node, for generating the appropriate instructions.</li>
 * </ol>
 */
class JCompilationUnit extends JAST {
	// Name of the source file.
	private String fileName;

	// Set of diagnostics already reported (deduplication key: fileName:message)
	private Set<String> reportedDiagnostics = new HashSet<String>();

	// Package name.
	private TypeName packageName;

	// List of imports.
	private ArrayList<TypeName> imports;

	// List of type declarations.
	private ArrayList<JAST> typeDeclarations;

	// List of CLFile objects corresponding to the type declarations in this
	// compilation unit.
	private ArrayList<CLFile> clFiles;

	// For imports and type declarations.
	private CompilationUnitContext context;

	// Whether a semantic error has been found.
	private boolean isInError;

	// Whether we've already reported the multiple-public-types error for this
	// compilation unit.
	private boolean multiplePublicReported = false;

	/**
	 * Constructs an AST node for a compilation unit.
	 *
	 * @param fileName         the name of the source file.
	 * @param line             line in which the compilation unit occurs in the
	 *                         source file.
	 * @param packageName      package name.
	 * @param imports          a list of imports.
	 * @param typeDeclarations type declarations.
	 */
	public JCompilationUnit(String fileName, int line, TypeName packageName, ArrayList<TypeName> imports,
			ArrayList<JAST> typeDeclarations) {
		super(line);
		this.fileName = fileName;
		this.packageName = packageName;
		this.imports = imports;
		this.typeDeclarations = typeDeclarations;
		clFiles = new ArrayList<CLFile>();
		compilationUnit = this;

		// Normalize public modifiers immediately so no later phase ever observes
		// multiple public types.
		if (typeDeclarations != null) {
			JClassDeclaration firstPublic = null;
			for (JAST td : typeDeclarations) {
				if (td instanceof JClassDeclaration) {
					JClassDeclaration cd = (JClassDeclaration) td;
					ArrayList<String> m = cd.mods();
					if (m != null && m.contains("public")) {
						if (firstPublic == null) {
							firstPublic = cd;
						} else {
							if (!multiplePublicReported) {
								reportSemanticError(cd.line(),
										"Multiple public type declarations in compilation unit; only one public type allowed");
								multiplePublicReported = true;
							}
							m.remove("public");
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the package in which this compilation unit is defined.
	 *
	 * @return the package in which this compilation unit is defined.
	 */
	public String packageName() {
		return packageName == null ? "" : packageName.toString().replace(".", "/");
	}

	/**
	 * Returns the list of CLFile objects corresponding to the type declarations in
	 * this compilation unit.
	 *
	 * @return the list of CLFile objects corresponding to the type declarations in
	 *         this compilation unit.
	 */
	public ArrayList<CLFile> clFiles() {
		return clFiles;
	}

	/**
	 * Returns true if a semantic error has occurred up to now, and false otherwise.
	 *
	 * @return true if a semantic error has occurred up to now, and false
	 *         otherwise..
	 */
	public boolean errorHasOccurred() {
		return isInError;
	}

	/**
	 * Reports a semantic error.
	 *
	 * Deduplicates by fileName + formatted message so the same problem for the same
	 * source file is printed only once while preserving separate reports for
	 * different files.
	 *
	 * @param line      line in which the error occurred in the source file.
	 * @param message   message identifying the error.
	 * @param arguments related values.
	 */
	public void reportSemanticError(int line, String message, Object... arguments) {
		String formatted = String.format(message, arguments);
		// Dedupe per file + line + message
		String key = fileName + ":" + line + ":" + formatted;
		if (reportedDiagnostics.contains(key)) {
			isInError = true;
			return;
		}
		reportedDiagnostics.add(key);
		isInError = true;
		System.err.printf("%s:%d: error: ", fileName, line);
		System.err.printf(formatted);
		System.err.println();

		System.out.printf("%s:%d: error: ", fileName, line);
		System.out.printf(formatted);
		System.out.println();
	}

	/**
	 * Constructs a context for the compilation unit, initializing it with imported
	 * types. Then pre-analyzes the unit's type declarations, adding their types to
	 * the context.
	 */
	public void preAnalyze() {
		context = new CompilationUnitContext();

		// Declare the two implicit types java.lang.Object and java.lang.String.
		context.addType(0, Type.OBJECT);
		context.addType(0, Type.STRING);

		// Declare any imported types.
		for (TypeName imported : imports) {
			try {
				Class<?> classRep = Class.forName(imported.toString());
				context.addType(imported.line(), Type.typeFor(classRep));
			} catch (Exception e) {
				JAST.compilationUnit.reportSemanticError(imported.line(), "Unable to find %s", imported.toString());
			}
		}

		// Defensive normalization again in preAnalyze in case modifications happened
		// after construction.
		if (typeDeclarations != null) {
			JClassDeclaration firstPublic = null;
			for (JAST td : typeDeclarations) {
				if (td instanceof JClassDeclaration) {
					JClassDeclaration cd = (JClassDeclaration) td;
					ArrayList<String> mods = cd.mods();
					if (mods != null && mods.contains("public")) {
						if (firstPublic == null) {
							firstPublic = cd;
						} else {
							if (!multiplePublicReported) {
								reportSemanticError(cd.line(),
										"Multiple public type declarations in compilation unit; only one public type allowed");
								multiplePublicReported = true;
							}
							mods.remove("public");
						}
					}
				}
			}
		}

		// Declare the locally declared type(s).
		CLEmitter.initializeByteClassLoader();
		for (JAST typeDeclaration : typeDeclarations) {
			((JTypeDecl) typeDeclaration).declareThisType(context);
		}

		// Pre-analyze the locally declared type(s). Generate (partial) Class instances,
		// reflecting only the member declaration information.
		CLEmitter.initializeByteClassLoader();
		for (JAST typeDeclaration : typeDeclarations) {
			((JTypeDecl) typeDeclaration).preAnalyze(context);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public JAST analyze(Context context) {
		// Defensive normalization in analyze() as well.
		if (typeDeclarations != null) {
			JClassDeclaration firstPublic = null;
			for (JAST td : typeDeclarations) {
				if (td instanceof JClassDeclaration) {
					JClassDeclaration cd = (JClassDeclaration) td;
					ArrayList<String> mods = cd.mods();
					if (mods != null && mods.contains("public")) {
						if (firstPublic == null) {
							firstPublic = cd;
						} else {
							if (!multiplePublicReported) {
								reportSemanticError(cd.line(),
										"Multiple public type declarations in compilation unit; only one public type allowed");
								multiplePublicReported = true;
							}
							mods.remove("public");
						}
					}
				}
			}
		}

		for (JAST typeDeclaration : typeDeclarations) {
			typeDeclaration.analyze(this.context);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void codegen(CLEmitter output) {
		for (JAST typeDeclaration : typeDeclarations) {
			typeDeclaration.codegen(output);
			output.write();
			clFiles.add(output.clFile());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void toJSON(JSONElement json) {
		JSONElement e = new JSONElement();
		json.addChild("JCompilationUnit:" + line, e);
		e.addAttribute("source", fileName);
		if (packageName != null) {
			e.addAttribute("package", packageName());
		}
		if (imports != null) {
			ArrayList<String> value = new ArrayList<String>();
			for (TypeName imported : imports) {
				value.add(String.format("\"%s\"", imported.toString()));
			}
			e.addAttribute("imports", value);
		}
		if (context != null) {
			context.toJSON(e);
		}
		if (typeDeclarations != null) {
			for (JAST typeDeclaration : typeDeclarations) {
				typeDeclaration.toJSON(e);
			}
		}
	}
}