package com.zysuper

import java.util.concurrent.TimeoutException

import Main.Block
import retrofit.client.Response
import retrofit.http.{GET, Query}
import retrofit.{Callback, RestAdapter, RetrofitError}
import rx.lang.scala.Observable

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import scala.language.postfixOps
import scala.concurrent.duration._

/**
 * Created by zysuper on 15-5-27.
 */
object GoogleTranslate {

  trait GoogleTranslate {
    //@GET("/translate_a/single??client=t&sl=en&tl=zh-CN&hl=zh-CN&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&dt=at&ie=UTF-8&oe=UTF-8&otf=2&ssel=3&tsel=3&kc=1&tk=520401|857330")
    @GET("/public/2.0/bmt/translate??from=en&to=zh&client_id=zVxwWbTWp5mmR0syGVRG0kNs")
    def translate(@Query("q") term: String, callback: Callback[Page])
  }

  val restAdapter = new RestAdapter.Builder().setServer("http://openapi.baidu.com").setDebug(false).build()
  //val restAdapter = new RestAdapter.Builder().setServer("https://translate.google.co.jp").setDebug(true).build()

  val service = restAdapter.create(classOf[GoogleTranslate])

  class Content {
    var src: String = _
    var dst: String = _
  }

  class Page {
    var from: String = _
    var to: String = _
    var trans_result: Array[Content] = _
  }

  def callbackFuture[T]: (Callback[T], Future[T]) = {
    val p = Promise[T]()
    val cb = new Callback[T] {
      def success(t: T, response: Response) = {
        p success t
      }

      def failure(error: RetrofitError) = {
        p failure error
      }
    }

    (cb, p.future)
  }

  implicit class ObservableOps[T](obs: Observable[T]) {
    def timedOut(totalSec: Long): Observable[T] = Observable[T] { sub =>
      obs.merge(Observable.never.timeout(totalSec seconds)).subscribe(
        n => {
          sub.onNext(n)
        },
        e => e match {
          case _: TimeoutException => {
            println(s"timeOut $totalSec");
            sub.onCompleted()
          }
          case m => {
            sub.onError(m)
          }
        },
        () => {
          sub.onCompleted()
        }
      )
    }
  }

  def translateStream(term: Block): Observable[Block] = {
    Observable.from(translate(term))
  }

  def translate(term: Block): Future[Block] = {
    async {
      val (cb, f) = callbackFuture[Page]
      service.translate(term.value.replace("\n"," "), cb)
      val result = await {
        f
      }
      term.translate = result.trans_result(0).dst
      //println(term.translate)
      term
    }
  }

  def main(args: Array[String]) {
    GoogleTranslate.translate(Block(1, "abc", "what needs to be done is exactly the same.")).onComplete({
      case Success(s) => println(s)
      case Failure(e) => e.printStackTrace()
    })
    Thread.sleep(30 * 1000)
  }

}
