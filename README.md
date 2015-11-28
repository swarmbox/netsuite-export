# NetSuite Export

NetSuite Export is a Java Application that exports all data from a NetSuite account and persists it to a MongoDB Database using the [NetSuite SuiteTalk WebServices API](http://www.netsuite.com/portal/platform/developer/suitetalk.shtml).

Originally inspired by [NetSuite Data Dumper](https://github.com/fferreri/netsuite-data-dumper).

## Setup an Instance of MongoDB

First, install and startup an instance of [MongoDB](https://docs.mongodb.org/manual/installation/). For example, on OS X:

```bash
cd ~/Downloads
curl -O https://fastdl.mongodb.org/osx/mongodb-osx-x86_64-3.0.7.tgz
tar xf mongodb-osx-x86_64-3.0.7.tgz
cd mongodb-osx-x86_64-3.0.7
mkdir data; touch mongodb.log
bin/mongod --fork --logpath "$(pwd)/mongodb.log" --dbpath "$(pwd)/data"
```

## Setup the NetSuite Export Application

From the project directory:

1) Generate the NetSuite Axis Classes:

```bash
mvn exec:java
```

2) Configure `<project-dir>/settings.xml` with appropriate values:

```xml
<settings>
    <ns-application-id>3EA7B1F7-C96A-5939-B3C4-81190DEA9C2F</ns-application-id>
    <ns-account-id>123456</ns-account-id>
    <ns-role-id>1000</ns-role-id><!-- Comment out this line to use the Administrator Role -->
    <ns-email>user@email.com</ns-email>
    <ns-password>pass1234</ns-password>
    <ns-page-size>1000</ns-page-size>
    <mg-server>127.0.0.1</mg-server>
    <mg-database>someDatabase</mg-database>
</settings>
```

3) Build and install the application:

```bash
mvn install
```

4) Run the application to export all data to MongoDB:

```bash
cd target/netsuite-export-1.0.0-distribution
java -jar netsuite-export-1.0.0.jar
```

## Notes

IMPORTANT: The database specified in `settings.xml` will be dropped and re-created each time this application is run.

Certain features may be disabled for a given NetSuite account. When records related to a disabled feature are processed an error will be logged and the export will continue.

## License

The project is published under a BSD license. See the [license file](https://github.com/swarmbox/netsuite-export/blob/master/LICENSE) for more information.

No NetSuite licensed code is provided by this project. NetSuite-related Apache Axis classes must be generated using NetSuite's SuiteTalk WebServices WSDL and used solely under the Terms and Conditions of their license.
