package service

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import cats.data.EitherT
import cats.implicits._
import errors.{AppError, NotFoundError, RateLimitExceeded}
import models.{Contributor, Repository}
import play.api.libs.json.Reads
import play.api.libs.ws._

@Singleton
class GithubContributorsService @Inject()(
  ws: WSClient
)(implicit ec: ExecutionContext) extends ContributorsService
{
  type FErrorOr[A] = EitherT[Future, AppError, A]
  val accessToken: Option[String] = sys.env.get("GH_TOKEN")

  private def lastPageNumber(header: String): Option[Int] = {
    val pattern = """.*page=([0-9]+)>; rel="last".*""".r
    header match {
      case pattern(page) => page.toIntOption
      case _ => None
    }
  }

  def request[A](request: WSRequest)(f: WSResponse => A): FErrorOr[A] = {
    EitherT(request.execute().map { response =>
      response.status match {
        case 404 => Left(NotFoundError)
        case 403 => Left(RateLimitExceeded)
        case 200 => Right(f(response))
      }
    })
  }

  def crawlPages[A: Reads](url: String): FErrorOr[List[A]] = {

    def requestWithPage(url: String, page: Int = 1): WSRequest = {
      val req = ws.url(url).withQueryStringParameters(
        "per_page" -> "100",
        "page" -> page.toString
      )
      accessToken.fold(req)(req.withAuth("", _, WSAuthScheme.BASIC))
    }

    def getFirstWithCount: FErrorOr[(Int, List[A])] = request(requestWithPage(url)) { resp =>
      val pagesCount = resp.header("link").flatMap(lastPageNumber).getOrElse(1)
      (pagesCount, resp.json.as[List[A]])
    }

    def getPage(page: Int): FErrorOr[List[A]] = request(requestWithPage(url, page))(_.json.as[List[A]])

    for {
      firstWithPage <- getFirstWithCount
      (pageNumber, first) = firstWithPage
      rest <- (2 to pageNumber).toList.map(page => getPage(page)).sequence
    } yield first ++ rest.flatten
  }

  def getRepositories(orgName: String): FErrorOr[List[Repository]] = {
    crawlPages[Repository](s"https://api.github.com/orgs/$orgName/repos")
  }

  def getContributors(orgName: String, repository: Repository): FErrorOr[List[Contributor]] = {
    crawlPages[Contributor](s"https://api.github.com/repos/$orgName/${repository.name}/contributors")
  }
}
