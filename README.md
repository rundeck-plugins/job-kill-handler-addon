# Rundeck Kill Handler Add-On
This add-on allows to track the PIDs of processes executed on a remote node, and then issue a kill command for those PIDs when the job finish. The feature must be configured on each job where it's needed.

## Basic Usage

1. To install the add-on copy the `rundeck-job-kill-handler-<version>.jar` file into the `$RDBASE/server/addons` directory.

2. Setup the `Capture Process IDs` log filter on your job. This filter will register any PID written to the execution output log according to the configured pattern.

For example, using the default configuration, you can print this string to the output log and the log filter will capture the value `2345`:
```
RUNDECK:PID:2345
```

3. Enable the `Kill tracked processes after execution` execution plugin. The default behavior is to kill any processes on the corresponding nodes with PIDs captured by the log filter after the job finishes for any reason, you can also make it kill children processes and/or only killing processes on job failure (job finishes with `FAIL` status or is killed).


## Operating System Support

Currently this plugin has been successfully tested on the following target node operating systems:
- Ubuntu 18.04
- Centos 6.10
- Windows Server 2019

The plugin should work correctly with any Linux or Windows node which complies with the following:
- Target operating system supports posix process session ids (SID), specifically the `pkill -s` command (only for killing children processess).

Some BSD-like operating systems (like macOS) don't support process SID nor the `-s` flag for pkill, hence the "Kill Children" option will not work.


## Building and Installing

To build the plugin, run the gradle build command:
```
./gradlew clean build
```
The resulting artifact will be found at `build/libs/job-kill-handler-VERSION.jar`

For installing, copy the jar artifact to `$RDBASE/server/addons` directory. After restarting rundeck you should see the plugins available.











