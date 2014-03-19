package net.roboconf.plugin.bash.template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import net.roboconf.core.model.runtime.Instance;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class InstanceTemplateHelper {
	
	private static MustacheFactory mf = new DefaultMustacheFactory();

	/**
	 * Reads the import values of the instances and injects them into the template file.
	 * See test resources to see the associated way to write templates 
	 * @param instance 
	 * @param templateFile
	 * @param writer
	 * @throws IOException
	 */
	public static void injectInstanceImports(Instance instance, String templateFile, Writer writer) throws IOException {
	    Mustache mustache = mf.compile(templateFile);
	    mustache.execute(writer, new InstanceBean(instance)).flush();
	}
	
	public static void injectInstanceImports(Instance instance, File templateFile, Writer writer) throws IOException {
		injectInstanceImports(instance, templateFile.getAbsolutePath(), writer);
	}
	
	public static void injectInstanceImports(Instance instance, String templateFile, File out) throws IOException {
		injectInstanceImports(instance, templateFile, new FileWriter(out));
	}
	
	public static void injectInstanceImports(Instance instance, File templateFile, File out) throws IOException {
		injectInstanceImports(instance, templateFile.getAbsolutePath(), out);
	}
}
