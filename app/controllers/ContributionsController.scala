package controllers

import javax.inject._

import scala.concurrent.ExecutionContext

import cats.implicits._
import errors.{NotFoundError, RateLimitExceeded}
import models.Contributor
import play.api.libs.json.Json
import play.api.mvc._
import services.ContributorsService

@Singleton
class ContributionsController @Inject()(
  val controllerComponents: ControllerComponents,
  contributorsService: ContributorsService
)(implicit ec: ExecutionContext) extends BaseController {

  def contributorsRank(orgName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    (for {
      repos <- contributorsService.getRepositories(orgName)
      contributors <- repos.map(contributorsService.getContributors(orgName, _)).sequence
    } yield {
      val mergedAndSorted = contributors
        .flatten
        .groupMapReduce(_.login)(_.contributions)(_ + _)
        .map((Contributor.apply _).tupled)
        .toList
        .sortBy(-_.contributions)

      Ok(Json.toJson(mergedAndSorted))
    }).valueOr {
      case NotFoundError => NotFound
      case RateLimitExceeded => Forbidden
    }
  }
}
