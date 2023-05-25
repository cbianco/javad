///usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS jcenter
//DEPS org.jd:jd-core:1.1.3	 
//DEPS info.picocli:picocli:4.6.3


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

@Command(
	name = "javad", 
	mixinStandardHelpOptions = true, 
	version = "javad 0.1",
	description = "javad made with jbang"
)
class javad implements Callable<Integer> {

	@Parameters(index = "0", description = "The java class to decompile")
	private Path javaClass;

	public static void main(String... args) {
		int exitCode = new CommandLine(new javad()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception { // your business logic goes here...
		
		ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();

		Loader loader = new Loader() {
			@Override
			public byte[] load(String internalName) throws LoaderException {
				try (InputStream is = Files.newInputStream(Paths.get(internalName))) {
		
					if (is == null) {
						return null;
					} else {
						try (InputStream in=is; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
							byte[] buffer = new byte[1024];
							int read = in.read(buffer);
			
							while (read > 0) {
								out.write(buffer, 0, read);
								read = in.read(buffer);
							}
			
							return out.toByteArray();
						} catch (IOException e) {
							throw new LoaderException(e);
						}
					}
				}
				catch (IOException e) {
					throw new LoaderException(e);
				}
			}
		
			@Override
			public boolean canLoad(String internalName) {
				return this.getClass().getResource("/" + internalName + ".class") != null;
			}
		};

		Printer printer = new Printer() {
			protected static final String TAB = "  ";
			protected static final String NEWLINE = "\n";
		
			protected int indentationCount = 0;
			protected StringBuilder sb = new StringBuilder();
		
			@Override public String toString() { return sb.toString(); }
		
			@Override public void start(int maxLineNumber, int majorVersion, int minorVersion) {}
			@Override public void end() {}
		
			@Override public void printText(String text) { sb.append(text); }
			@Override public void printNumericConstant(String constant) { sb.append(constant); }
			@Override public void printStringConstant(String constant, String ownerInternalName) { sb.append(constant); }
			@Override public void printKeyword(String keyword) { sb.append(keyword); }
			@Override public void printDeclaration(int type, String internalTypeName, String name, String descriptor) { sb.append(name); }
			@Override public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }
		
			@Override public void indent() { this.indentationCount++; }
			@Override public void unindent() { this.indentationCount--; }
		
			@Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
			@Override public void endLine() { sb.append(NEWLINE); }
			@Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }
		
			@Override public void startMarker(int type) {}
			@Override public void endMarker(int type) {}
		};

		decompiler.decompile(loader, printer, javaClass.toAbsolutePath().toString());
		
		String source = printer.toString();

		System.out.println(source);

		return 0;
	}
}
