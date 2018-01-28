import _root_.sbtassembly.AssemblyPlugin.autoImport._
import _root_.sbtassembly.PathList

name := "YourTwitterSentiment"

version := "1.0"

scalaVersion := "2.11.12"
val sparkVersion = "2.2.0"
val configVersion = "1.3.0"
val coreNLPVersion = "3.8.0"
val jacksonVersion = "2.8.1"

scalacOptions ++=
  Seq("-encoding", "UTF8", "-unchecked", "-deprecation", "-language:postfixOps", "-language:implicitConversions", "-language:higherKinds", "-language:reflectiveCalls")


libraryDependencies ++= Seq(
  "org.apache.spark"      %%        "spark-core"              %   sparkVersion, // % Provided excludeAll ExclusionRule("org.scalatest"),
  "org.apache.spark"      %%        "spark-sql"               %   sparkVersion, // % Provided excludeAll ExclusionRule("org.scalatest"),
  "org.apache.spark"      %%        "spark-mllib"             %   sparkVersion,
  "org.apache.spark"      %%        "spark-hive"              %   sparkVersion, // % Provided excludeAll ExclusionRule("org.scalatest"),
  "org.apache.spark"      %%        "spar2-yarn"              %   sparkVersion, // % Provided excludeAll ExclusionRule("org.scalatest"),
  "org.apache.spark"      %%        "spark-streaming"         %   sparkVersion,
  "org.apache.spark"      %%        "spark-streaming-kafka-0-8" % sparkVersion,
  "org.apache.bahir"      %%        "spark-streaming-twitter"   % sparkVersion,
  "org.apache.kafka"      %%        "kafka"                     % "1.0.0",
  "org.twitter4j"         %         "twitter4j-core"            % "4.0.6",
  "com.google.code.gson"  %         "gson"                      % "2.8.2" withSources(),
  "com.typesafe"          %         "config"                    % configVersion,
  "edu.stanford.nlp"      %         "stanford-corenlp"          % coreNLPVersion, //Provided excludeAll ExclusionRule("org.slf4j"),
  "edu.stanford.nlp"      %         "stanford-corenlp"          % coreNLPVersion classifier "models",
  "com.johnsnowlabs.nlp" %%         "spark-nlp"                 % "1.2.4",
  "com.github.scopt"      %%        "scopt"                     % "3.5.0",
  "com.fasterxml.jackson.core" %    "jackson-databind"          % jacksonVersion
)


assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "streaming", "twitter", _*) => MergeStrategy.deduplicate
  case PathList("org", "apache", "spark", _*) => MergeStrategy.discard
  case PathList("org", "spark_project", _*) => MergeStrategy.discard
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
  case "log4j.properties" => MergeStrategy.deduplicate
  case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}