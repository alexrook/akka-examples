package com.example

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.example.DataChunk._

object MainRoute {
  private val newline = ByteString("\n")

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport
      .json()
      // comment out the lines below to comma-delimited JSON streaming
      .withFramingRenderer(
        // this enables new-line delimited JSON streaming
        Flow[ByteString].map(byteString => byteString ++ newline)
      )

  def route: Route =
    path("finite") {
      get {
        complete(DataSource.source)
      }
    } ~
      path("infinite") {
        get {
          complete(DataSource.iSource)
        } ~
        put {
          complete {
            val of = DataSource.flag
            val nf = !of
            DataSource.flag = nf
            s"old flag:$of, new flag:$nf\n"
          }
        }
      }

}
