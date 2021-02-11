package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.data.EitherT
import cats.instances.future._
import errors.{AppError, NotFoundError, RateLimitExceeded}
import models.{Contributor, Repository}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._
import play.api.libs.json._
import service.ContributorsService

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class ContributionsControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {


  def errorService(error: AppError): ContributorsService = new ContributorsService {
    override def getRepositories(orgName: String): EitherT[Future, AppError, List[Repository]] =
      EitherT.leftT(error)

    override def getContributors(orgName: String, repository: Repository): EitherT[Future, AppError, List[Contributor]] =
      EitherT.leftT(error)
  }

  val uri = "/org/tests/contributors"

  "ContributionsController GET" should {

    "return not found with according service error" in {
      val controller = new ContributionsController(stubControllerComponents(), errorService(NotFoundError))
      val ranks = controller.contributorsRank("").apply(FakeRequest(GET, uri))

      status(ranks) mustBe NOT_FOUND
    }

    "return forbidden with request limit exceeded" in {
      val controller = new ContributionsController(stubControllerComponents(), errorService(RateLimitExceeded))
      val ranks = controller.contributorsRank("").apply(FakeRequest(GET, uri))

      status(ranks) mustBe FORBIDDEN
    }

    "return contributors ranks" in {
      val contributorsService = new ContributorsService {
        override def getRepositories(orgName: String): EitherT[Future, AppError, List[Repository]] =
          EitherT.rightT(List(
            Repository("1"),
            Repository("2"),
            Repository("3")
          ))

        override def getContributors(orgName: String, repository: Repository): EitherT[Future, AppError, List[Contributor]] =
          EitherT.rightT(List(
            Contributor("1", 1),
            Contributor("2", 2),
            Contributor("3", 3)
          ))
      }
      val controller = new ContributionsController(stubControllerComponents(), contributorsService)
      val ranks = controller.contributorsRank("").apply(FakeRequest(GET, uri))

      status(ranks) mustBe OK
      contentType(ranks) mustBe Some("application/json")
      contentAsString(ranks) mustBe """[{"name":"3","contributions":9},{"name":"2","contributions":6},{"name":"1","contributions":3}]"""
    }
  }
}
