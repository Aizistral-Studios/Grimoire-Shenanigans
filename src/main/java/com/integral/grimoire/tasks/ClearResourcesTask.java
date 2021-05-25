package com.integral.grimoire.tasks;

import org.gradle.api.tasks.Delete;

public class ClearResourcesTask extends Delete {

	public ClearResourcesTask() {
		super();
		this.delete(this.getProject().file(this.getProject().getBuildDir().getName() + "/resources/main"));
	}

}