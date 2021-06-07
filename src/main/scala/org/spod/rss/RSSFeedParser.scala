package org.spod.rss

import java.io.{BufferedInputStream, ByteArrayInputStream}

import scala.xml.XML
import zio.{Has, IO, ULayer, ZIO, ZLayer}
import org.spod.error.FeedParserError

object RSSFeedParser {

  type RSSFeedParserEnv = Has[RSSFeedParser.Service]

  trait Service {
    def parse(feed: String): IO[FeedParserError, RSSFeed]
  }

  val live: ULayer[RSSFeedParserEnv] = ZLayer.succeed(new Service {
    override def parse(feed: String): IO[FeedParserError, RSSFeed] = {
      //TODO: Run loading XML and other operations which might fail into "effects" more carefully
      //TODO: Make sure the opened streams are closed: use ManagedResource?
      val root = XML.load(
        new BufferedInputStream(new ByteArrayInputStream(feed.getBytes()))
      )
      IO.fromEither(RSSFeed.from(root).toEither.left.map(FeedParserError(_)))
    }
  })

  def parse(
      feed: String
  ): ZIO[RSSFeedParserEnv, FeedParserError, RSSFeed] =
    ZIO.accessM(_.get.parse(feed))
}
