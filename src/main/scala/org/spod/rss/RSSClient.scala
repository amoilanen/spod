package org.spod.rss

import java.net.URL
import java.nio.file.Path

import cats.syntax.option._
import org.spod.download.Downloader
import org.spod.download.Downloader.Downloader
import org.spod.error.{FeedFetchError, SPodError}
import org.spod.rss.RSSFeedParser.RSSFeedParser
import sttp.client3.basicRequest
import sttp.client3.httpclient.zio.SttpClient
import sttp.model.Uri
import zio.{Has, IO, ZIO, ZLayer}
import zio.console._

object RSSClient {

  type RSSClient = Has[RSSClient.Service]

  trait Service {
    def fetchFeed(url: URL, destinationDirectory: Path): IO[SPodError, RSSFeed]
  }

  class ServiceImpl(sttpClient: SttpClient.Service,
                    feedParser: RSSFeedParser.Service,
                    downloader: Downloader.Service,
                    console: Console.Service) extends Service {
    override def fetchFeed(url: URL, destinationDirectory: Path): IO[SPodError, RSSFeed] = {
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
        _ <- {
          val downloadedFeeds = rssFeed.items.map(item => {
            val destinationFile: Path = destinationDirectory.resolve(item.title)
            val urlToDownload: URL = item.link
            for {
              _ <- putStrLn(s"Downloading '${item.title}'...")
                .provide(Has(console))
                .mapError(error => new SPodError("Failed to print", error.some))
              result <- downloader.download(urlToDownload, destinationFile.toFile())
            } yield result
          })
          ZIO.collectAll(downloadedFeeds)
        }
      } yield rssFeed
    }
  }

  val live: ZLayer[SttpClient with RSSFeedParser with Downloader with Console, Nothing, RSSClient] =
    ZLayer.fromServices[
      SttpClient.Service,
      RSSFeedParser.Service,
      Downloader.Service,
      Console.Service,
      RSSClient.Service
    ] { (sttpClient, feedParser, downloader, console) =>
      new ServiceImpl(sttpClient, feedParser, downloader, console)
    }

  def fetchFeed(url: URL, destinationDirectory: Path): ZIO[RSSClient, SPodError, RSSFeed] =
    ZIO.accessM(_.get.fetchFeed(url, destinationDirectory))
}
