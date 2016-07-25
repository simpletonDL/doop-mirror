package org.clyze.doop

import org.clyze.doop.core.AnalysisPostProcessor
import org.apache.commons.io.FilenameUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.clyze.doop.core.Analysis
import org.clyze.doop.core.Doop

class CommandLineAnalysisPostProcessor implements AnalysisPostProcessor {

    protected Log logger = LogFactory.getLog(getClass())

    @Override
    void process(Analysis analysis) {
        printStats(analysis)
        linkResult(analysis)
    }


    protected void printStats(Analysis analysis) {
        if (analysis.options.X_ONLY_FACTS.value)
            return

        // We have to store the query results to a list since the
        // closure argument of the connector does not generate an
        // iterable stream.

        def lines = [] as List<String>
        analysis.connector.processPredicate("Stats:Runtime") { String line ->
            lines.add(line)
        }

        logger.info "-- Runtime metrics --"
        lines.sort()*.split(", ").each {
            printf("%-80s %,.2f\n", it[0], it[1] as float)
        }

        if (!analysis.options.X_STATS_NONE.value) {
            lines = [] as List<String>
            analysis.connector.processPredicate("Stats:Metrics") { String line ->
                lines.add(line)
            }

            // We have to first sort (numerically) by the 1st column and
            // then erase it

            logger.info "-- Statistics --"
            lines.sort()*.replaceFirst(/^[0-9]+[ab]?@ /, "")*.split(", ").each {
                printf("%-80s %,d\n", it[0], it[1] as int)
            }
        }
    }

    protected void linkResult(Analysis analysis) {
        if (analysis.options.X_ONLY_FACTS.value) {
            def facts = new File(analysis.options.X_ONLY_FACTS.value)
            logger.info "Making facts available at $facts"
            analysis.executor.execute("ln -s -f ${analysis.facts} $facts")
            return
        }

        def jre = analysis.options.JRE.value
        if (jre != "system") jre = "jre${jre}"
        def jarName = FilenameUtils.getBaseName(analysis.inputJarFiles[0].toString())

        def humanDatabase = new File("${Doop.doopHome}/results/${jarName}/${analysis.name}/${jre}/${analysis.id}")
        humanDatabase.mkdirs()
        logger.info "Making database available at $humanDatabase"
        analysis.executor.execute("ln -s -f ${analysis.database} $humanDatabase")

        def lastAnalysis = "${Doop.doopHome}/last-analysis"
        logger.info "Making database available at $lastAnalysis"
        analysis.executor.execute("ln -s -f -n ${analysis.database} $lastAnalysis")
    }

}
