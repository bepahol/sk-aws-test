#!/bin/ksh

cd `dirname $0`

echo "
<project>
  <groupId>edu.berkeley</groupId>
  <artifactId>simple-project</artifactId>
  <modelVersion>4.0.0</modelVersion>
  <name>Simple Project</name>
  <packaging>jar</packaging>
  <version>1.0</version>
  <dependencies>
    <dependency> <!-- Spark dependency -->
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-sql_2.11</artifactId>
      <version>2.3.1</version>
    </dependency>
  </dependencies>
  <properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
  </properties>
</project>
" > ~/spark-2.3.1-bin-hadoop2.7/pom.xml

mkdir -p ~/spark-2.3.1-bin-hadoop2.7/src/main/java
echo "
/* SimpleApp.java */
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;

public class SimpleApp {
  public static void main(String[] args) {
    String logFile = \"README.md\"; // Should be some file on your system
    SparkSession spark = SparkSession.builder().appName(\"Simple Application\").getOrCreate();
    Dataset<String> logData = spark.read().textFile(logFile).cache();

    long numAs = logData.filter(s -> s.contains(\"a\")).count();
    long numBs = logData.filter(s -> s.contains(\"b\")).count();

    System.out.println(\"Lines with a: \" + numAs + \", lines with b: \" + numBs);

    spark.stop();
  }
}
" > ~/spark-2.3.1-bin-hadoop2.7/src/main/java/SimpleApp.java

echo "
/* SimpleApp.java */
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;

public class SimpleApp {
  public static void main(String[] args) {
    String logFile = \"/var/tmp/silverking/skfs/skfs_mnt/skfs/README.md\"; // Should be some file on your system
    SparkSession spark = SparkSession.builder().appName(\"Simple Application\").getOrCreate();
    Dataset<String> logData = spark.read().textFile(logFile).cache();

    long numAs = logData.filter(s -> s.contains(\"a\")).count();
    long numBs = logData.filter(s -> s.contains(\"b\")).count();

    System.out.println(\"Lines with a: \" + numAs + \", lines with b: \" + numBs);

    spark.stop();
  }
}
" > ~/spark-2.3.1-bin-hadoop2.7/src/main/java/SimpleAppSkfs.java

cp ~/spark-2.3.1-bin-hadoop2.7/README.md /var/tmp/silverking/skfs/skfs_mnt/skfs/README.md

export JAVA_HOME=/usr/lib/jvm/java-1.8.0
cd ~/spark-2.3.1-bin-hadoop2.7/
mvn package

cp ~/spark-2.3.1-bin-hadoop2.7/target/simple-project-1.0.jar /var/tmp/silverking/skfs/skfs_mnt/skfs/simple-project-1.0.jar
