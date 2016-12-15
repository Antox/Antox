package im.tox.tox4j.impl.jni

import java.io.File
import java.net.URL

import com.google.common.io.Files
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.language.postfixOps
import scala.sys.process._

object ToxLoadJniLibrary {

  /**
   * Set this to true to enable downloading native libraries from the web.
   *
   * On mobile devices, for example, we may not want to download large
   * amounts of data and instead prefer to fail. The libraries on github are
   * non-stripped (debug) versions, so for mobile devices, they are a
   * bad fallback.
   */
  var webFallbackEnabled = false // scalastyle:ignore var.field

  private val logger = Logger(LoggerFactory.getLogger(getClass))

  private val RepoUrl = "https://raw.githubusercontent.com/tox4j/tox4j.github.io/master/native"

  private val AlreadyLoaded = "Native Library (.+) already loaded in another classloader".r
  private val NotFoundDalvik = "Couldn't load .+ from loader .+ findLibrary returned null".r
  private val NotFoundJvm = "no .+ in java.library.path".r

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  private val target = {
    val osName =
      if (sys.props("java.vm.name") == "Dalvik") {
        "Android"
      } else {
        sys.props("os.name")
      }

    Map(
      "Android" -> Map(
        "aarch64" -> "aarch64-linux-android",
        "armv7l" -> "arm-linux-androideabi",
        "i686" -> "i686-linux-android"
      ),
      "Linux" -> Map(
        "amd64" -> "x86_64-linux"
      ),
      "Mac OS X" -> Map(
        "x86_64" -> "x86_64-darwin"
      )
    )(osName)(sys.props("os.arch"))
  }

  private def withTempFile(prefix: String, suffix: String)(block: File => Unit): Unit = {
    val file = File.createTempFile(prefix, suffix)
    file.deleteOnExit()
    try {
      block(file)
    } finally {
      // This may fail if the OS doesn't support deleting files that are in use, but deleteOnExit
      // will ensure that it is cleaned up on normal JVM termination.
      file.delete()
    }
  }

  private def webUrl(name: String): URL = {
    val libraryName = System.mapLibraryName(name)
    new URL(s"$RepoUrl/$target/$libraryName")
  }

  /**
   * Downloads a native library from [[RepoUrl]]/[[target]] and loads it into the current [[ClassLoader]].
   *
   * @param name Base name of the library. E.g. for libtox4j-c.so, this is "tox4j-c".
   */
  def loadFromWeb(name: String, url: URL): Unit = {
    val libraryName = System.mapLibraryName(name)
    withTempFile(name, libraryName) { libraryFile =>
      logger.info(s"Downloading $url to $libraryFile")
      val start = System.currentTimeMillis()
      url #> libraryFile !!
      val end = System.currentTimeMillis()
      logger.info(s"Downloading $libraryName took ${end - start}ms")

      System.load(libraryFile.getPath)
    }
  }

  /**
   * Load a native library from an existing location by copying it to a new, temporary location and loading
   * that new library.
   *
   * @param location A [[File]] pointing to the existing library.
   */
  def loadFromSystem(location: File): Unit = {
    withTempFile(location.getName, location.getName) { libraryFile =>
      logger.info(s"Copying $location to $libraryFile")
      Files.copy(location, libraryFile)

      System.load(libraryFile.getPath)
    }
  }

  def load(name: String): Unit = {
    try {
      System.loadLibrary(name)
    } catch {
      case exn: UnsatisfiedLinkError =>
        exn.getMessage match {
          case AlreadyLoaded(location) =>
            logger.warn(s"${exn.getMessage} copying file and loading again")
            loadFromSystem(new File(location))
          case NotFoundDalvik() | NotFoundJvm() =>
            logger.warn(s"Library '$name' not found: ${exn.getMessage}")
            val url = webUrl(name)
            if (webFallbackEnabled) {
              loadFromWeb(name, url)
            } else {
              logger.error(
                s"Could not load native library '$name' and web download disabled " +
                  s"(or we would have downloaded it from $url)."
              )
            }
          case _ =>
            throw exn
        }
    }
  }

}
