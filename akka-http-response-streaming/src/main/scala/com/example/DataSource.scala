package com.example

import akka.NotUsed
import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import scala.util.Random

object DataSource {

  var flag = true

  def source: Source[DataChunk, NotUsed] =
    Source(
      List(
        DataChunk(1, "the first"),
        DataChunk(2, "the second"),
        DataChunk(3, "the thrid"),
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
              val n = Random.nextInt()
              val dc = DataChunk(n, s"data:$n")
              println(s"next chunk:$dc")
              dc
            }
            .takeWhile(_ => flag))
      .throttle(1, 2.second)

}
