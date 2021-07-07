package com.integral.grimoire.ide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.impldep.aQute.bnd.osgi.Builder;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

import com.integral.grimoire.GrimoireExtension;
import com.integral.grimoire.GrimoireShenanigans;

import groovy.xml.MarkupBuilder;

public class GrimoireEclipse {

	public static void configureEclipse(GrimoireExtension extension, Project project, File srgMappings, File mixinRefmap, File reobfSrg) {
		EclipseModel eclipseModel = (EclipseModel) project.getExtensions().findByName("eclipse");
		if (eclipseModel == null) {
			project.getLogger().lifecycle("[MixinGradle] Skipping eclipse integration, extension not found");
			return;
		}

		eclipseModel.getJdt().getFile().withProperties(it -> {
			it.setProperty("org.eclipse.jdt.core.compiler.processAnnotations", "enabled");
		});

		TaskProvider<EclipseJdtAptTask> settings = project.getTasks().register("eclipseJdtApt", EclipseJdtAptTask.class);
		settings.configure(task -> {
			task.setDescription("Creates the Eclipse JDT APT settings file");
			task.output = project.file(".settings/org.eclipse.jdt.apt.core.prefs");
			task.mappingsIn = reobfSrg;
		});

		project.getTasks().findByName("eclipse").dependsOn("eclipseJdtApt");

		TaskProvider<EclipseFactoryPath> factories = project.getTasks().register("eclipseFactoryPath", EclipseFactoryPath.class);
		factories.configure(task -> {
			task.config = project.getConfigurations().findByName("annotationProcessor");
			task.output = project.file(".factorypath");
		});


		project.getTasks().findByName("eclipse").dependsOn("eclipseFactoryPath");
	}

	static class EclipseJdtAptTask extends DefaultTask {
		@InputFile File mappingsIn;
		@Input File refmapOut = this.getProject().file("build/eclipseJdtApt/mixins.refmap.json");
		@Input File mappingsOut = this.getProject().file("build/eclipseJdtApt/mixins.mappings.tsrg");
		@Input Map<String, String> processorOptions = new TreeMap<>();

		@Input File genTestDir = this.getProject().file("build/.apt_generated_test");
		@Input File genDir = this.getProject().file("build/.apt_generated");

		@OutputFile File output;

		public EclipseJdtAptTask() {
			// NO-OP
		}

		@TaskAction
		public void run() throws Exception {
			if (!this.refmapOut.exists()) {
				this.refmapOut.getParentFile().mkdirs();
				this.refmapOut.createNewFile();
			}

			if (!this.mappingsOut.exists()) {
				this.mappingsOut.getParentFile().mkdirs();
				this.mappingsOut.createNewFile();
			}

			this.genTestDir.mkdirs();
			this.genDir.mkdirs();

			GrimoireExtension extension = this.getProject().getExtensions().findByType(GrimoireExtension.class);
			OrderedProperties props = new OrderedProperties();
			props.put("eclipse.preferences.version", "1");
			props.put("org.eclipse.jdt.apt.aptEnabled", "true");
			props.put("org.eclipse.jdt.apt.reconcileEnabled", "true");
			props.put("org.eclipse.jdt.apt.genSrcDir", this.genDir.getCanonicalPath());
			props.put("org.eclipse.jdt.apt.genSrcTestDir", this.genTestDir.getCanonicalPath());
			props.arg("reobfSrgFile", this.mappingsIn.getCanonicalPath());
			props.arg("outTsrgFile", this.mappingsOut.getCanonicalPath());
			props.arg("outRefMapFile", this.refmapOut.getCanonicalPath());

			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(this.output));
			props.store(writer, null);
			writer.close();
		}
	}

	static class EclipseFactoryPath extends DefaultTask {
		@Input Configuration config;
		@OutputFile File output;

		public EclipseFactoryPath() {
			// NO-OP
		}

		@TaskAction
		public void run() {
			try {
				OutputStream stream = new FileOutputStream(this.output);
				OutputStreamWriter writer = new OutputStreamWriter(stream);
				writer.write("<factorypath>");

				this.config.getResolvedConfiguration().getResolvedArtifacts().forEach(dep -> {
					try {
						String id = dep.getFile().getAbsolutePath();
						writer.write(System.lineSeparator());
						writer.write("  <factorypathentry kind='EXTJAR' id='" + id + "' enabled='true' runInBatchMode='false' />");
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}

				});

				writer.write(System.lineSeparator());
				writer.write("</factorypath>");

				writer.close();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	@SuppressWarnings("serial")
	static class OrderedProperties extends Properties {
		Set<Object> order = new LinkedHashSet<Object>();

		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(this.order);
		}

		@Override
		public synchronized Object put(Object key, Object value) {
			this.order.add(key);
			return super.put(key, value);
		}

		public Object arg(String key, String value) {
			return this.put("org.eclipse.jdt.apt.processorOptions/" + key, value);
		}
	}

}