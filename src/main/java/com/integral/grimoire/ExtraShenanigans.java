package com.integral.grimoire;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.Task;

public abstract class ExtraShenanigans {
	protected final Project project;
	protected final GrimoireShenanigans plugin;

	public ExtraShenanigans(GrimoireShenanigans plugin) {
		this.plugin = plugin;
		this.project = plugin.project;
	}

	public abstract Task copySrgsTask();

	public abstract String getAnnotationProccessor();

	public abstract boolean isChadMC();

	public abstract void extraReobfMap(File mixinSrg);

	public abstract void addSourceReplacements();

	public abstract void apIntegration();

}
