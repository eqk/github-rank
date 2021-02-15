package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.EitherT
import cats.instances.future._
import errors.{AppError, NotFoundError, RateLimitExceeded}
import models.{Contributor, Repository}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import services.ContributorsService

class ContributionsControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  def errorService(error: AppError): ContributorsService = {
    val service = mock[ContributorsService]
    when(service.getContributors(any(), any())).thenReturn(EitherT.leftT(error))
    when(service.getRepositories(any())).thenReturn(EitherT.leftT(error))
    service
  }

  "ContributionsController GET" should {
    val uri = "/org/tests/contributors"

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
      val repos = List(Repository("A"), Repository("B"), Repository("C"))
      val service = mock[ContributorsService]
      when(service.getRepositories(any())).thenReturn(EitherT.rightT(repos))
      when(service.getContributors(any(), ArgumentMatchers.eq(Repository("A")))).thenReturn(EitherT.rightT(List(
        Contributor("c1", 1),
        Contributor("c2", 2),
        Contributor("c3", 3)
      )))
      when(service.getContributors(any(), ArgumentMatchers.eq(Repository("B")))).thenReturn(EitherT.rightT(List(
        Contributor("c1", 3),
        Contributor("c2", 4),
        Contributor("c3", 2)
      )))
      when(service.getContributors(any(), ArgumentMatchers.eq(Repository("C")))).thenReturn(EitherT.rightT(List(
        Contributor("c1", 2),
        Contributor("c2", 2),
        Contributor("c3", 4)
      )))

      val expected = Json.toJson(List(Contributor("c3", 9), Contributor("c2", 8), Contributor("c1", 6))).toString()

      val controller = new ContributionsController(stubControllerComponents(), service)
      val actual = controller.contributorsRank("").apply(FakeRequest(GET, uri))

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsString(actual) mustBe expected
    }
  }
}
