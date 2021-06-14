package org.spod.error

class SPodError(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.getOrElse(null))
