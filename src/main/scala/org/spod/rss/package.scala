package org.spod

import cats.data.ValidatedNel
import org.spod.error.SPodError

package object rss {

  type ParsingResult[A] = ValidatedNel[SPodError, A]
}
