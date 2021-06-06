package org.spod.rss

import org.spod.error.FeedFormatError

object RSSFeedParser {

  def parse(feed: String): Either[FeedFormatError, RSSFeed] = ???
}
