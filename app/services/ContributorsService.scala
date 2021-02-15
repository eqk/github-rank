package services

import scala.concurrent.Future

import cats.data.EitherT
import com.google.inject.ImplementedBy
import errors.AppError
import models.{Contributor, Repository}

@ImplementedBy(classOf[GithubContributorsService])
trait ContributorsService {
  def getRepositories(orgName: String): EitherT[Future, AppError, List[Repository]]
  def getContributors(orgName: String, repository: Repository): EitherT[Future, AppError, List[Contributor]]
}
