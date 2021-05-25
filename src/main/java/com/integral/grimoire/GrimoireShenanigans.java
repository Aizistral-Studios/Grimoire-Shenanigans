package com.integral.grimoire;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.filters.ReplaceTokens;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import com.integral.grimoire.fg12.ExtraShenanigans_12;
import com.integral.grimoire.fg23.ExtraShenanigans_23;
import com.integral.grimoire.tasks.ClearBuildTask;
import com.integral.grimoire.tasks.ClearResourcesTask;

public class GrimoireShenanigans implements Plugin<Project> {
	public Project project;
	public ExtraShenanigans extraShenanigans;

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void apply(Project project) {
		this.project = project;
		this.applyExternalPlugin("forge");
		project.getExtensions().create("grimoire", GrimoireExtension.class, this);

		// Make sure to clear build cache before compiling/processing classes/resources.
		// Required for proper inflation/token replacement when building
		try {
			this.project.getTasks().getByName("clearBuildCache");
			this.project.getTasks().getByName("clearResourcesCache");
		} catch (Exception ex) {
			this.project.getTasks().getByName("sourceMainJava").dependsOn(this.makeTask("clearBuildCache", ClearBuildTask.class));
			this.project.getTasks().getByName("processResources").dependsOn(this.makeTask("clearResourcesCache", ClearResourcesTask.class));
		}

		// Check if user enabled our incredibly very shenanigans and make them happen if so
		if (this.areGrimoireShenanigansEnabled()) {
			try {
				Class<?> fgClass = Class.forName("net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin");
				if (fgClass != null) {
					this.extraShenanigans = new ExtraShenanigans_23(this);
				}
			} catch (Exception ex) {
				this.extraShenanigans = new ExtraShenanigans_12(this);
			}

			if (this.extraShenanigans != null) {
				this.applyGrimoireShenanigans();
			} else {
				this.project.getLogger().error("Failed to instantiate ExtraShenanigans!");
			}
		}

		// Inform the user whether or not Grimoire shenanigans are enabled.
		// Doing this after evaluate so that banner gets printed first
		this.project.afterEvaluate(new Action() {

			@Override
			public void execute(Object arg) {
				project.getLogger().lifecycle("Grimoire shenanigans " +
						(GrimoireShenanigans.this.areGrimoireShenanigansEnabled() ? "enabled" : "disabled"));

				if (GrimoireShenanigans.this.areGrimoireShenanigansEnabled()) {
					project.getLogger().lifecycle("Using Mixin refmap name: " +
							GrimoireShenanigans.this.getMixinRefmapName());
				}
			}

		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected void applyGrimoireShenanigans() {
		// Add important repositories for Mixin annotation processor
		this.addMavenRepo(this.project, "spongepowered-repo", "https://repo.spongepowered.org/maven/");
		this.project.getRepositories().mavenCentral();

		// Make custom task for copying srg-files into build/srgs directory
		Task copyTask = this.extraShenanigans.copySrgsTask();
		JavaCompile compileJava = (JavaCompile) this.project.getTasks().getByName("compileJava");

		if (copyTask != null) {
			copyTask.dependsOn("genSrgs");
			compileJava.dependsOn(copyTask);
		}

		// Add annotation processor for... annotation processing, I suppose
		this.project.getDependencies().add("annotationProcessor", "org.spongepowered:mixin:0.7.11-SNAPSHOT");

		// Create directory and srg/refmap output files for Mixin annotation processor
		this.project.file(this.project.getBuildDir().getName() + "/mixins").mkdirs();
		File mixinSrg = this.project.file(this.project.getBuildDir().getName() + "/mixins/mixins." + this.project.getName() + ".srg");
		File mixinRefMap = this.project.file(this.project.getBuildDir().getName() + "/mixins/" + this.getMixinRefmapName());

		// We probably don't need this, but having it will allow to interact with these
		// properties through buildscript. Probably. Why? Why not.
		this.project.getExtensions().getExtraProperties().set("mixinSrg", mixinSrg);
		this.project.getExtensions().getExtraProperties().set("mixinRefMap", mixinRefMap);

		// Add compiler args to point annotation processor to its input/output files
		try {
			List<String> compilerArgs = compileJava.getOptions().getCompilerArgs();
			compilerArgs.add("-Xlint:-processing");
			compilerArgs.add("-AoutSrgFile=" + mixinSrg.getCanonicalPath());
			compilerArgs.add("-AoutRefMapFile=" + mixinRefMap.getCanonicalPath());
			compilerArgs.add("-AreobfSrgFile=" + this.project.file(this.project.getBuildDir().getName() + "/srgs/mcp-srg.srg").getCanonicalPath());
			compileJava.getOptions().setCompilerArgs(compilerArgs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Make sure default .jar artifact will embed our glorious refmap we've been putting together
		Jar jarTask = (Jar) this.project.getTasks().getByName("jar");
		jarTask.from(mixinRefMap);

		// Why do we need this one again?..
		this.extraShenanigans.extraReobfMap(mixinSrg);

		// Automatically replace any @MIXIN_REFMAP@ tokens in project resources
		ProcessResources processResources = (ProcessResources) this.project.getTasks().getByName("processResources");
		Map<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		Map<String, String> tokenMap = new LinkedHashMap<String, String>();
		tokenMap.put("MIXIN_REFMAP", this.getMixinRefmapName());
		propertyMap.put("tokens", tokenMap);

		processResources.filter(propertyMap, ReplaceTokens.class);
	}

	public boolean areGrimoireShenanigansEnabled() {
		return Boolean.parseBoolean(String.valueOf(this.project.getProperties().get("enableGrimoireShenanigans")));
	}

	public String getMixinRefmapName() {
		// Allow users to define their own refmap name if they wish
		if (this.project.hasProperty("mixinRefmapName"))
			return String.valueOf(this.project.getProperties().get("mixinRefmapName"));
		else
			return this.project.getName() + ".refmap.json";
	}

	public void applyExternalPlugin(String plugin) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("plugin", plugin);
		this.project.apply(map);
	}

	public DefaultTask makeTask(String name) {
		return this.makeTask(name, DefaultTask.class);
	}

	public <T extends Task> T makeTask(String name, Class<T> type) {
		return makeTask(this.project, name, type);
	}

	public MavenArtifactRepository addMavenRepo(Project proj, final String name, final String url) {
		return proj.getRepositories().maven(new Action<MavenArtifactRepository>() {
			@Override
			public void execute(MavenArtifactRepository repo) {
				repo.setName(name);
				repo.setUrl(url);
			}
		});
	}

	public FlatDirectoryArtifactRepository addFlatRepo(Project proj, final String name, final Object... dirs) {
		return proj.getRepositories().flatDir(new Action<FlatDirectoryArtifactRepository>() {
			@Override
			public void execute(FlatDirectoryArtifactRepository repo) {
				repo.setName(name);
				repo.dirs(dirs);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static <T extends Task> T makeTask(Project proj, String name, Class<T> type) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("type", type);
		return (T) proj.task(map, name);
	}

}

