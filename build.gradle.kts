import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

import groovy.lang.GroovyObject

plugins {
	kotlin("jvm") version "1.3.61"
	id("java")
	id("eclipse")
	id("idea")
	id("fabric-loom") version "0.2.6-SNAPSHOT"
	id("maven-publish")
	id("com.jfrog.artifactory") version "4.9.0"
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

if(rootProject.file("private.gradle").exists()) { //Publishing details
	apply(from = "private.gradle")
}

base {
	archivesBaseName = project.findProperty("archives_base_name") as String
}
group = project.findProperty("maven_group") as String
version = project.findProperty("mod_version") as String + "+" + project.findProperty("minecraft_version") as String

minecraft {
}

repositories {
	mavenCentral()
	maven(url = "http://maven.fabricmc.net/") // Fabric maven - home of Fabric API and ModMenu
	maven(url = "http://server.bbkr.space:8081/artifactory/libs-release") // Cotton maven - home of Cotton projects
	maven(url = "https://maven.abusedmaster.xyz") // NerdHub maven - home of Cardinal Components
	mavenCentral()
	jcenter()
}

fun DependencyHandlerScope.includeAndExpose(dep: String) {
	modImplementation(dep)
	include(dep)
}

fun DependencyHandlerScope.includeKt(dep: String) {
	val fullName = "org.jetbrains.kotlin:$dep:${project.findProperty("kotlin_version") as String}"
	modImplementation(fullName)
	include(fullName)
}

dependencies {
	minecraft("com.mojang:minecraft:" + project.findProperty("minecraft_version") as String)
	mappings("net.fabricmc:yarn:" + project.findProperty("minecraft_version") as String + "+build." + project.findProperty("yarn_build") as String + ":v2")
	modImplementation("net.fabricmc:fabric-loader:" + project.findProperty("loader_version") as String)
	modImplementation("net.fabricmc.fabric-api:fabric-api:" + project.findProperty("fabric_version") as String)
	compileOnly("com.google.code.findbugs:jsr305:3.0.2")

	modImplementation("net.fabricmc:fabric-language-kotlin:" + project.findProperty("kotlin_version") as String + "+build." + project.findProperty("fabric_kotlin_build") as String)

	includeAndExpose("org.jetbrains.intellij.deps:trove4j:1.0.20181211")

	includeKt("kotlin-script-runtime")
	includeKt("kotlin-scripting-common")
	includeKt("kotlin-scripting-jvm")
	includeKt("kotlin-scripting-jvm-host-embeddable")
	includeKt("kotlin-daemon-embeddable")
	includeKt("kotlin-compiler-embeddable")
	includeKt("kotlin-scripting-compiler-impl-embeddable")
	includeKt("kotlin-scripting-compiler-embeddable")
	includeKt("kotlin-scripting-jsr223-embeddable")
}

tasks.getByName<ProcessResources>("processResources") {
	filesMatching("fabric.mod.json") {
		expand(
				mutableMapOf(
						"version" to version
				)
		)
	}
}

val remapJar = tasks.getByName("remapJar")
val remapSourcesJar = tasks.getByName("remapSourcesJar")

// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
// if it is present.
// If you remove this task, sources will not be generated.
val sourcesJar = tasks.create<Jar>("sourcesJar") {
	classifier = "sources"
	from(sourceSets["main"].allSource)
}

// configure the maven publication
publishing {
	publications {
		create("main", MavenPublication::class.java) {
			// add all the jars that should be included when publishing to maven
			//artifact(jar) {
			//	builtBy remapJar
			//}

			artifact (buildDir.absolutePath as String + "/libs/" + project.findProperty("archives_base_name") as String + "-" + project.findProperty("version") as String + ".jar") { //release jar - file location not provided anywhere in loom
				classifier = null
				builtBy (remapJar)
			}

			artifact (buildDir.absolutePath as String + "/libs/" + project.findProperty("archives_base_name") + "-" + project.findProperty("version") as String + "-dev.jar") { //release jar - file location not provided anywhere in loom
				classifier = "dev"
				builtBy (remapJar)
			}

			artifact(sourcesJar) {
				builtBy (remapSourcesJar)
			}
		}
	}

	// select the repositories you want to publish to
	repositories {
		// uncomment to publish to the local maven
		// mavenLocal()
	}
}

artifactory {
	if (project.hasProperty("artifactoryUsername")) {
		setContextUrl("http://server.bbkr.space:8081/artifactory/")
		publish(delegateClosureOf<PublisherConfig> {
			repository(delegateClosureOf<GroovyObject> {
				if ((version as String).contains("SNAPSHOT")) {
					setProperty("repoKey", "libs-snapshot")
				} else {
					setProperty("repoKey", "libs-release")
				}

				setProperty("username", project.findProperty("artifactoryUsername"))
				setProperty("password", project.findProperty("artifactoryPassword"))
			})
			defaults(delegateClosureOf<GroovyObject> {
				invokeMethod("publications", "mavenJava")
				setProperty("publishPom", true)
				setProperty("publishArtifacts", true)
			})
		})
	} else {
		println("Cannot configure artifactory; please define ext.artifactoryUsername and ext.artifactoryPassword before running artifactoryPublish")
	}
}
