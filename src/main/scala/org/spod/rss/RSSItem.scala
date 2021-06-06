package org.spod.rss

import java.net.URL
import java.time.Instant

import cats.implicits._

import org.spod.error.FeedFormatError

import scala.util.Try
import scala.xml.Node
import java.text.SimpleDateFormat

case class RSSItem(
    title: String,
    description: String,
    link: URL,
    publicationDate: Instant
)

object RSSItem {

  val pubDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")

  def from(xml: Node): ParsingResult[RSSItem] = {
    val title = (xml \ "title").text
    val description = (xml \ "description").text
    val linkResult = Try(new URL((xml \ "link").text)).toEither.left
      .map(FeedFormatError(_))
      .toValidatedNel
    val pubDateResult = Try(
      pubDateFormat.parse((xml \ "pubDate").text).toInstant
    ).toEither.left.map(FeedFormatError(_)).toValidatedNel
    (linkResult, pubDateResult).mapN({
      case (link, pubDate) => RSSItem(title, description, link, pubDate)
    })
  }
}
