/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

enablePlugins(JavaAppPackaging)

Universal / packageName := "app" // should produce app.zip

libraryDependencies += "com.amazonaws" % "aws-java-sdk-cloudformation" % "1.12.734" % IntegrationTest

libraryDependencies += "com.amazonaws" % "aws-java-sdk-autoscaling" % "1.12.734" % IntegrationTest

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.7.2" // aws SDK depends on old version of jackson

libraryDependencies += "org.scalatest" %% "scalatest" % Dependencies.scalaTestVersion % IntegrationTest
