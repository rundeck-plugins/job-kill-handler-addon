# Rundeck Kill Handler Add-On

## Basic Usage

This add-on allows to track the PIDs of processes executed on a remote node, and the issue a kill command for those PIDs when the job finish.

To enable, install the add-on and setup the `Capture Process IDs` log filter on your job. This filter will register any PID written to the execution output log according to the configured pattern.

For example, using the default configuration, you can print this string to the output log to register PID 2345 on the current node:
```
RUNDECK:PID:2345
```

Then to enable the killing of these processes at execution finish, enable the `Kill tracked processes after execution` execution plugin. This plugin will connect to each node after execution finish and issue a kill command for each of the tracked PIDs of that node. You can also configure the plugin to attempt to kill children processes of the registered PIDs.


## Operating System Support

Currently this plugin has been successfully tested on the following target node operating systems:
- Ubuntu 18.04
- Centos 6.10

The plugin should work correctly with any Linux node which complies with the following:
- Node definition indicates `unix` osFamily
- Target operating system supports posix process session ids (SID), specifically the `pkill -s` command.

Some BSD-like operating systems (like macOS) don't support process SID nor the `-s` flag for pkill, hence the "Kill Children" option will not work.
Windows operating systems are not currently supported by this plugin.


## Building and Installing

To build the plugin, run the gradle build command:
```
./gradlew clean build
```
The resulting artifact will be found at `build/libs/job-kill-handler-VERSION.jar`

For installing, copy the jar artifact to $RDBASE/server/addons directory. After restarting rundeck you should see the plugins available.














