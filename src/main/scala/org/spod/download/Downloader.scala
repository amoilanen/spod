package org.spod.download

import java.io.{BufferedInputStream, BufferedOutputStream, File, FileOutputStream, IOException, InputStream, OutputStream}
import java.net.URL

import org.spod.error.SPodError
import org.spod.progress.ProgressBar
import org.spod.util.EffectWrappers
import zio.blocking.Blocking
import zio.{Has, IO, Ref, Task, URLayer, ZIO, ZLayer, ZManaged}
import zio.console.Console
import zio.stream.{ZSink, ZStream}

object Downloader {

  type Downloader = Has[Downloader.Service]

  trait Service {
    def download(link: URL, destination: File): IO[SPodError, Unit]
  }

  case class ServiceImpl(console: Console.Service, blocking: Blocking.Service) extends Service with EffectWrappers {
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


    private def contentLength(link: URL): Task[Int] =
      blocking.effectBlocking {
        val urlConnection = link.openConnection()
        urlConnection.getContentLength
      }

    private def copy(
                      totalLength: Int,
                      in: InputStream,
                      out: OutputStream
                    ): IO[IOException, Long] = {
      val progressBar = new ProgressBar(totalLength)
      val source = ZStream.fromInputStream(in).provide(Has(blocking))
      val sink = ZSink.fromOutputStream(out).provide(Has(blocking))

      Ref.make(0).flatMap({ totalProgressRef =>
        source.mapChunksM(chunk =>
          for {
            total <- totalProgressRef.updateAndGet(_ + chunk.length)
            _ <- progressBar.printProgress(total).provide(Has(console))
          } yield chunk
        ).run(sink)
      })
    }
  }

  val live: URLayer[Console with Blocking, Downloader] =
    ZLayer
      .fromServices[Console.Service, Blocking.Service, Downloader.Service] { (console, blocking) =>
        ServiceImpl(console, blocking)
      }

  def download(
      link: URL,
      destination: File
  ): ZIO[Downloader, SPodError, Unit] =
    ZIO.accessM(_.get.download(link, destination))
}
