# 3DPrinting protocol


The system consists of two main parts: 

**Android app**: Stored in a _android_app_ folder as Android Studio project

**Desktop middleware**: Stored in _middleware_ folder

## Installation guide:

### Android app

Import the project into android studio, build and run ;) No other dependencies required

### Middleware
 **Dependencies**:
 
 - [mono project](http://www.mono-project.com) - required to run LPR Sender
 - [fswatch](https://github.com/emcrisostomo/fswatch) - due to the bug in ippserver, the sripts cannot be executed automatically, fswatch utility is used to monitor the data directory of ippserver and exetute scripts
 - [ippsample](http://istopwg.github.io/ippsample/) - ippserver utility from this bundle is used to connect to Android client
 
 **Installation**:
 
 - All the files with `*.sh` extention must have executable executable permissions (execute `chmod +x fileName` )
 - An ipserver binary has to be copied to _middleware_ folder
 
 **Configuration**:
 
 In a file `wrapper.sh` the information such as IP address of a local SafeQ server, the username or the name of a queue are stored and could be modified
 
 ## How to run
 
 the desktop middleware is started by executing the `startup.sh` file. Log data are storred in a log.txt file in an active directory.
 
 Android app is used through share menu of an Android device. An android device app **has to be** started before `startup.sh` otherwise the printers will not be discovered by the app.
