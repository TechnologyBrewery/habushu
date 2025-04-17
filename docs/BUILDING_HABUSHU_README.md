[[Return to Main Documentation]](../README.md#building-habushu)

# Building Habushu

You can use `mvnd` or `mvn` to build Habushu. We recommend `mvnd`, which provides a substantial performance boost when running the examples.

If you are working on Habushu, please be aware of some nuances in working with a plugin that defines a custom Maven build lifecycle and packaging. If the `habushu-maven-plugin` has not been previously built or there are unbuilt changes to the `habusu` lifecycle, developers must manually build the
`habushu-maven-plugin` and then execute **another, separate** build of the [examples](../examples) to use the updated `habushu-maven-plugin` and `habushu` lifecycle.  Developers are **not** able to build updates to the `habushu` lifecycle and test their application in the [examples](../examples) within the same build.  
That said, if developers do not update the `habushu` lifecycle and simply make updates to existing `Mojo`s defined in the `habushu-maven-plugin`, a single build may be used to build `habushu-maven-plugin` and apply the updates to the [examples](../examples). To assist, there are two profiles available in the build:

* `mvnd clean install -Pbootstrap`: Builds the `habushu-maven-plugin` such that the custom `habushu` lifecycle may be utilized within subsequent builds.
* **NOTE:** If updates are made to the `habushu` lifecycle (i.e. updates to the `habushu` lifecycle mapping configuration made in `habushu-maven-plugin/src/main/resources/META-INF/plexus/components.xml`), developers are **REQUIRED** to use two builds to test - one to build the lifecycle, then a second to use that updated lifecycle.  Code changes to `Mojo` classes within the existing `habushu` lifecycle work via normal builds without the need for a second pass.
* `mvnd clean install -Pdefault`: (ACTIVE BY DEFAULT - `-Pdefault` does not need to be specified) builds all modules.  Developers may use this profile to build and apply changes to existing `habushu-maven-plugin` `Mojo` classes.

## Common Issues

### Plugin Does Not Yet Exist

If you encounter the following error, please see the section above for details on how to use the `bootstrap` profile to appropriately build the `habushu-maven-plugin` and its associated custom Maven lifecycle. This error will typically only occur when attempting to use an in-flight `SNAPSHOT` version of Habushu that is not yet published to the Maven Central repository.

```
[WARNING] The POM for org.technologybrewery.habushu:habushu-maven-plugin:jar:0.0.1-SNAPSHOT is missing, no dependency information available
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[ERROR] Unresolveable build extension: Plugin org.technologybrewery.habushu:habushu-maven-plugin:0.0.1-SNAPSHOT or one of its dependencies could not be resolved: Could not find artifact org.technologybrewery.habushu:habushu-maven-plugin:jar:0.0.1-SNAPSHOT @ 
[ERROR] Unknown packaging: habushu @ line 15, column 13
```