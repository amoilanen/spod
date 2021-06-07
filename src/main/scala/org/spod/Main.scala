package org.spod

import java.net.URL

import org.spod.error.{FeedParserError, SPodError}
import org.spod.rss.{RSSClient, RSSFeed, RSSFeedParser}
import zio.{Runtime, UIO, URIO}
import sttp.client3.httpclient.zio.HttpClientZioBackend

object Main extends App {

  val feed =
    """<?xml version="1.0" encoding="UTF-8" ?>
      |<rss version="2.0">
      |<channel>
      | <title>RSS Title</title>
      | <description>This is an example of an RSS feed</description>
      | <link>http://www.example.com/main.html</link>
      | <copyright>2020 Example.com All rights reserved</copyright>
      | <lastBuildDate>Mon, 06 Sep 2010 00:01:00 +0000 </lastBuildDate>
      | <pubDate>Sun, 06 Sep 2009 16:20:00 +0000</pubDate>
      | <ttl>1800</ttl>
      |
      | <item>
      |  <title>Example entry</title>
      |  <description>Here is some text containing an interesting description.</description>
      |  <link>http://www.example.com/blog/post/1</link>
      |  <guid isPermaLink="false">7bd204c6-1655-4c27-aeee-53f933c5395f</guid>
      |  <pubDate>Sun, 06 Sep 2009 16:20:00 +0000</pubDate>
      | </item>
      |
      |</channel>
      |</rss>
      |""".stripMargin

  val runtime = Runtime.default
  val parsedFeed: UIO[Either[FeedParserError, RSSFeed]] =
    RSSFeedParser.parse(feed).provideLayer(RSSFeedParser.live).either

  val feedParsingResult = runtime.unsafeRun(parsedFeed)
  println(feedParsingResult)

  val url = new URL("http://feeds.bbci.co.uk/news/world/rss.xml")
  val rssClientEnv =
    (RSSFeedParser.live ++ HttpClientZioBackend.layer()) >>> RSSClient.live

  val parsedFetchFeed: UIO[Either[Throwable, RSSFeed]] =
    RSSClient.fetchFeed(url).provideLayer(rssClientEnv).either

  val feedFetchingAndParsingResult = runtime.unsafeRun(parsedFetchFeed)
  println(feedFetchingAndParsingResult)
}
