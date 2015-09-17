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

3. Our project depends on the following external dependencies which can be downloaded using [Apache Ant][ant]:
    - [Java Jersey](http://jersey.java.net/) which is [dual licensed](https://jersey.java.net/license.html):
    COMMON DEVELOPMENT AND DISTRIBUTION LICENSE and GPL 2.
    - [Weka](http://www.cs.waikato.ac.nz/ml/weka/) licensed under GPL 3.
    - [Libsvm](http://www.csie.ntu.edu.tw/~cjlin/libsvm/) with a license compatible with GPL.
    - [Stanford CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml) licensed under the GNU General Public License (v3 or later; Stanford NLP code is GPL v2+, but the composite with external libraries is v3+).

    To download and resolve these dependencies from their respective repositories use:
    
    ```
    ant resolve
    ```

### Building the project
    
To build the project, we recommend using the [Eclipse IDE for Java EE Developers](http://www.eclipse.org/downloads/) with the [EGit plugin](http://www.eclipse.org/egit/download/) installed (Option B). Otherwise, the project also contains a _build.xml_ to be used with [Apache Ant][ant] (Option A).

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

### Running the server

1. Make sure you have the _data_ directory (not in the repository; contact devs for information) inside *$CATALINA_BASE*. You should be able to figure this path from the print messages you see after launching the server. Example path: _/usr/local/Cellar/tomcat/8.0.9/libexec/_.

2. You need to build libsvm before you may run the server for the first time. To do that run `make` inside _data/libsvm_ directory or follow the instructions in the README file present there.

3. Start the Tomcat server (eg. using `$ catalina run` or `# service tomcat start` etc.).

Now follow the steps on [emr-vis-web](https://github.com/trivedigaurav/emr-vis-web) to setup the front-end application.

### Login
The the rest calls to the server are protected with a [basic access http authentication](https://en.wikipedia.org/wiki/Basic_access_authentication). The default login credentials are _"username"_ and _"password"_. You are encouraged to change them in [UserAuthentication.java](src/frontEnd/serverSide/UserAuthentication.java) when running the app on a publicly accessible server.

[homebrew]: http://brew.sh/
[git]: http://git-scm.com/
[ant]: http://ant.apache.org/

## License 
This project is released under the GPL 3 license. Take a look at the [LICENSE](LICENSE.md) file in the source for more information.
