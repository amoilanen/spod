package org.spod.error

abstract class SPodError(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.getOrElse(null))
