package com.integral.grimoire.incelmc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import groovy.lang.Closure;

public class SourceCopyTask extends DefaultTask {
	@InputFiles
	SourceDirectorySet source;

	@Input
	HashMap<String, Object> replacements = new HashMap<String, Object>();

	@Input
	ArrayList<String> includes = new ArrayList<String>();

	@OutputDirectory
	File output;

	@SuppressWarnings("unchecked")
	@TaskAction
	public void doTask() throws IOException {
		this.getLogger().debug("INPUTS >> " + this.source);
		this.getLogger().debug("OUTPUTS >> " + this.getOutput());

		// get the include/exclude patterns from the source (this is different than what's returned by getFilter)
		PatternSet patterns = new PatternSet();
		patterns.setIncludes(this.source.getIncludes());
		patterns.setExcludes(this.source.getExcludes());

		// get output
		File out = this.getOutput();
		if (out.exists()) {
			this.deleteDir(out);
		}

		out.mkdirs();
		out = out.getCanonicalFile();

		// resolve replacements
		HashMap<String, String> repl = new HashMap<String, String>(this.replacements.size());
		for (Entry<String, Object> e : this.replacements.entrySet()) {
			if (e.getKey() == null || e.getValue() == null)
			{
				continue; // we dont deal with nulls.
			}

			Object val = e.getValue();
			while (val instanceof Closure) {
				val = ((Closure<Object>) val).call();
			}

			repl.put(Pattern.quote(e.getKey()), val.toString());
		}

		this.getLogger().debug("REPLACE >> " + repl);

		// start traversing tree
		for (DirectoryTree dirTree : this.source.getSrcDirTrees()) {
			File dir = dirTree.getDir();
			this.getLogger().debug("PARSING DIR >> " + dir);

			// handle nonexistant srcDirs
			if (!dir.exists() || !dir.isDirectory()) {
				continue;
			} else {
				dir = dir.getCanonicalFile();
			}

			// this could be written as .matching(source), but it doesn't actually work
			// because later on gradle casts it directly to PatternSet and crashes
			FileTree tree = this.getProject().fileTree(dir).matching(this.source.getFilter()).matching(patterns);

			for (File file : tree) {
				File dest = this.getDest(file, dir, out);
				dest.getParentFile().mkdirs();
				dest.createNewFile();

				if (this.isIncluded(file)) {
					this.getLogger().debug("PARSING FILE IN >> " + file);
					String text = Files.toString(file, Charsets.UTF_8);

					for (Entry<String, String> entry : repl.entrySet()) {
						text = text.replaceAll(entry.getKey(), entry.getValue());
					}

					this.getLogger().debug("PARSING FILE OUT >> " + dest);
					Files.write(text, dest, Charsets.UTF_8);
				} else {
					Files.copy(file, dest);
				}
			}
		}
	}

	private File getDest(File in, File base, File baseOut) throws IOException {
		String relative = in.getCanonicalPath().replace(base.getCanonicalPath(), "");
		return new File(baseOut, relative);
	}

	private boolean isIncluded(File file) throws IOException {
		if (this.includes.isEmpty())
			return true;

		String path = file.getCanonicalPath().replace('\\', '/');
		for (String include : this.includes) {
			if (path.endsWith(include.replace('\\', '/')))
				return true;
		}

		return false;
	}

	private boolean deleteDir(File dir) {
		if (dir.exists()) {
			File[] files = dir.listFiles();
			if (null != files) {
				for (File file : files) {
					if (file.isDirectory()) {
						this.deleteDir(file);
					} else {
						file.delete();
					}
				}
			}
		}
		return (dir.delete());
	}

	public File getOutput() {
		return this.output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public void setSource(SourceDirectorySet source) {
		this.source = source;
	}

	public FileCollection getSource() {
		return this.source;
	}

	public void replace(String key, Object val) {
		this.replacements.put(key, val);
	}

	public void replace(Map<String, Object> map) {
		this.replacements.putAll(map);
	}

	public HashMap<String, Object> getReplacements() {
		return this.replacements;
	}

	public void include(String str) {
		this.includes.add(str);
	}

	public void include(List<String> strs) {
		this.includes.addAll(strs);
	}

	public ArrayList<String> getIncudes() {
		return this.includes;
	}
}
