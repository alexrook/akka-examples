package com.example

import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, ResponseEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import akka.util.ByteString

import com.example.models._

object MainRoute {
  private val newline = ByteString("\n\n")

  object StreamingJson {
    implicit val chunkJson: JsonEntityStreamingSupport =
      EntityStreamingSupport
        .json()
        // comment out the lines below to comma-delimited JSON streaming
        .withFramingRenderer(
          // this enables new-line delimited JSON streaming
          Flow[ByteString].map(byteString => byteString ++ newline)
        )

    implicit val wholeJson: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  }

  import StreamingJson.chunkJson

  def route: Route =
    path("demo") {
      get {
        parameters("r".?) { resName: Option[String] =>
          val fileName = resName.getOrElse("index.html")
          complete(getResource(fileName))
        }
      }
    } ~
      pathPrefix("finite") {
        pathEnd {
          get {
            complete(DataSource.thrSource)
          }
        } ~
        path("concat") {
          get {
            complete(DataSource.sourceConcat)
          }
        }
      } ~
      pathPrefix("infinite") {
        pathEnd {
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
        } ~
        path("flow") {
          get {

            complete(DataSource.iSourceFlow)
          }
        }

      } ~
      pathPrefix("users") {
        pathEnd {
          complete(DataSource.hbUsersSource)
        } ~
        path("docs") {
          get {
            complete(DataSource.hbUserDocs)
          }
        } ~
        path("file") {
          get {
            parameter("f".as[String]) { f: String =>
              complete(DataSource.hbFileUsers(f))
            }
          }
        }

      } ~
      pathPrefix("files") {
        path("user") {
          get {
            parameter("f".as[String]) { f: String =>
              complete(DataSource.hbFileUsers(f))
            }
          }
        }
      }

  def getResource(fileName: String): HttpResponse = {
    import scala.io.Source
    val template = Source.fromResource(s"www/$fileName")
    val entity: ResponseEntity = HttpEntity(ContentTypes.`text/html(UTF-8)`, template.mkString)
    HttpResponse(entity = entity)
  }

}
