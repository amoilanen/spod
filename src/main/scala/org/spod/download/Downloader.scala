package org.spod.download

import java.io.File
import java.net.URL

import org.spod.error.SPodError
import sttp.client3.httpclient.zio.SttpClient
import zio.{Has, IO, ZIO, ZLayer}
import zio.console.Console
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException

object Downloader {

  type DownloaderEnv = Has[Downloader.Service]

  trait Service {
    def download(link: URL, destination: File): IO[SPodError, Unit]
  }

  val live: ZLayer[Has[Console.Service] with Has[
    SttpClient.Service
  ], Nothing, Has[Service]] =
    ZLayer
      .fromServices[Console.Service, SttpClient.Service, Downloader.Service] {
        (console, sttpClient) =>
          new Service {
            override def download(
                link: URL,
                destination: File
            ): IO[SPodError, Unit] = {
              //TODO: Wrap resource acquisition and stream creation into IOs
              //TODO: Is it possible to use SttpClient instead?

              //TODO: Create ProgressBar and report progress
              val in = new BufferedInputStream(link.openStream)
              val out = new FileOutputStream(destination)
              val urlConnection = link.openConnection()
              try {
                val totalContentLength = urlConnection.getContentLength
                val bufferSize = 1024
                val dataBuffer = new Array[Byte](bufferSize)
                var bytesRead = 0
                while (bytesRead != -1) {
                  bytesRead = in.read(dataBuffer, 0, bufferSize)
                  if (bytesRead > 0) {
                    out.write(dataBuffer, 0, bytesRead)
                  }
                }
              } catch {
                case e: IOException =>
                  IO.fail(new SPodError("Download failed", Some(e)))
              } finally {
                if (in != null) in.close()
                if (out != null) out.close()
              }
              // Just a stub, use real IOs inside the method and wrap effects into IOs
              IO.succeed()
            }
          }
      }

  def download(
      link: URL,
      destination: File
  ): ZIO[DownloaderEnv, SPodError, Unit] =
    ZIO.accessM(_.get.download(link, destination))
}
