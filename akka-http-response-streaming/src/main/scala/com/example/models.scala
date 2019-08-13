package com.example

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object models {

  case class DataChunk(id: Int, data: String)

  implicit val dataChunkJsonFormat: RootJsonFormat[DataChunk] = jsonFormat2(DataChunk)

  case class User(title:             Option[String],
                  mail:              Option[String],
                  userPrincipalName: Option[String],
                  displayName:       Option[String],
                  objectGUID:        String,
                  distinguishedName: String,
                  objectClass:       String)

  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat7(User)

  case class UserDocs(user: String, docs: Long)

  implicit val userDocsJsonFormat: RootJsonFormat[UserDocs] = jsonFormat2(UserDocs)

}
