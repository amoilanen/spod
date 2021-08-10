package org.spod.rss

import java.net.URL

import org.spod.error.{FeedFetchError, SPodError}
import org.spod.rss.RSSFeedParser.RSSFeedParser
import sttp.client3.basicRequest
import sttp.client3.httpclient.zio.SttpClient
import sttp.model.Uri
import zio.{Has, IO, ZIO, ZLayer}

object RSSClient {

  type RSSClient = Has[RSSClient.Service]

  trait Service {
    def fetchFeed(url: URL): IO[SPodError, RSSFeed]
  }

  val live: ZLayer[SttpClient with RSSFeedParser, Nothing, RSSClient] =
    ZLayer.fromServices[
      SttpClient.Service,
      RSSFeedParser.Service,
      RSSClient.Service
    ] { (sttpClient, feedParser) =>
      new Service {
        override def fetchFeed(url: URL): IO[SPodError, RSSFeed] = {
          val request = basicRequest
            .get(Uri(url.toURI))

          for {
            rawFeed <-
              sttpClient
                .send(request)
                .map(
                  _.body.left.map(FeedFetchError(_))
                )
                .mapError(error =>
                  FeedFetchError("Failed to fetch feed", Some(error))
                )
                .absolve
            rssFeed <- feedParser.parse(rawFeed)
          } yield rssFeed
        }
      }
    }

  def fetchFeed(url: URL): ZIO[RSSClient, SPodError, RSSFeed] =
    ZIO.accessM(_.get.fetchFeed(url))
}
