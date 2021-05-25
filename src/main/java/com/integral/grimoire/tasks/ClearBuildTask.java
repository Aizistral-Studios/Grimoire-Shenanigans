package com.integral.grimoire.tasks;

import org.gradle.api.tasks.Delete;

public class ClearBuildTask extends Delete {

	public ClearBuildTask() {
		super();
		this.delete(this.getProject().file(this.getProject().getBuildDir().getName() + "/classes/main"));
	}

}