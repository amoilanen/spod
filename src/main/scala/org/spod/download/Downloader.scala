package org.spod.download

import java.io.{BufferedInputStream, BufferedOutputStream, File, FileOutputStream, IOException}
import java.net.URL

import org.spod.error.SPodError
import sttp.client3.httpclient.zio.SttpClient
import zio.{Has, IO, ZIO, ZLayer, ZManaged}
import zio.console.Console

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

            private def inputStream(link: URL): ZManaged[Any, Throwable, BufferedInputStream] =
              ZManaged.make(ZIO.effect(new BufferedInputStream(link.openStream())))(
                stream => ZIO.effectTotal(stream.close())
              )

            private def outputStream(file: File): ZManaged[Any, Throwable, BufferedOutputStream] =
              ZManaged.make(ZIO.effect(new BufferedOutputStream(new FileOutputStream(file))))(
                stream => ZIO.effectTotal(stream.close())
              )

            override def download(
                link: URL,
                destination: File
            ): IO[SPodError, Unit] = {
              inputStream(link).use { in: BufferedInputStream =>
                outputStream(destination). use { out: BufferedOutputStream =>
                  //TODO: Wrap the call to get total content length into a ZIO
                  val urlConnection = link.openConnection()
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
                  //TODO: Turn byte chunks read from the input stream into a ZStream
                  //TODO: Create ProgressBar and report progress
                  ZIO.succeed()
                }
              }.mapError(error => new SPodError("Failed to download link", Some(error)))
            }
          }
      }

  def download(
      link: URL,
      destination: File
  ): ZIO[DownloaderEnv, SPodError, Unit] =
    ZIO.accessM(_.get.download(link, destination))
}
