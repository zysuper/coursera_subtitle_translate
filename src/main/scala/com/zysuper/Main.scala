package com.zysuper

import java.io._

import rx.lang.scala.Observable

import scala.collection.Iterator
import scala.io.Source

/**
 * Created by zysuper on 15-5-27.
 */
object Main extends App {
  if (args.length < 1) {
    println("[please input path: ")
  }

  case class Block(id: Int, timeString: String, value: String, var translate: String = null)

  def groupBy[T](iter: Iterator[T], p: T => Boolean): Iterator[List[T]] = {
    var a = List[List[T]]()
    while (iter.hasNext) {
      val seq = iter.takeWhile(p).toList
      a ::= seq
    }
    Iterator(a: _*)
  }

  def readBlocks(file: File): Observable[Block] = Observable.from {
    groupBy[String](Source.fromFile(file).getLines(), _.matches("\\s*") == false).
      map(seq => Block(seq(0).toInt, seq(1), seq.drop(2).mkString("\n"))).toIterable
  }

  def translate(blocks: Observable[Block]): Observable[Block] = {
    blocks.flatMap(b => GoogleTranslate.translateStream(b))
  }

  def translateFile(inFile: File, outFile: File): Unit = {
    val writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "utf-8"))
    try {
      translate(readBlocks(inFile)).toBlocking.toList.sortBy(_.id).foreach { b =>
        //println(b.value + " ====> " + b.translate)
        writer.println(b.id)
        writer.println(b.timeString)
        writer.println(b.value)
        if (b.translate != null)
          writer.println(b.translate)
        writer.println()
        writer.flush()
      }
    } finally {
      writer.close()
    }
  }

  val path = new File(args(0))
  if (path.isDirectory) {
    val list = path.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".srt")
    })
    list.asInstanceOf[Array[File]].toList.foreach { file =>
      val names = file.getName.split("\\.")
      val out = new File(file.getParentFile, names(0) + "_new." + names(1))
      translateFile(file, out)
    }
  } else {
    System.err.println("path is not exists or is not a file")
  }
  //Thread.sleep(3 * 60 * 1000)

}
