package org.spod.error

case class FeedFormatError(cause: Throwable)
    extends SPodError(s"Wrong RSS Feed format: ${cause.getMessage}", Some(cause))
