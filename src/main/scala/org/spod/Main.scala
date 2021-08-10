package org.spod

import java.net.URL

import org.spod.rss.{RSSClient, RSSFeed, RSSFeedParser}
import zio.{Runtime, UIO}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.blocking.Blocking

object Main extends App {
  val runtime = Runtime.default

  val url = new URL("http://feeds.bbci.co.uk/news/world/rss.xml")
  val rssClient =
    ((Blocking.live >>> RSSFeedParser.live) ++ HttpClientZioBackend.layer()) >>> RSSClient.live

  val parsedFetchFeed: UIO[Either[Throwable, RSSFeed]] =
    RSSClient.fetchFeed(url).provideLayer(rssClient).either

  val feedFetchingAndParsingResult = runtime.unsafeRun(parsedFetchFeed)
  println(feedFetchingAndParsingResult)
}
