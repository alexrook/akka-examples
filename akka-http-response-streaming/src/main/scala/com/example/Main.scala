package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object Main {
  def main(args: Array[String]): Unit = {
    val port: Int =9027
    implicit val system: ActorSystem = ActorSystem("Main")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    Http().bindAndHandle(MainRoute.route, "localhost", 9027)
    println(s"Server online at http://localhost:$port/")
  }
}
