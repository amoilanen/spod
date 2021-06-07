package org.spod.error

case class FeedFetchError(message: String, cause: Option[Throwable] = None)
    extends SPodError(message, cause)
