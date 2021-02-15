package services

import scala.concurrent.Await
import scala.concurrent.duration._

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server

import scala.concurrent.ExecutionContext.Implicits.global

class GithubContributorsServiceSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  "GithubContributorsService" should {
    "handle all pages from first to last" in {
      val testUri = "/test_crawl"
      val numberOfPages = 3
      val linkHeader = "link" -> s"""<$testUri?page=$numberOfPages>; rel="last""""

      Server.withRouterFromComponents() { components =>
        import Results._
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/test_crawl") =>
            Action {
              Ok(Json.arr(JsString(""))).withHeaders(linkHeader)
            }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val result = Await.result(
            new GithubContributorsService(client, "").crawlPages[String](testUri).value,
            10.seconds
          )
          result.isRight mustBe true
          result.toOption.get.length mustBe numberOfPages
        }
      }
    }
  }
}
