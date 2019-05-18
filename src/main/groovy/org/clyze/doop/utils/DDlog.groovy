package org.clyze.doop.utils

import groovy.transform.TupleConstructor
import groovy.transform.TypeChecked
import groovy.util.logging.Log4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.apache.log4j.Logger
import org.clyze.doop.common.DoopErrorCodeException
import org.clyze.utils.Executor
import org.clyze.utils.Helper

/**
 * This class compiles and runs Souffle analysis with DDlog via the DDlog converter.
 */
@Log4j
@TupleConstructor
@TypeChecked
class DDlog {

	static final String convertedLogicName = "converted_logic" as String
    Executor executor
    File scriptFile
    File outDir

    /**
     * Reads the DDlog directory from its corresponding environment variable.
     *
     * @param log    the logger object to use for debug messages
     * @return       the DDlog path
     */
    private static String getDDlogDir(Logger log) {
		String DDLOG_DIR = "DDLOG_DIR"
		String ddlogDir = System.getenv(DDLOG_DIR)
		if (!ddlogDir) {
			throw new DoopErrorCodeException(24, new RuntimeException("Environment variable ${DDLOG_DIR} is empty."))
		} else {
			log.debug "Using DDlog in ${ddlogDir}"
            return ddlogDir
		}
    }

    /**
     * Copies the DDlog Souffle converter tools.
     *
     * @param log     the logger object to use for debug messages
     * @param outDir  the output directory where the files will be copied
     */
    public static void copyDDlogConverter(Logger log, File outDir) {
		String ddlogSouffleDir = "${getDDlogDir(log)}/tools"
		["souffle-grammar.pg", "souffle-converter.py"].each {
			File from = new File(ddlogSouffleDir, it)
			File to = new File(outDir, it)
			log.debug "COPY: ${from.canonicalPath} -> ${to.canonicalPath}"
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
		}
    }

    /**
     * Compile the (converted) analysis logic.
     *
	 * @param jobs		 the number of jobs to use when compiling the analysis
     */
    File compileWithDDlog(int jobs) {
        // Step 1. Call converter for logic only.
		def cmdConvert = ["${doopHome}/bin/run-in-dir.sh" as String,
						  outDir.canonicalPath,
						  "${outDir}/souffle-converter.py" as String,
                          "--logic-only",
						  scriptFile.canonicalPath,
						  convertedLogicPrefix ]
		log.debug "Running logic conversion command: ${cmdConvert}"
		executeCmd(cmdConvert)

		// Step 2: Compile the analysis.
		def genTime = Helper.timing {
			log.info "Compiling the analysis: code generation..."
			String convertedLogic = "${convertedLogicPrefix}.dl" as String
			def cmdGenRust = "${doopHome}/bin/run-in-dir.sh ${getDDlogDir(log)} stack run -- -i ${convertedLogic} --action=compile -L lib".split().toList()
			executeCmd(cmdGenRust)
		}
		log.info "Time: ${genTime}"
		def buildTime = Helper.timing {
			log.info "Compiling the analysis: building (using ${jobs} jobs)..."
			log.debug "Build dir: ${buildDir}"
			def cmdBuildRust = "${doopHome}/bin/run-in-dir.sh ${buildDir} cargo build -j ${jobs} --release".split().toList()
			executeCmd(cmdBuildRust)
		}
		log.info "Time: ${buildTime}"

        return new File(analysisBinary)
    }

    private String getBuildDir() {
        return "${convertedLogicPrefix}_ddlog" as String
	}

    /**
     * Read Doop home from environment variable.
     */
    private String getDoopHome() {
        String doopHome = System.getenv("DOOP_HOME")
		if (!doopHome) {
			throw new DoopErrorCodeException(24, new RuntimeException("Environment variable DOOP_HOME is empty."))
		} else {
			log.debug "Using Doop home: ${doopHome}"
            return doopHome
		}
    }

    private String getConvertedLogicPrefix() {
		return "${outDir}/${convertedLogicName}" as String
    }

    private String getAnalysisBinary() {
        return "${buildDir}/target/release/${convertedLogicName}_cli" as String
    }

	/**
	 * Execute the 'run' phase using the DDLog Souffle converter.
	 *
	 * @param db		 the database directory
	 * @param jobs		 the number of jobs to use when running the analysis
	 */
	private void runWithDDlog(File db, int jobs) {
        // Step 1. Convert the facts.
		def cmdConvert = ["${doopHome}/bin/run-in-dir.sh" as String,
						  outDir.canonicalPath,
						  "${outDir}/souffle-converter.py" as String,
						  scriptFile.canonicalPath,
                          "--facts-only",
						  convertedLogicPrefix ]
		log.debug "Running facts conversion command: ${cmdConvert}"
		executeCmd(cmdConvert)

		// Step 2: Run the analysis.
		log.info "Running the analysis (using ${jobs} jobs)..."
		def runTime = Helper.timing {
			def dump = "${db.canonicalPath}/dump"
			def dat = "${convertedLogicPrefix}.dat"

			// Hack: use script to get away with redirection.
			def cmdRun = "${doopHome}/bin/run-with-redirection.sh ${dat} ${dump} ${analysisBinary} -w ${jobs}".split().toList()
			executeCmd(cmdRun)
		}
		log.info "Time: ${runTime}"
	}

	/**
	 * Executes a command using a temporary file for its output.
	 *
	 * @param cmd	the command line to invoke
	 */
	void executeCmd(List<String> command) {
		log.debug command
		Path tmpFile = Files.createTempFile("", "")
		executor.executeWithRedirectedOutput(command, tmpFile.toFile()) { println it }
		Files.delete(tmpFile)
	}

}