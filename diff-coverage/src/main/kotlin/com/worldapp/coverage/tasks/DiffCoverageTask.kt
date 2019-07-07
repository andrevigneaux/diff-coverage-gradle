package com.worldapp.coverage.tasks

import com.worldapp.coverage.configuration.ChangesetCoverageConfiguration
import com.worldapp.coverage.configuration.toReport
import com.worldapp.coverage.report.ReportGenerator
import com.worldapp.diff.CodeUpdateInfo
import com.worldapp.diff.ModifiedLinesDiffParser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.slf4j.LoggerFactory
import java.io.File

open class DiffCoverageTask : DefaultTask() {

    internal lateinit var diffCoverageReport: ChangesetCoverageConfiguration

    init {
        group = "verification"
        description = "Builds coverage report only for modified code"
        dependsOn += "test"
    }

    @TaskAction
    fun executeAction() {
        val updatesInfo = CodeUpdateInfo(obtainUpdatesInfo(diffCoverageReport.diffFile))
        val report = diffCoverageReport.toReport(getJacocoExtension())

        jacocoReport().let {
            ReportGenerator(
                    project.projectDir,
                    it.executionData.files,
                    it.allClassDirs.files,
                    it.allSourceDirs.files,
                    updatesInfo
            )
        }.create(report)
    }

    private fun Project.getJacocoExtension(): JacocoPluginExtension {
        return extensions.getByType(JacocoPluginExtension::class.java)
    }

    private fun jacocoReport(): JacocoReport {
        return project.tasks.findByName("jacocoTestReport")
                as? JacocoReport
                ?: throw IllegalStateException("jacocoTestReport task wasn't found")
    }

    private fun getJacocoExtension(): JacocoPluginExtension {
        return project.extensions.getByType(JacocoPluginExtension::class.java)
    }

    private fun obtainUpdatesInfo(diffFilePath: String?): Map<String, Set<Int>> {
        val diffFile = diffFilePath
                ?.let(::File)
                ?: throw RuntimeException("Diff file path not specified: $diffFilePath")

        diffFile.takeIf(File::exists)
                ?.takeIf(File::isFile)
                ?: throw RuntimeException("No such file: $diffFile")

        log.debug("Starting to retrieve modified lines from $diffFile")
        return ModifiedLinesDiffParser().collectModifiedLines(diffFile)
    }

    companion object {
        val log = LoggerFactory.getLogger(DiffCoverageTask::class.java)
    }
}