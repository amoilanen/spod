package org.spod

import cats.data.ValidatedNel
import org.spod.error.FeedFormatError

package object rss {

  type ParsingResult[A] = ValidatedNel[FeedFormatError, A]
}
