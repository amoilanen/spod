package org.spod.error

case class FeedFormatError(cause: Throwable)
    extends SPodError("Wrong RSS Feed format", Some(cause))
