package org.spod.download

import java.io.{BufferedInputStream, BufferedOutputStream, Closeable, File, FileOutputStream, IOException, InputStream, OutputStream}
import java.net.URL

import org.spod.error.SPodError
import sttp.client3.httpclient.zio.SttpClient
import zio.{Has, IO, Task, ZIO, ZLayer, ZManaged}
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

            private def inputStreamFromLink(link: URL): Task[BufferedInputStream] =
              ZIO.effect(new BufferedInputStream(link.openStream()))
            private def outputStreamToFile(file: File): Task[BufferedOutputStream] =
              ZIO.effect(new BufferedOutputStream(new FileOutputStream(file)))
            private def close(closeable: Closeable) =
              ZIO.effectTotal(closeable.close())

            private def contentLength(link: URL): Task[Int] =
              ZIO.effect {
                val urlConnection = link.openConnection()
                urlConnection.getContentLength
              }

            private def copy(in: InputStream, out: OutputStream) = {
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

            override def download(
                link: URL,
                destination: File
            ): IO[SPodError, Unit] = {
              (for {
                in <- ZManaged.make(inputStreamFromLink(link))(close)
                out <- ZManaged.make(outputStreamToFile(destination))(close)
                downloadableLength <- contentLength(link).toManaged_
                _ <- copy(in, out).toManaged_
              } yield ())
                .mapError(error => new SPodError("Failed to download link", Some(error)))
                .useNow
            }
          }
      }

  def download(
      link: URL,
      destination: File
  ): ZIO[DownloaderEnv, SPodError, Unit] =
    ZIO.accessM(_.get.download(link, destination))
}
