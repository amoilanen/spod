package org.spod

class ProgressBar(total: Int, barWidth: Int = 150) {
  def printProgress(completed: Int): Unit = {
    val completedPercent = (100 * completed) / total
    val completedWidth = (completedPercent * barWidth) / 100
    val progressValue = "*" * completedWidth + " " * (barWidth - completedWidth)
    val percent = 100 * (completed.toDouble / total.toDouble)
    val formattedPercent = f"$percent%2.2f"

    val progressDisplay = s"[$progressValue] $formattedPercent%\r"
    print(progressDisplay)
  }
}

object ProgressBar extends App {

  val maxValue = 2143
  val progressBar = new ProgressBar(maxValue)
  (0 to maxValue).foreach(percent => {
    Thread.sleep(10)
    progressBar.printProgress(percent)
  })
}
