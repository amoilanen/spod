package org.spod.download

import java.io.{BufferedInputStream, BufferedOutputStream, Closeable, File, FileOutputStream, IOException, InputStream, OutputStream}
import java.net.URL

import org.spod.error.SPodError
import org.spod.progress.ProgressBar
import zio.blocking.Blocking
import zio.{Has, IO, Ref, Task, ZIO, ZLayer, ZManaged}
import zio.console.Console
import zio.stream.{ZSink, ZStream}

object Downloader {

  type DownloaderEnv = Has[Downloader.Service]

  trait Service {
    def download(link: URL, destination: File): IO[SPodError, Unit]
  }

  val live =
    ZLayer
      .fromFunction[Console with Blocking, Downloader.Service] {
        env =>
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

            private def copy(totalLength: Int, in: InputStream, out: OutputStream): ZIO[Console with Blocking, IOException, Long] = {
              val progressBar = new ProgressBar(totalLength)
              val source = ZStream.fromInputStream(in)
              val sink = ZSink.fromOutputStream(out)

              val totalProgressRef = Ref.make(0)
              source.mapChunksM(chunk => for {
                total <- totalProgressRef
                total <- total.updateAndGet(x => x + chunk.length)
                _ <- progressBar.printProgress(total)
              } yield chunk).run(sink)
            }

            override def download(
                link: URL,
                destination: File
            ): IO[SPodError, Unit] = {
              (for {
                in <- ZManaged.make(inputStreamFromLink(link))(close)
                out <- ZManaged.make(outputStreamToFile(destination))(close)
                downloadableLength <- contentLength(link).toManaged_
                _ <- copy(downloadableLength, in, out).toManaged_
              } yield ())
                .mapError(error => new SPodError("Failed to download link", Some(error)))
                .useNow
            }.provide(env)
          }
      }

  def download(
      link: URL,
      destination: File
  ): ZIO[DownloaderEnv, SPodError, Unit] =
    ZIO.accessM(_.get.download(link, destination))
}
