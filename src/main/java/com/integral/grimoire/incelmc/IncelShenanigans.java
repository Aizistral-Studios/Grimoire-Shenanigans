package com.integral.grimoire.incelmc;

import java.io.File;
import java.lang.reflect.Method;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.GroovySourceSet;
import org.gradle.api.tasks.ScalaSourceSet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.scala.ScalaCompile;

import com.integral.grimoire.ExtraShenanigans;
import com.integral.grimoire.GrimoireShenanigans;

import net.minecraftforge.gradle.userdev.UserDevExtension;
import net.minecraftforge.gradle.userdev.UserDevPlugin;

public class IncelShenanigans extends ExtraShenanigans {

	public IncelShenanigans(GrimoireShenanigans plugin) {
		super(plugin);
	}

	@Override
	public Task copySrgsTask() {
		File srgDir = new File(this.project.getBuildDir(), "createMcpToSrg");
		File srg = new File(srgDir, "output.tsrg");

		if (!srg.exists()) {
			try {
				srgDir.mkdirs();
				srg.createNewFile();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
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

	@Override
	public String getAnnotationProccessor() {
		return "org.spongepowered:mixin:0.8.3-SNAPSHOT:processor";
	}

	@Override
	public boolean isChadMC() {
		return false;
	}

	@Override
	public void addSourceReplacements() {
		this.createSourceCopyTasks().replace("@MIXIN_REFMAP@", this.plugin.getMixinRefmapName());
	}

	private final SourceCopyTask createSourceCopyTasks() {
		JavaPluginConvention javaConv = (JavaPluginConvention) this.project.getConvention().getPlugins().get("java");
		SourceSet main = javaConv.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

		// do the special source moving...

		File dir = new File(this.project.getBuildDir(), "sources/java");

		SourceCopyTask task = this.plugin.makeTask("sourceCopyTask", SourceCopyTask.class);
		task.setSource(main.getJava());
		task.setOutput(dir);

		JavaCompile compile = (JavaCompile) this.project.getTasks().getByName(main.getCompileJavaTaskName());
		compile.dependsOn("sourceMainJava");
		compile.setSource(dir);

		return task;
	}

}

