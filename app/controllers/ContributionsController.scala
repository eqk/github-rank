package controllers

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import models.Contributor
import play.api.libs.json.Json
import play.api.mvc._
import service.ContributorsService

@Singleton
class ContributionsController @Inject()(
  val controllerComponents: ControllerComponents,
  contributorsService: ContributorsService[Future]
)(implicit ec: ExecutionContext) extends BaseController {

  def contributorsRank(orgName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    for {
      repos <- contributorsService.getRepositories(orgName)
      contributors <- Future.traverse(repos)(contributorsService.getContributors(orgName, _))
    } yield {
      val mergedAndSorted = contributors
        .flatten
        .groupMapReduce(_.login)(_.contributions)(_ + _)
        .map((Contributor.apply _).tupled)
        .toList
        .sortBy(-_.contributions)

      Ok(Json.toJson(mergedAndSorted))
    }
  }
}
