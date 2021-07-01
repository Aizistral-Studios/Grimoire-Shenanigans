package com.integral.grimoire;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.AccessRule;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.tooling.model.eclipse.EclipseExternalDependency;

import com.integral.grimoire.chadmc.ChadShenanigans;
import com.integral.grimoire.incelmc.IncelShenanigans;
import com.integral.grimoire.tasks.ClearBuildTask;
import com.integral.grimoire.tasks.ClearResourcesTask;

public class GrimoireShenanigans implements Plugin<Project> {
	public Project project;
	public ExtraShenanigans extraShenanigans;
	public Rule forbiddenRule;

	protected boolean enabled = false;

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void apply(Project project) {
		this.enabled = true;
		this.project = project;
		this.applyExternalPlugin("maven-publish");
		this.applyExternalPlugin("eclipse");
		project.getExtensions().create("grimoire", GrimoireExtension.class, this);

		// Make sure to clear build cache before compiling/processing classes/resources.
		// Required for proper inflation/token replacement when building
		try {
			this.project.getTasks().getByName("clearBuildCache");
			this.project.getTasks().getByName("clearResourcesCache");
		} catch (Exception ex) {
			this.project.getTasks().getByName("compileJava").dependsOn(this.makeTask("clearBuildCache", ClearBuildTask.class));
			this.project.getTasks().getByName("processResources").dependsOn(this.makeTask("clearResourcesCache", ClearResourcesTask.class));
		}

		// Check if user enabled our incredibly very shenanigans and make them happen if so
		if (this.areGrimoireShenanigansEnabled()) {
			try {
				Class.forName("net.minecraftforge.gradle.user.patch.ForgeUserPlugin");
				this.extraShenanigans = new ChadShenanigans(this);
			} catch (Exception ex) {
				try {
					Class.forName("net.minecraftforge.gradle.userdev.UserDevPlugin");
					this.extraShenanigans = new IncelShenanigans(this);
				} catch (Exception ex2) {
					throw new RuntimeException("Could not locate any valid ForgeGradle version!", ex2);
				}
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

				if (GrimoireShenanigans.this.extraShenanigans.isChadMC())
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
		this.addMavenRepo(this.project, "spongepowered-repo", "https://repo.spongepowered.org/repository/maven-public/");
		this.addMavenRepo(this.project, "juan-github", "https://github.com/juanmuscaria/maven/raw/master/");
		this.project.getRepositories().mavenCentral();

		// Make custom task for copying srg-files into build/srgs directory
		Task copyTask = this.extraShenanigans.copySrgsTask();
		JavaCompile compileJava = (JavaCompile) this.project.getTasks().getByName("compileJava");

		if (copyTask != null) {
			copyTask.dependsOn("genSrgs");
			compileJava.dependsOn(copyTask);
		}

		// Add annotation processor for... annotation processing, I suppose
		this.project.getDependencies().add("annotationProcessor", this.extraShenanigans.getAnnotationProccessor());

		if (!this.isGrimoireItself()) {
			if (!this.disableGrimoireDependency()) {
				if (this.extraShenanigans.isChadMC()) {
					this.project.getDependencies().add("compileOnly", "io.github.crucible.grimoire:Grimoire-mc1.7.10-api:" + this.grimoireVersion());
					this.project.getDependencies().add("runtimeOnly", "io.github.crucible.grimoire:Grimoire-mc1.7.10-dev:" + this.grimoireVersion());
				} else {
					this.project.getDependencies().add("compileOnly", "io.github.crucible.grimoire:Grimoire-mc1.12.2-api:" + this.grimoireVersion());
					this.project.getDependencies().add("runtimeOnly", "io.github.crucible.grimoire:Grimoire-mc1.12.2-dev:" + this.grimoireVersion());
				}
			}

			EclipseModel eclipse = (EclipseModel) this.project.getExtensions().getByName("eclipse");
			List<AccessRule> basicRules = new ArrayList<>();
			this.addRule(basicRules, this.getForbiddenRule(), "com/gamerforea/eventhelper/config/**");
			this.addRule(basicRules, this.getForbiddenRule(), "com/gamerforea/eventhelper/util/EventUtils**");

			// Forbid everything with package longer than 3 symbols
			this.addRule(basicRules, this.getForbiddenRule(), "io/github/crucible/grimoire/common/????*/**");
			this.addRule(basicRules, this.getForbiddenRule(), "io/github/crucible/grimoire/mc1_7_10/????*/**");
			this.addRule(basicRules, this.getForbiddenRule(), "io/github/crucible/grimoire/mc1_12_2/????*/**");
			this.addRule(basicRules, this.getForbiddenRule(), "io/github/crucible/omniconfig/????*/**");

			// Also forbid omniconfig/lib specifically
			this.addRule(basicRules, this.getForbiddenRule(), "io/github/crucible/omniconfig/lib/**");

			eclipse.classpath(path -> {
				path.file(file -> {
					file.whenMerged(obj -> {
						if (obj instanceof Classpath) {
							Classpath clp = (Classpath) obj;

							clp.getEntries().forEach(entryObj -> {
								if (entryObj instanceof AbstractClasspathEntry) {
									AbstractClasspathEntry abs = (AbstractClasspathEntry) entryObj;

									if ("lib".equals(abs.getKind())) {
										abs.getAccessRules().addAll(basicRules);
									}
								}
							});
						}
					});
				});
			});
		}

		if (this.extraShenanigans.isChadMC()) {
			// Create directory and srg/refmap output files for Mixin annotation processor
			File mixinBuild = new File(this.project.getBuildDir(), "mixins");
			mixinBuild.mkdirs();

			File mixinSrg = new File(mixinBuild, "mixins." + this.project.getName() + ".srg");
			File mixinRefMap = new File(mixinBuild, this.getMixinRefmapName().replace("/", "$").replace(File.separator, "$"));

			try {
				if (!mixinSrg.exists()) {
					mixinSrg.createNewFile();
				}

				if (!mixinRefMap.exists()) {
					mixinRefMap.createNewFile();
				}
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}

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

			jarTask.from(mixinRefMap, action -> {
				String name = mixinRefMap.getName();
				String destination = mixinRefMap.getName().replace("$", "/");

				if (destination.contains("/")) {
					destination = destination.substring(0, destination.lastIndexOf('/'));
					action.into(destination).eachFile(copy -> {
						copy.setName(name.substring(name.lastIndexOf('$')+1, name.length()));
					});
				}
			});

			// Why do we need this one again?..
			this.extraShenanigans.extraReobfMap(mixinSrg);

			// Automatically replace any @MIXIN_REFMAP@ tokens in project resources
			ProcessResources processResources = (ProcessResources) this.project.getTasks().getByName("processResources");
			Map<String, Object> propertyMap = new LinkedHashMap<String, Object>();
			Map<String, String> tokenMap = new LinkedHashMap<String, String>();
			tokenMap.put("MIXIN_REFMAP", this.getMixinRefmapName());
			propertyMap.put("tokens", tokenMap);

			processResources.filter(propertyMap, ReplaceTokens.class);

			// Also in project sources
			this.extraShenanigans.addSourceReplacements();
		}
	}

	private void addRule(List<AccessRule> rules, Rule rule, String pattern) {
		rules.add(new AccessRule(rule.getName(), pattern));
	}

	public String grimoireVersion() {
		return this.project.hasProperty("grimoireVersion") ? this.project.getProperties().get("grimoireVersion").toString() : "+";
	}

	public boolean areGrimoireShenanigansEnabled() {
		return this.enabled;
	}

	public Rule getForbiddenRule() {
		if (this.forbiddenRule == null) {
			if (this.project.hasProperty("grimoireRestrictionRule")) {
				try {
					this.forbiddenRule = Rule.valueOf(String.valueOf(this.project.getProperties().get("grimoireRestrictionRule")));
				} catch (Exception ex) {
					this.forbiddenRule = Rule.FORBIDDEN;
				}
			} else {
				this.forbiddenRule = Rule.FORBIDDEN;
			}
		}

		return this.forbiddenRule;
	}

	public boolean isGrimoireItself() {
		if (this.project.hasProperty("isGrimoireItself"))
			return Boolean.parseBoolean(String.valueOf(this.project.getProperties().get("isGrimoireItself")));
		else
			return false;
	}

	public boolean disableGrimoireDependency() {
		if (this.project.hasProperty("disableGrimoireDependency"))
			return Boolean.parseBoolean(String.valueOf(this.project.getProperties().get("disableGrimoireDependency")));
		else
			return false;
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

	public static enum Rule {
		FORBIDDEN("nonaccessible"),
		ACESSIBLE("accessible"),
		DISCOURAGED("discouraged");

		private final String name;

		private Rule(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

}

