package org.spod.rss

import java.io.{BufferedInputStream, ByteArrayInputStream}

import scala.xml.XML
import zio.{Has, IO, URLayer, ZIO, ZLayer}
import org.spod.error.FeedParserError
import zio.blocking.Blocking

object RSSFeedParser {

  type RSSFeedParser = Has[RSSFeedParser.Service]

  trait Service {
    def parse(feed: String): IO[FeedParserError, RSSFeed]
  }

  class ServiceImpl(blocking: Blocking.Service) extends Service {
    override def parse(feed: String): IO[FeedParserError, RSSFeed] = {
      //TODO: Run loading XML and other operations which might fail into "effects" more carefully
      //TODO: Make sure the opened streams are closed: use ManagedResource?
      val root = XML.load(
        new BufferedInputStream(new ByteArrayInputStream(feed.getBytes()))
      )
      IO.fromEither(RSSFeed.from(root).toEither.left.map(FeedParserError(_)))
    }
  }

  val live: URLayer[Has[Blocking.Service], Has[Service]] = ZLayer.fromService[Blocking.Service, RSSFeedParser.Service](blocking =>
    new ServiceImpl(blocking)
  )

  def parse(
      feed: String
  ): ZIO[RSSFeedParser, FeedParserError, RSSFeed] =
    ZIO.accessM(_.get.parse(feed))
}
