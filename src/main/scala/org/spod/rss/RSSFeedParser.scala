package org.spod.rss

import java.io.{BufferedInputStream, ByteArrayInputStream}

import scala.xml.XML
import cats.data.NonEmptyList
import zio.{Has, IO, ULayer, ZIO, ZLayer}
import org.spod.error.SPodError

object RSSFeedParser {

  type HasRSSFeedParser = Has[RSSFeedParser.Service]

  trait Service {
    def parse(feed: String): IO[NonEmptyList[SPodError], RSSFeed]
  }

  val live: ULayer[HasRSSFeedParser] = ZLayer.succeed(new Service {
    override def parse(feed: String): IO[NonEmptyList[SPodError], RSSFeed] = {
      //TODO: Run loading XML and other operations which might fail into "effects" more carefully
      //TODO: Make sure the opened streams are closed: use ManagedResource?
      val root = XML.load(
        new BufferedInputStream(new ByteArrayInputStream(feed.getBytes()))
      )
      IO.fromEither(RSSFeed.from(root).toEither)
    }
  })

  def parse(
      feed: String
  ): ZIO[HasRSSFeedParser, NonEmptyList[SPodError], RSSFeed] =
    ZIO.accessM(_.get.parse(feed))
}
