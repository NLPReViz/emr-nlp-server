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
    
    If you don't have the JDK installed or have an older one, you may get the latest version from the [Oracle Technology Network](http://www.oracle.com/technetwork/java/index.html).

2. We use the [Apache Tomcat](http://tomcat.apache.org/) server to deploy the app. On a Mac with [homebrew][homebrew] you may use `$ brew install tomcat` to install the server on your machine.

3. To build the project, we recommend using the [Eclipse IDE for Java EE Developers](http://www.eclipse.org/downloads/) with the [EGit plugin](http://www.eclipse.org/egit/download/) installed (Option B). Otherwise, the project also contains a _build.xml_ to be used with [Apache Ant][ant] (Option A).


#### _Option A:_ Use ant to build the project

1. Clone the emr-nlp-server repository using [git][git]:

    ```
    git clone https://github.com/trivedigaurav/emr-nlp-server.git
    cd emr-nlp-server
    ```

2. Specify the path to the _webapps_ directory in `CATALINA_HOME` environment variable and use `ant deploy` to to build and deploy the backend app. 

    For example if your Tomcat's _webapps_ directory accessible as _/usr/local/Cellar/tomcat/7.0.54/libexec/webapps/_, then you may use:

    ```
    env CATALINA_HOME=/usr/local/Cellar/tomcat/8.0.9/libexec/ ant deploy
    ```


#### _Option B:_ Build the project in Eclipse

1. Clone the emr-nlp-server repository using EGit: **File** > **Import** > Git ... 

3. Export the project into a .war file: **File** > **Export** > Web > WAR File to the Tomcat's _webapps/_ directory.

### Run the server

1. Make sure you have the _data_ directory (not in the repository; contact devs for information) inside *$CATALINA_BASE*. You should be able to figure this path from the print messages you see after launching the server. Example path: _/usr/local/Cellar/tomcat/8.0.9/libexec/_.

2. You need to build libsvm before you may run the server for the first time. To do that run `make` inside _data/libsvm_ directory or follow the instructions in the README file present there.

3. Start the Tomcat server (eg. using `$ catalina run` or `# service tomcat start` etc.).

### Cleaning model files
Receiving and handling feedbacks from the front-end ([emr-vis-web](https://github.com/trivedigaurav/emr-vis-web)) will modify the feedback management and model files contained in the *CATALINA_base/data* directory. If you wish to reset these management files, e.g. to restart a new experiment, follow the following steps:

1. Copy [ResetDB.jar](ResetDB.jar) file into your _data_ directory. 
2. Run the command `java -jar ResetDB.jar 1` from the _data_ directory.

--

Now follow the steps on [emr-vis-web](https://github.com/trivedigaurav/emr-vis-web) to setup the front-end application.

### Login
The default login credentials are _"username"_ and _"password"_. You are encouraged to change them from https://github.com/trivedigaurav/emr-nlp-server/blob/master/src/frontEnd/serverSide/UserAuthentication.java when running the app on a publicly accessible server.

[homebrew]: http://brew.sh/
[git]: http://git-scm.com/
[ant]: http://ant.apache.org/
