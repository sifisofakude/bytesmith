plugins	{
	id("org.jetbrains.kotlin.android")
	id("com.android.library")
}

android	{
  namespace = "io.github.sifisofakude.core.bytesmith"
	compileSdk = 36
	
	defaultConfig {
	  minSdk = 23
	}
}

dependencies  {
  implementation("io.github.sifisofakude:filesystem-android:0.4.0")
  implementation(project(":common"))
}

