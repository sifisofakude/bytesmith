package io.github.sifisofakude.core.bytesmith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

import io.github.sifisofakude.filesystem.*

class BytesmithTest	{
	@Test
	fun compile()	{
		val testRoot = "../libs"
		val files = listOf(
			"Hey.java","HeyMom.kt","HeyMom2.java"
		).map { testRoot + "/$it" }

		val outputDir = "../libs/HeyB/out.jar"

		assertTrue(FileOperation(testRoot).exists())

		files.forEach	{
			assertTrue(FileOperation(it).exists())
		}

		val args = listOf("-d",outputDir,"-cp","$testRoot/libs/kotlin-stdlib.jar",*files.toTypedArray())

		val result = Main().compile(args)
		assertTrue(result.success)
	}
}
