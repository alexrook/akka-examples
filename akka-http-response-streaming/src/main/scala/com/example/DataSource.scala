package com.example

import scala.collection.immutable
import scala.concurrent.duration._
import scala.util.Random

import java.io.FileInputStream

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Mutation, Result, Scan}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}

import akka.NotUsed
import akka.stream.alpakka.hbase.HTableSettings
import akka.stream.alpakka.hbase.scaladsl.HTableStage
import akka.stream.scaladsl.{Flow, Source}

import com.example.models._

object DataSource {

  var flag = true

  def source: Source[DataChunk, NotUsed] =
    Source(
      List(
        DataChunk(1, "the first"),
        DataChunk(2, "the second"),
        DataChunk(3, "the third"),
        DataChunk(4, "the fourth"),
        DataChunk(5, "the fifth"),
        DataChunk(6, "the sixth")
      )
      // you need throttling for demonstration, otherwise
      // it's too fast and you don't see what's happening
    ).throttle(1, 1.second)

  def iSource: Source[DataChunk, NotUsed] =
    Source
      .fromIterator(
        () =>
          Iterator
            .continually {
              val n  = Random.nextInt()
              val dc = DataChunk(n, s"data:$n")
              println(s"next chunk:$dc")
              dc
            }
            .takeWhile(_ => flag)
      )
      .throttle(1, 1.second)

  def iSourceFlow: Source[DataChunk, NotUsed] =
    iSource.via(
      //Similar to fold but is not a terminal operation,
      // emits its current value which starts at zero and then applies the current
      // and next value to the given function f, emitting the next current value
      Flow[DataChunk].scan(DataChunk(0, ""))((acc, dc) => acc.copy(id = acc.id + dc.id, data = acc.data + dc.data))
    )

//HBase
  def hbUsersSource: Source[User, NotUsed] = {
    val uConv: User => immutable.Seq[Mutation] = _ => List.empty
    val tableSettings: HTableSettings[User] =
      HTableSettings(getHBaseConfig, TableName.valueOf("Dathena:USERS"), immutable.Seq("Attributes"), uConv)

    val scan = new Scan()

    HTableStage
      .source(scan, tableSettings)
      .via {
        Flow[Result].map { ret: Result =>
          import scala.collection.JavaConverters._

          val m: Map[String, String] =
            ret.getFamilyMap(Bytes.toBytes("Attributes")).asScala.toMap.map {
              case (c, v) => (Bytes.toString(c), Bytes.toString(v))
            }

          User(
            title             = m.get("title"),
            mail              = m.get("mail"),
            userPrincipalName = m.get("userPrincipalName"),
            displayName       = m.get("displayName"),
            objectGUID        = m.getOrElse("objectGUID", ""),
            distinguishedName = m.getOrElse("distinguishedName", ""),
            objectClass       = m.getOrElse("objectClass_complete", "")
          )
        }
      }
  }

  def hbUserDocs: Source[Seq[UserDocs], NotUsed] = {
    val conv: UserDocs => immutable.Seq[Mutation] = _ => List.empty
    val tableSettings: HTableSettings[UserDocs] =
      HTableSettings(getHBaseConfig, TableName.valueOf("Dathena:USER-DOC"), immutable.Seq("Information"), conv)

    val scan = new Scan()

    HTableStage
      .source(scan, tableSettings)
      .via {
        Flow[Result].map { ret: Result =>
          import scala.collection.JavaConverters._

          val m: Map[String, String] =
            ret.getFamilyMap(Bytes.toBytes("Information")).asScala.toMap.map {
              case (c, v) => (Bytes.toString(c), Bytes.toString(v))
            }

          m.get("user").map { user: String =>
            user -> 1L
          }
        }
      }
      .via {
        Flow[Option[(String, Long)]]
          .scan(Map.empty[String, UserDocs]) { (acc: Map[String, UserDocs], pair: Option[(String, Long)]) =>
            pair
              .map {
                case (user: String, doc: Long) => {
                  val old = acc.getOrElse(user, UserDocs(user, 0L))
                  acc.updated(user, UserDocs(user, old.docs + doc))
                }
              }
              .getOrElse(acc)
          }
      }
      .map(_.values.toSeq)
  }

  private def getHBaseConfig: Configuration = {
    val config: Configuration = new Configuration(false)

    config.addResource(new FileInputStream("/etc/hbase/conf/hbase-site.xml"))

    HBaseConfiguration.create(config)
  }

}
