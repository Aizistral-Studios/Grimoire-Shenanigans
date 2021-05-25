package com.integral.grimoire.fg23;

import java.io.File;
import java.lang.reflect.Method;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskAction;

import com.integral.grimoire.ExtraShenanigans;
import com.integral.grimoire.GrimoireShenanigans;

import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.tasks.user.reobf.ReobfTask;
import net.minecraftforge.gradle.user.TaskSingleReobf;
import net.minecraftforge.gradle.user.patch.UserPatchBasePlugin;
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin;

public class ExtraShenanigans_23 extends ExtraShenanigans {

	public ExtraShenanigans_23(GrimoireShenanigans plugin) {
		super(plugin);
	}

	@Override
	public Task copySrgsTask() {
		//CopySrgsTask copyTask = this.plugin.makeTask("copySrgs", CopySrgsTask.class);
		//copyTask.init((ForgePlugin) this.project.getPlugins().getPlugin("forge"));
		//return copyTask;
		return null;
	}

	@Override
	public void extraReobfMap(File mixinSrg) {
		//TaskSingleReobf reobf = (TaskSingleReobf) this.project.getTasks().getByName("reobf");
		//reobf.addSecondarySrgFile(mixinSrg);
	}

	// Custom Tasks

	public static class CopySrgsTask extends Copy {
		public CopySrgsTask() {
			super();
		}

		public void init(ForgePlugin forgePlugin) {
			this.from(forgePlugin.delayedFile(Constants.DIR_MCP_MAPPINGS + "/srgs/"));

			this.include("**/*.srg");
			this.into(this.getProject().getBuildDir().getName() + "/srgs");

		}

		@TaskAction
		public void doTask() {
			// NO-OP
		}
	}

}

