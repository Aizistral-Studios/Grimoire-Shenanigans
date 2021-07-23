package com.integral.grimoire.chadmc;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
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

import com.integral.grimoire.ExtraShenanigans;
import com.integral.grimoire.GrimoireShenanigans;

import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedBase.IDelayedResolver;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.tasks.user.reobf.ReobfTask;
import net.minecraftforge.gradle.user.patch.ForgeUserPlugin;
import net.minecraftforge.gradle.user.patch.UserPatchBasePlugin;

public class ChadShenanigans extends ExtraShenanigans {

	public ChadShenanigans(GrimoireShenanigans plugin) {
		super(plugin);
	}

	@Override
	public Task copySrgsTask() {
		CopySrgsTask copyTask = this.plugin.makeTask("copySrgs", CopySrgsTask.class);
		copyTask.init((ForgeUserPlugin) this.project.getPlugins().getPlugin("forge"));
		return copyTask;
	}

	@Override
	public void extraReobfMap(File mixinSrg) {
		ReobfTask reobf = (ReobfTask) this.project.getTasks().getByName("reobf");
		reobf.addExtraSrgFile(mixinSrg);
	}

	@Override
	public void apIntegration() {
		// NO-OP
	}

	// Custom Tasks

	public static class CopySrgsTask extends Copy {
		public CopySrgsTask() {
			super();
		}

		public void init(ForgeUserPlugin forgePlugin) {
			this.from(new DelayedFile(this.getProject(), "{SRG_DIR}", (IDelayedResolver<?>)forgePlugin));

			this.include("**/*.srg");
			this.into(this.getProject().getBuildDir().getName() + "/srgs");
		}

		@TaskAction
		public void doTask() {
			// NO-OP
		}
	}

	@Override
	public String getAnnotationProccessor() {
		return "org.spongepowered:mixin:0.8.3-SNAPSHOT:processor";
	}

	@Override
	public boolean isChadMC() {
		return true;
	}

	@Override
	public void addSourceReplacements() {
		ForgeUserPlugin forge = (ForgeUserPlugin) this.project.getPlugins().getPlugin("forge");
		forge.getExtension().replace("@MIXIN_REFMAP@", this.plugin.getMixinRefmapName());
	}

}
