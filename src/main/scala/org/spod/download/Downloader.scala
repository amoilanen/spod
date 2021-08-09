package org.spod.download

import java.io.{BufferedInputStream, BufferedOutputStream, Closeable, File, FileOutputStream, IOException, InputStream, OutputStream}
import java.net.URL

import org.spod.error.SPodError
import org.spod.progress.ProgressBar
import zio.blocking.Blocking
import zio.{Has, IO, RIO, Ref, Task, ZIO, ZLayer, ZManaged}
import zio.console.Console
import zio.stream.{ZSink, ZStream}
import zio.blocking._

object Downloader {

  type DownloaderEnv = Has[Downloader.Service]

  trait Service {
    def download(link: URL, destination: File): IO[SPodError, Unit]
  }

  case class ServiceImpl(env: Console with Blocking) extends Service {
    override def download(
                           link: URL,
                           destination: File
                         ): IO[SPodError, Unit] = {
      (for {
        in <- ZManaged.make(inputStreamFromLink(link))(close)
        out <- ZManaged.make(outputStreamToFile(destination))(close)
        downloadableLength <- contentLength(link).provide(env).toManaged_
        _ <- copy(downloadableLength, in, out).provide(env).toManaged_
      } yield ())
        .mapError(error =>
          new SPodError("Failed to download link", Some(error))
        )
        .useNow
    }

    private def inputStreamFromLink(
                                     link: URL
                                   ): Task[BufferedInputStream] =
      ZIO.effect(new BufferedInputStream(link.openStream()))

    private def outputStreamToFile(
                                    file: File
                                  ): Task[BufferedOutputStream] =
      ZIO.effect(new BufferedOutputStream(new FileOutputStream(file)))

    private def close(closeable: Closeable) =
      ZIO.effectTotal(closeable.close())

    private def contentLength(link: URL): RIO[Blocking, Int] =
      effectBlocking {
        val urlConnection = link.openConnection()
        urlConnection.getContentLength
      }

    private def copy(
                      totalLength: Int,
                      in: InputStream,
                      out: OutputStream
                    ): ZIO[Console with Blocking, IOException, Long] = {
      val progressBar = new ProgressBar(totalLength)
      val source = ZStream.fromInputStream(in)
      val sink = ZSink.fromOutputStream(out)

      Ref.make(0).flatMap({ totalProgressRef =>
        source.mapChunksM(chunk =>
          for {
            total <- totalProgressRef.updateAndGet(_ + chunk.length)
            _ <- progressBar.printProgress(total)
          } yield chunk
        ).run(sink)
      })
    }
  }

  val live =
    ZLayer
      .fromFunction[Console with Blocking, Downloader.Service] { env =>
        ServiceImpl(env)
      }

  def download(
      link: URL,
      destination: File
  ): ZIO[DownloaderEnv, SPodError, Unit] =
    ZIO.accessM(_.get.download(link, destination))
}
