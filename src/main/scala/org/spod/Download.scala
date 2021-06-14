package org.spod

import java.io.File
import java.net.URL

import org.spod.download.Downloader
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.Runtime
import zio.console

object Download extends App {

  val url =
    new URL(
      "https://play.podtrac.com/npr-500005/edge1.pod.npr.org/anon.npr-mp3/npr/newscasts/2021/06/14/newscast160837.mp3"
    )
  val destinationFile = new File("./temp.mp3")

  val runtime = Runtime.default

  val downloaderEnv =
    (HttpClientZioBackend.layer() ++ console.Console.live) >>> Downloader.live

  val downloadUrl = runtime.unsafeRun(
    Downloader.download(url, destinationFile).provideLayer(downloaderEnv)
  )
  println(downloadUrl)
}
