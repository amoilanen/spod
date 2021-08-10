package org.spod.rss

import cats.implicits._
import scala.xml.Node

case class RSSFeed(items: List[RSSItem])

object RSSFeed {
  def from(xml: Node): ParsingResult[RSSFeed] = {
    val itemsResult = (xml \ "channel" \ "item").map(RSSItem.from(_))

    itemsResult.sequence.map(items =>
      RSSFeed(items.toList))
  }
}
