package org.spod

import org.spod.progress.ProgressBar
import zio.Runtime

object Progress extends App {

  val runtime = Runtime.default
  val maxValue = 2143
  val progressBar = new ProgressBar(maxValue)
  (0 to maxValue).foreach(currentProgress => {
    Thread.sleep(10)
    runtime.unsafeRun(progressBar.printProgress(currentProgress))
  })
}
