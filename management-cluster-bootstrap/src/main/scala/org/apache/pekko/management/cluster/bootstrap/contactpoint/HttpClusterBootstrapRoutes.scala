/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2017-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.management.cluster.bootstrap.contactpoint

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.cluster.{ Cluster, Member }
import org.apache.pekko.event.{ Logging, LoggingAdapter }
import org.apache.pekko.http.javadsl.server.directives.RouteAdapter
import org.apache.pekko.http.scaladsl.model.{ HttpRequest, Uri }
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrapSettings
import org.apache.pekko.management.cluster.bootstrap.contactpoint.HttpBootstrapJsonProtocol.{ ClusterMember, SeedNodes }

import scala.concurrent.duration._

final class HttpClusterBootstrapRoutes(settings: ClusterBootstrapSettings) extends HttpBootstrapJsonProtocol {

  import org.apache.pekko.http.scaladsl.server.Directives._

  private def routeGetSeedNodes: Route = extractClientIP { clientIp =>
    extractActorSystem { implicit system =>
      import org.apache.pekko.cluster.MemberStatus
      val cluster = Cluster(system)

      def memberToClusterMember(m: Member): ClusterMember =
        ClusterMember(m.uniqueAddress.address, m.uniqueAddress.longUid, m.status.toString, m.roles)

      val state = cluster.state

      // TODO shuffle the members so in a big deployment nodes start joining different ones and not all the same?
      val members = state.members
        .diff(state.unreachable)
        .filter(m =>
          m.status == MemberStatus.up || m.status == MemberStatus.weaklyUp || m.status == MemberStatus.joining)
        .take(settings.contactPoint.httpMaxSeedNodesToExpose)
        .map(memberToClusterMember)

      val info = SeedNodes(cluster.selfMember.uniqueAddress.address, members)
      log.info(
        "Bootstrap request from {}: Contact Point returning {} seed-nodes [{}]",
        clientIp,
        members.size,
        members.map(_.node).mkString(", "))
      complete(info)
    }
  }

  /** Scala API */
  val routes: Route =
    (path("bootstrap" / "seed-nodes") & get) {
      toStrictEntity(1.second) { // always drain everything
        routeGetSeedNodes
      }
    }

  /** Java API */
  def getRoutes: org.apache.pekko.http.javadsl.server.Route = RouteAdapter(routes)

  private def log(implicit sys: ActorSystem): LoggingAdapter =
    Logging(sys, classOf[HttpClusterBootstrapRoutes])

}

object ClusterBootstrapRequests {

  import org.apache.pekko.http.scaladsl.client.RequestBuilding._

  def bootstrapSeedNodes(baseUri: Uri): HttpRequest =
    Get(baseUri + "/bootstrap/seed-nodes")

}