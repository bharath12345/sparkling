import AssemblyKeys._

name := "sparkling"

version := "1.0"

organization  := "in.bharathwrites"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
    ("org.apache.spark"       %  "spark-core_2.10"      % "0.9.1").
      exclude("org.mortbay.jetty", "servlet-api").
      exclude("commons-beanutils", "commons-beanutils-core").
      exclude("commons-collections", "commons-collections").
      exclude("commons-collections", "commons-collections").
      exclude("com.esotericsoftware.minlog", "minlog"),
    "org.apache.spark"       %  "spark-mllib_2.10"     % "0.9.1",
    "org.apache.commons"     %  "commons-math"         % "2.2"
  )

assemblySettings
