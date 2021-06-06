package org.spod.rss

import java.io.{BufferedInputStream, ByteArrayInputStream}

import cats.data.NonEmptyList
import org.spod.error.SPodError

import scala.xml.XML

object RSSFeedParser {

  def parse(feed: String): Either[NonEmptyList[SPodError], RSSFeed] = {
    val root = XML.load(new BufferedInputStream(new ByteArrayInputStream(feed.getBytes())))
    RSSFeed.from(root).toEither
  }
}
