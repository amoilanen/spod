package org.spod.progress

import java.io.IOException

import zio.ZIO
import zio.console.{Console, putStr}

class ProgressBar(total: Int, barWidth: Int = 150) {

  private def formatCompletedPercent(completed: Int): String = {
    val percent = 100 * (completed.toDouble / total.toDouble)
    f"$percent%2.2f"
  }

  private def formatProgressBar(completed: Int): String = {
    val completedPercent = (100 * completed) / total
    val completedWidth = (completedPercent * barWidth) / 100
    "[" + "*" * completedWidth + " " * (barWidth - completedWidth) + "]"
  }

  def printProgress(completed: Int): ZIO[Console, IOException, Unit] = {
    val formattedProgress = s"${formatProgressBar(completed)} ${formatCompletedPercent(completed)}%\r"
    putStr(formattedProgress)
  }
}
