# jBilling

## Requirements:

* Java 8+
* Grails 2.4.3
* PostgreSQL 13 *(required for testing, other runtime databases are supported)*

To run jBilling from source, you will need to have Java 8+ and Grails 2.4.3 installed.  
To install `grails` download version 2.4.3 from the [Grails Archive](http://www.grails.org/download/)
and follow the installation instructions for your operating system.  
**OR**  
Install `sdkman` from [sdkman.io](https://sdkman.io/install)
Once installation is done, install `grails` version 2.4.3 using this command
```shell
sdk install grails 2.4.3
```

[Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [PostgreSQL](http://www.postgresql.org/) can be downloaded and installed by visiting the vendor's websites, or in linux environments by using the package manager (apt-get or yum).

## Cloning the Source Code from GitHub

Install [Git](http://git-scm.com/download/). 

Clone the repository from `git@github.com:billinghub/earnbill-convergent-billing.git`

```shell
git clone git@github.com:billinghub/earnbill-convergent-billing.git
```

## Configuring PostgreSQL:

To run jBilling with the out-of-box reference database, you must have PostgreSQL installed and configured with a **'jbilling'** user and an empty **'jbilling_test'** database. The setup scripts also expect that the user will allow local connections without a password.

Edit the PostgreSQL pg_hba.conf file and change the "local" and "IPv4" localhost connection types:
```html
# "local" is for Unix domain socket connections only
local   all         all                               trust
# IPv4 local connections:
host    all         all         127.0.0.1/32          trust
```
Connect to PostgreSQL and create the test user and database.

```shell
CREATE ROLE jbilling WITH LOGIN SUPERUSER CREATEDB CREATEROLE PASSWORD 'jbilling';
CREATE DATABASE jbilling_test WITH OWNER jbilling;
```

## Setup:

Run the grails `compile` target to compile the jBilling source code, then run the `prepare-test` target to load the reference database and prepare all the required resources.

```shell
cd service-modules/ 
mvn clean install
cd ..
grails clean
grails compile
grails compile
grails copy-resources
grails compile-reports
grails compile-designs
sh ./run-app.sh
```

*The Grails compile target may halt with a compiler error on some environments, running `compile` a second time usually resolves the issue.*

## Running from Source:

### Windows: 
```shell
run-app.bat
```

### Linux/Mac: 
```shell
./run-app.sh
```

## Running in Debug mode:

- Execute the `debug-app.sh` file this will start listening to **Java Debug Wire Protocol** (JDWP) on port 5005.
![img.png](docs/images/jdwp_start.png)
- Create a new Debug Configuration  
![img.png](docs/images/debug_config.png)
- Save the configuration and click on debug icon  
![img.png](docs/images/save_config.png)
