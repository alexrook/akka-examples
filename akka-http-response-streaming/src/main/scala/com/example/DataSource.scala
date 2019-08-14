package com.example

import scala.collection.immutable
import scala.concurrent.duration._
import scala.util.Random

import java.io.FileInputStream

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Mutation, Result, Scan}
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}

import akka.NotUsed
import akka.stream.alpakka.hbase.HTableSettings
import akka.stream.alpakka.hbase.scaladsl.HTableStage
import akka.stream.scaladsl.{Flow, Source}

import com.example.models._

object DataSource {

  var flag = true

  //finite
  def source = Source(
    List(
      DataChunk(1, "the first"),
      DataChunk(2, "the second"),
      DataChunk(3, "the third"),
      DataChunk(4, "the fourth"),
      DataChunk(5, "the fifth"),
      DataChunk(6, "the sixth")
    )
  )
  // you need throttling for demonstration, otherwise
  // it's too fast and you don't see what's happening
  def thrSource: Source[DataChunk, NotUsed] = source.throttle(1, 1.second)

  def sourceConcat: Source[DataChunk, NotUsed] = {
    val exMap: Map[Int, Seq[String]] =
      Map(1 -> Seq("a", "b", "c"), 2 -> Seq("d", "e", "f"), 3 -> Seq("g", "h", "i"), 4 -> Seq("j", "k", "l"), 5 -> Seq("m", "n", "o"))
    source.via {
      Flow[DataChunk].flatMapMerge(5, (dc: DataChunk) => {
        println(dc)
        val list = (exMap.getOrElse(dc.id, Seq.empty) :+ "-").toList
        println(list.mkString(","))
        Source(list).map(DataChunk(dc.id, _))
      })
    }
  }.throttle(1, 1.second)

  //infinite
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
  val information: Array[Byte] = Bytes.toBytes("Information")
  val attributes:  Array[Byte] = Bytes.toBytes("Attributes")

  def hbUsersSource: Source[User, NotUsed] = {
    val uConv: User => immutable.Seq[Mutation] = _ => List.empty
    val tableSettings: HTableSettings[User] =
      HTableSettings(hBaseConfig, TableName.valueOf("Dathena:USERS"), immutable.Seq("Attributes"), uConv)

    val scan = new Scan()

    HTableStage
      .source(scan, tableSettings)
      .via {
        Flow[Result].map { ret: Result =>
          import scala.collection.JavaConverters._

          val m: Map[String, String] =
            ret.getFamilyMap(attributes).asScala.toMap.map {
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
    import scala.collection.JavaConverters._

    val conv: UserDocs => immutable.Seq[Mutation] = _ => List.empty
    val tableSettings: HTableSettings[UserDocs] =
      HTableSettings(hBaseConfig, TableName.valueOf("Dathena:USER-DOC"), immutable.Seq("Information"), conv)

    val scan = new Scan()

    HTableStage
      .source(scan, tableSettings)
      .map { ret: Result =>
        val m: Map[String, String] =
          ret.getFamilyMap(information).asScala.toMap.map {
            case (c, v) => (Bytes.toString(c), Bytes.toString(v))
          }

        for {
          user: String <- m.get("user")
          _ <- m.get("file")
        } yield user

      }
      .scan(Map.empty[String, UserDocs]) { (acc: Map[String, UserDocs], pair: Option[String]) =>
        pair
          .map { user: String =>
            {
              val old: UserDocs = acc.getOrElse(user, UserDocs(user, 0L))
              acc.updated(user, UserDocs(user, old.docs + 1))
            }
          }
          .getOrElse(acc)
      }
      .map(_.values.toSeq)
  }

  def hbFileUsers(file: String): Source[String, NotUsed] = {

    val tableSettings: HTableSettings[UserDocs] =
      HTableSettings(hBaseConfig, TableName.valueOf("Dathena:USER-DOC"), immutable.Seq("Information"), _ => List.empty)

    val filter = new SingleColumnValueFilter(information, Bytes.toBytes("file"), CompareOp.EQUAL, Bytes.toBytes(file))
    val scan   = new Scan()

    scan.setFilter(filter)

    HTableStage
      .source(scan, tableSettings)
      .map { ret: Result =>
        Bytes.toString(CellUtil.cloneValue(ret.getColumnLatestCell(information, Bytes.toBytes("user"))))
      }
  }

  def hbUserFiles(user: String): Source[String, NotUsed] = {

    val tableSettings: HTableSettings[UserDocs] =
      HTableSettings(hBaseConfig, TableName.valueOf("Dathena:USER-DOC"), immutable.Seq("Information"), _ => List.empty)

    val filter = new SingleColumnValueFilter(information, Bytes.toBytes("user"), CompareOp.EQUAL, Bytes.toBytes(user))
    val scan   = new Scan()

    scan.setFilter(filter)

    HTableStage
      .source(scan, tableSettings)
      .map { ret: Result =>
        Bytes.toString(CellUtil.cloneValue(ret.getColumnLatestCell(information, Bytes.toBytes("file"))))
      }
  }

  lazy val hBaseConfig: Configuration = {
    val config: Configuration = new Configuration(false)

    config.addResource(new FileInputStream("/etc/hbase/conf/hbase-site.xml"))

    HBaseConfiguration.create(config)
  }

}
