# Grimoire Shenanigans
Gradle plugin that simplifies workspace setup for Grimoire implementers. Works in tandem with **ForgeGradle** (and **MixinGradle** if on 1.12.2).

## Workspace Setup:
For an example of how this plugin can be used in project, check out following templates.
- For 1.7.10: https://github.com/Aizistral-Studios/ForgeWorkspaceSetup/tree/1.7.10-grimmix
- For 1.12.2: https://github.com/Aizistral-Studios/ForgeWorkspaceSetup/tree/1.12.2-grimmix

## Features:
- Compatible with both 1.7.10 and 1.12.2 toolchain;
- On 1.7.10, fully takes care of specifying proper compiler arguments to ensure Mixin annotation processor will do its job and generate appropriate refmap when compiling;
- Automatically specifies [repository](https://github.com/juanmuscaria/maven) where Grimoire can be located, as well as proper artifacts to use at compile-time and runtime;
- Adds tasks for clearing build cache and resource cache before constructing a jar. Needed to ensure proper file inflation/token replacement when building;
- For Eclipse projects, adds an amount of access rules to `.classpath`, which help you to not accidentally stick your fingers into non-API parts of Grimoire;
- Integrates Mixin annotation processor tests for Eclipse IDE;
- Automatically replaces `@MIXIN_REFMAP@` when processing project resources and sources with actual refmap name as specified by `mixinRefmapName` property.

## Gradle Properties
This plugin uses an amount of properties which you can specify in gradle.properties file if you need to control them. Among these are:
- `grimoireVersion`, allows you define Grimoire version that should be attached as dependency to project. Can be either strict version or maven-like version range;
- `grimoireRestrictionRule`, which allows you to specify what type of access rules will be defined for non-API parts of Grimoire in Eclipse projects. Possible values are `nonaccessible` (default), `discouraged` and `accessible`;
- `disableGrimoireDependency`, allows to prevent plugin from automatically specifying Grimoire artifacts among project dependencies. Might be useful if you, for instance, use custom mappings and need to create your own deobfuscated artifact to serve as `dev` version;
- `mixinRefmapName`, specifies mixin refmap name that will be generated for this project and embedded in production jar. Can include subdirectories to avoid conflicting refmap names on the classpath in runtime, for instance `examplemod/examplemod.refmap.json` will put your refmap with name `examplemod.refmap.json` into `examplemod` folder within your jar when building;
- `disableMixinRefmap`, allows to disable adding tasks for refmap generation and embedding it into main .jar artifact upon building. Might be useful if you want your particular project to depend on Grimoire for extra features like omniconfig or EventHelper integration, but don't need Mixin itself;
- `isGrimoireItself`, should only be used by Grimoire project alone.
