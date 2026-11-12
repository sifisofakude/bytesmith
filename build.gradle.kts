plugins {
	kotlin("jvm") version "2.4.10"
	`maven-publish`
	`java-library`
	signing
}

version = "1.1.0-beta01"
group = "io.github.sifisofakude.bytesmith"

kotlin 	{
	jvmToolchain(21)
}

dependencies	{
	implementation(libs.filesystem)
	implementation(libs.archive.util)
	implementation(libs.ecj)
	implementation(libs.kotlin.compiler)

	testImplementation(kotlin("test"))
}

java 	{
	withSourcesJar()
	withJavadocJar()
}

publishing 	{
	publications	{
		create<MavenPublication>("mavenJava")	{
			from(components["java"])

			artifactId = "bytesmith"

			pom	{
				name.set("bytesmith")
				description.set(
					"Lightweight Kotlin and Java compiler toolkit for JVM and Android with support for "+
	        "mixed-language projects, compiler plugins and custom classpath."
				)
				
	      url.set("https://github.com/sifisofakude/bytesmith")

	      licenses {
		      license {
		        name.set("MIT License")
		        url.set("https://opensource.org/licenses/MIT")
		        distribution.set("repo")
		      }
	      }

	      developers {
		      developer {
		        id.set("sifisofakude")
		        name.set("Sifiso Fakude")
		      }
	      }

	      scm {
		      connection.set(
	        	"scm:git:git://github.com/sifisofakude/bytesmith.git"
		      )
		      developerConnection.set(
	        	"scm:git:ssh://github.com/sifisofakude/bytesmith.git"
		      )
		      url.set("https://github.com/sifisofakude/bytesmith")
	      }
			}
		}
	}
	
	// repositories {
	//   maven {
	//     name = "CentralStaging"
	//     url = uri(layout.buildDirectory.dir("central-staging"))
	//   }
	// }
}

signing {
	useInMemoryPgpKeys(
	  providers.environmentVariable("GPG_SIGNING_KEY").orNull,
	  providers.environmentVariable("GPG_PASSPHRASE").orNull
	)

	sign(publishing.publications)
}
