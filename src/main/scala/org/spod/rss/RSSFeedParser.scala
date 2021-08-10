package org.spod.rss

import java.io.{BufferedInputStream, ByteArrayInputStream}

import cats.syntax.option._

import scala.xml.XML
import zio.{Has, IO, URLayer, ZIO, ZLayer, ZManaged}
import org.spod.error.{FeedParserError, SPodError}
import org.spod.util.EffectWrappers
import zio.blocking.Blocking

import scala.util.Try

object RSSFeedParser {

  type RSSFeedParser = Has[RSSFeedParser.Service]

  trait Service {
    def parse(feed: String): IO[SPodError, RSSFeed]
  }

  class ServiceImpl(blocking: Blocking.Service) extends Service with EffectWrappers {
    override def parse(feed: String): IO[SPodError, RSSFeed] = {
      val feedStream = ZIO.effect(new BufferedInputStream(new ByteArrayInputStream(feed.getBytes())))
      (for {
        stream <- ZManaged.fromAutoCloseable(feedStream)
        root <- blocking.effectBlocking(Try(XML.load(stream)).toEither).absolve.toManaged_
        result <- IO.fromEither(RSSFeed.from(root).toEither.left
          .map(FeedParserError(_))).toManaged_
      } yield result).mapError(error => new SPodError("Failed to load feed", error.some)).useNow
    }
  }

  val live: URLayer[Blocking, RSSFeedParser] = ZLayer.fromService[Blocking.Service, RSSFeedParser.Service](blocking =>
    new ServiceImpl(blocking)
  )

  def parse(
      feed: String
  ): ZIO[RSSFeedParser, SPodError, RSSFeed] =
    ZIO.accessM(_.get.parse(feed))
}
