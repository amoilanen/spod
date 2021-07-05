package org.spod.download

import java.io.{BufferedInputStream, BufferedOutputStream, Closeable, File, FileOutputStream, IOException, InputStream, OutputStream}
import java.net.URL

import org.spod.error.SPodError
import sttp.client3.httpclient.zio.SttpClient
import zio.blocking.Blocking
import zio.{Has, IO, Task, ZIO, ZLayer, ZManaged}
import zio.console.Console
import zio.stream.{ZSink, ZStream}

object Downloader {

  type DownloaderEnv = Has[Downloader.Service]

  trait Service {
    def download(link: URL, destination: File): IO[SPodError, Unit]
  }

  val live =
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

            private def copy(in: InputStream, out: OutputStream): ZIO[Blocking, IOException, Long] = {
              val source = ZStream.fromInputStream(in)
              val sink = ZSink.fromOutputStream(out)

              //TODO: Create ProgressBar and report progress
              source.run(sink)
            }

            override def download(
                link: URL,
                destination: File
            ): IO[SPodError, Unit] = {
              (for {
                in <- ZManaged.make(inputStreamFromLink(link))(close)
                out <- ZManaged.make(outputStreamToFile(destination))(close)
                downloadableLength <- contentLength(link).toManaged_
                _ <- copy(in, out).provideLayer(Blocking.live).toManaged_
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
