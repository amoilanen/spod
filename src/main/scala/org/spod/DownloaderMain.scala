package org.spod

import java.io.File
import java.net.URL

import org.spod.download.Downloader
import zio.Runtime
import zio.blocking.Blocking
import zio.console

object DownloaderMain extends App {

  val url =
    new URL(
      "https://play.podtrac.com/npr-500005/edge1.pod.npr.org/anon.npr-mp3/npr/newscasts/2021/06/14/newscast160837.mp3"
    )
  val destinationFile = new File("./temp.mp3")

  val runtime = Runtime.default

  val downloaderEnv =
    (Blocking.live ++ console.Console.live) >>> Downloader.live

  val downloadUrl = runtime.unsafeRun(
    Downloader.download(url, destinationFile).provideLayer(downloaderEnv)
  )
  println(downloadUrl)
}
