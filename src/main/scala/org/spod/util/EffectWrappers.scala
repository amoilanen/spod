package org.spod.util

import java.io.Closeable

import zio.{UIO, ZIO}

trait EffectWrappers {

  def close(closeable: Closeable): UIO[Unit] =
    ZIO.effectTotal(closeable.close())
}
