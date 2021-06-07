package org.spod.error

import cats.data.NonEmptyList

case class FeedParserError(formatErrors: NonEmptyList[FeedFormatError])
    extends SPodError(
      s"Failed to parse the feed, errors: ${formatErrors.map(_.getMessage).toList.mkString(",")}"
    )
