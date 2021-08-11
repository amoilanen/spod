package org.spod

import java.net.URL
import java.nio.file.Paths

import org.spod.download.Downloader
import org.spod.rss.{RSSClient, RSSFeed, RSSFeedParser}
import zio.{Runtime, UIO, console}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.blocking.Blocking
import zio.console._

object Main extends App {
  val runtime = Runtime.default

  val url = new URL("https://rss.wbur.org/onpoint/rss")
  //TODO: Use ZIO effects
  val destinationDirectory = Paths.get("./podcast")
  destinationDirectory.toFile().mkdirs()
  val downloaderLayer = (Blocking.live ++ Console.live) >>> Downloader.live
  val rssClient =
    ((Blocking.live >>> RSSFeedParser.live) ++ downloaderLayer ++ Console.live ++ HttpClientZioBackend.layer()) >>> RSSClient.live


  val parsedFetchFeed: UIO[Either[Throwable, RSSFeed]] =
    RSSClient.fetchFeed(url, destinationDirectory).provideLayer(rssClient).either

  val feedFetchingAndParsingResult = runtime.unsafeRun(parsedFetchFeed)
  println(feedFetchingAndParsingResult)
}
