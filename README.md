# emr-nlp-server 

emr-nlp-server provides the backend service for the [emr-vis-web](https://github.com/trivedigaurav/emr-vis-web) project.

## Getting Started

To get started, install the pre-requisites, get the emr-nlp-server application and then launch the service as described below:

### Prerequisites

1. You must have Java Development Kit (JDK) 1.7 to build or Java Runtime (JRE) 1.7 to run this project. To confirm that you have the right version of JRE installed, run `$ java -version` and verify that the output is similar to:

    ```
    java version "1.7.0_51"
    Java(TM) SE Runtime Environment (build 1.7.0_51-b13)
    Java HotSpot(TM) 64-Bit Server VM (build 24.51-b03, mixed mode)
    ```
    
    If you don't have the JDK installed or have one of the previous version you may get the latest version from the [Oracle Technology Network](http://www.oracle.com/technetwork/java/index.html).

2. We use the [Apache Tomcat](http://tomcat.apache.org/) server to deploy the app. On a Mac with [homebrew][homebrew] you may use `$ brew install tomcat` to install the server on your machine.

3. To build the project, we recommend using the [Eclipse IDE for Java EE Developers](http://www.eclipse.org/downloads/) with the [EGit plugin](http://www.eclipse.org/egit/download/) installed. This step is required only if you plan to build from the source.

### Get the emr-nlp-server application

#### _Option A:_ Use the pre-built .war file to run the project

1. Download [emr-nlp-server.war](https://github.com/trivedigaurav/emr-nlp-server/blob/master/emr-nlp-server.war).

2. Navigate to the home directory of your tomcat server. You can use `$ catalina version` to check the value of  `CATALINA_HOME`.

3. `cd` to the _webapps/_ directory. If you are using the default tomcat setup, your present working directory would be something like _/usr/local/Cellar/tomcat/7.0.54/libexec/webapps/_.

4. Move _emr-nlp-server.war_ to this _webapps/_ directory. Skip the next section and continue reading.


#### _Option B:_ Build the project in Eclipse

1. Clone the emr-nlp-server repository using EGit: **File** > **Import** > Git ...

2. Export the project into a .war file: **File** > **Export** > Web > WAR File to the _webapps/_ directory as specified in steps 2-3 in the section above.

### Run the server

1. Navigate to the folder containing the _data_ directory (not in the repository). This folder may reside anywhere on your file-system and doesn't have to be present in one of the project folders.

2. Start the Tomcat server using `$ catalina run` or `$ catalina start`. Note that the _data_ directory must reside in the same directory you start the server from. So, if you are using Eclipse to launch the server on build it must be present where the Eclipse executable is running from.
3. Follow the README file inside the _libsvm_ / _data_ directory to compile LibSVM before running emr-nlp-server.

Now follow the steps on [emr-vis-web](https://github.com/trivedigaurav/emr-vis-web) to setup the front-end application.

[homebrew]: http://brew.sh/
