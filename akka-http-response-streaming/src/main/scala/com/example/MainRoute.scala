package com.example

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

  def route: Route =
    path("demo") {
      get {
        parameters("r".?) { resName: Option[String] =>
          val fileName = resName.getOrElse("index.html")
          complete(getResource(fileName))
        }
      }
    } ~
      path("finite") {
        get {
          import StreamingJson.chunkJson
          complete(DataSource.source)
        }
      } ~
      pathPrefix("infinite") {
        pathEnd {
          get {
            import StreamingJson.chunkJson
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
            import StreamingJson.chunkJson
            complete(DataSource.iSourceFlow)
          }
        }

      } ~
      pathPrefix("users") {
        pathEnd {
          import StreamingJson.chunkJson
          complete(DataSource.hbUsersSource)
        } ~
        path("docs") {
          get {
            import spray.json.DefaultJsonProtocol._

            import StreamingJson.chunkJson
            complete(DataSource.hbUserDocs)
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
