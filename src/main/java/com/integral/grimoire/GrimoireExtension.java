package com.integral.grimoire;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.google.common.base.Strings;

/**
 * I dunno, maybe we'll need this someday.
 *
 * @author Aizistral
 */

public class GrimoireExtension {
	protected transient Project project;

	public GrimoireExtension(GrimoireShenanigans plugin) {
		this.project = plugin.project;
	}
}

