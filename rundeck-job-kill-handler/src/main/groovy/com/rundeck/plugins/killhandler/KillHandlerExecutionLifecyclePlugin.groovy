package com.rundeck.plugins.killhandler

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.NodeExecutionService
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import groovy.transform.CompileStatic
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Plugin(name = KillHandlerExecutionLifecyclePlugin.PROVIDER_NAME, service = ServiceNameConstants.ExecutionLifecycle)
@PluginDescription(title = KillHandlerExecutionLifecyclePlugin.PLUGIN_TITLE, description = KillHandlerExecutionLifecyclePlugin.PLUGIN_DESC)
@CompileStatic
class KillHandlerExecutionLifecyclePlugin implements ExecutionLifecyclePlugin {
    private final Logger log = LoggerFactory.getLogger(KillHandlerExecutionLifecyclePlugin.name)
    static final String PROVIDER_NAME = 'killhandler'
    static final String PLUGIN_TITLE = "Kill tracked processes after execution"
    static final String PLUGIN_DESC = '''Kill all processes collected by the 'Capture Process IDs' log filter\n\n
This operation will only affect nodes with 'unix' as osFamily, and will use the 'kill' and 'pkill' commands which must be available at the node.
'''

    private static final String OSFAMILY_UNIX = "unix"

    AuthorizedServicesProvider rundeckAuthorizedServicesProvider
    KillHandlerProcessTrackingService processTrackingService

    @PluginProperty(
            title = "Kill Children",
            description = "Also kill processes whose process SID matches the tracked PIDs"
    )
    boolean killChilds = true

    @PluginProperty(
            title = "Kill only on job failure",
            description = "Kill processes only if job failed or is killed"
    )
    boolean onFailOnly = false

    @Override
    ExecutionLifecycleStatus afterJobEnds(final JobExecutionEvent event) {

        if (!onFailOnly || !event.result.result.success || event.result.aborted) {

            def execId = event.execution.id
            def executionTrackData = processTrackingService.getExecutionTrackData(execId)
            if (executionTrackData) {

                // Delete stored execution data
                processTrackingService.flushExecution(execId)

                event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
                def authContext = event.executionContext.getUserAndRolesAuthContext()
                def nodeExecutionService = rundeckAuthorizedServicesProvider.getServicesWith(authContext).getService(NodeExecutionService)
                def execContext = event.executionContext

                event.nodes.each { node ->

                    def nodePidData = executionTrackData.get(node.nodename)
                    if (nodePidData && nodePidData.pids) {

                        // For now we only process unix nodes.
                        if (OSFAMILY_UNIX.equalsIgnoreCase(node.osFamily)) {

                            def commaPidList = nodePidData.pids.join(",")
                            event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node '${node.nodename}': ${commaPidList}")

                            // Issue PID kill
                            def cmdKill = "kill -9 " + nodePidData.pids.join(" ")
                            nodeExecutionService.executeCommand(execContext,
                                    ExecArgList.fromStrings(false, cmdKill),
                                    node)

                            // Kill children processes
                            if (killChilds) {
                                event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing processes by parent on node '${node.nodename}': ${commaPidList}")
                                // When the parent pid is killed, children processes change its ppid to 1 (init pid)
                                // To circumvent this, we issue a kill by SID also.
                                // This command will not work on macOS version of pkill :(
                                def cmdKillSid = "pkill -SIGKILL -s " + commaPidList
                                nodeExecutionService.executeCommand(execContext,
                                        ExecArgList.fromStrings(false, cmdKillSid),
                                        node)

//                                // Issue kill by parent id. This works only when the parent process is still alive.
//                                def cmdKillPpid = "pkill -SIGKILL -P " + commaPidList
//                                nodeExecutionService.executeCommand(execContext,
//                                        ExecArgList.fromStrings(false, cmdKillPpid),
//                                        node)

                            }
                        }
                    }
                }
            }
        }
        return null
    }
}

